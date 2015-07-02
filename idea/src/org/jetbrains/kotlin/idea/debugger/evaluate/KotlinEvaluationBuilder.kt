/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger.evaluate

import com.intellij.debugger.DebuggerBundle
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.SuspendContext
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.expression.*
import com.intellij.diagnostic.LogMessageEx
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.ExceptionUtil
import com.sun.jdi.InvocationException
import com.sun.jdi.ObjectReference
import com.sun.jdi.VMDisconnectedException
import com.sun.jdi.VirtualMachine
import com.sun.jdi.request.EventRequest
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.Progress
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.idea.caches.resolve.JavaResolveExtension
import org.jetbrains.kotlin.idea.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinEvaluateExpressionCache.CompiledDataDescriptor
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinEvaluateExpressionCache.ParametersDescriptor
import org.jetbrains.kotlin.idea.debugger.evaluate.compilingEvaluator.loadClasses
import org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine.ExtractionResult
import org.jetbrains.kotlin.idea.util.DebuggerUtils
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.attachment.attachmentByPsiFile
import org.jetbrains.kotlin.idea.util.attachment.mergeAttachments
import org.jetbrains.kotlin.load.java.structure.impl.JavaClassImpl
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.codeFragmentUtil.debugTypeInfo
import org.jetbrains.kotlin.psi.codeFragmentUtil.suppressDiagnosticsInDebugMode
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.types.Flexibility
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.ASM5
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*

private val RECEIVER_NAME = "\$receiver"
private val THIS_NAME = "this"

object KotlinEvaluationBuilder: EvaluatorBuilder {
    override fun build(codeFragment: PsiElement, position: SourcePosition?): ExpressionEvaluator {
        if (codeFragment !is JetCodeFragment || position == null) {
            return EvaluatorBuilderImpl.getInstance()!!.build(codeFragment, position)
        }

        val file = position.getFile()
        if (file !is JetFile) {
            throw EvaluateExceptionUtil.createEvaluateException("Couldn't evaluate kotlin expression in non-kotlin context")
        }

        if (position.getLine() < 0) {
            throw EvaluateExceptionUtil.createEvaluateException("Couldn't evaluate kotlin expression at $position")
        }

        if (codeFragment.getContext() !is JetElement) {
            val attachments = arrayOf(attachmentByPsiFile(position.getFile()),
                                      attachmentByPsiFile(codeFragment),
                                      Attachment("breakpoint.info", "line: ${position.getLine()}"))

            logger.error("Trying to evaluate ${codeFragment.javaClass} with context ${codeFragment.getContext()?.javaClass}", mergeAttachments(*attachments))
            throw EvaluateExceptionUtil.createEvaluateException("Couldn't evaluate kotlin expression in this context")
        }

        return ExpressionEvaluatorImpl(KotlinEvaluator(codeFragment, position))
    }
}

val logger = Logger.getInstance(javaClass<KotlinEvaluator>())

class KotlinEvaluator(val codeFragment: JetCodeFragment,
                      val sourcePosition: SourcePosition
) : Evaluator {
    override fun evaluate(context: EvaluationContextImpl): Any? {
        if (codeFragment.getText().isEmpty()) {
            return context.getDebugProcess().getVirtualMachineProxy().mirrorOf()
        }

        var isCompiledDataFromCache = true
        try {
            val compiledData = KotlinEvaluateExpressionCache.getOrCreateCompiledData(codeFragment, sourcePosition, context) {
                fragment, position ->
                isCompiledDataFromCache = false
                extractAndCompile(fragment, position, context)
            }
            val result = runEval4j(context, compiledData)

            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            // If bytecode was taken from cache and exception was thrown - recompile bytecode and run eval4j again
            if (isCompiledDataFromCache && result is ExceptionThrown && result.kind == ExceptionThrown.ExceptionKind.BROKEN_CODE) {
                return runEval4j(context, extractAndCompile(codeFragment, sourcePosition, context)).toJdiValue(virtualMachine)
            }

            return result.toJdiValue(virtualMachine)
        }
        catch(e: EvaluateException) {
            throw e
        }
        catch(e: ProcessCanceledException) {
            exception(e)
        }
        catch(e: VMDisconnectedException) {
            exception(DebuggerBundle.message("error.vm.disconnected"))
        }
        catch (e: Exception) {
            val attachments = arrayOf(attachmentByPsiFile(sourcePosition.getFile()),
                                      attachmentByPsiFile(codeFragment),
                                      Attachment("breakpoint.info", "line: ${sourcePosition.getLine()}"))
            logger.error(LogMessageEx.createEvent(
                                "Couldn't evaluate expression",
                                ExceptionUtil.getThrowableText(e),
                                mergeAttachments(*attachments)))

            val cause = if (e.getMessage() != null) ": ${e.getMessage()}" else ""
            exception("An exception occurs during Evaluate Expression Action $cause")
        }
    }

    override fun getModifier(): Modifier? {
        return null
    }

    companion object {
        private fun extractAndCompile(codeFragment: JetCodeFragment, sourcePosition: SourcePosition, context: EvaluationContextImpl): CompiledDataDescriptor {
            codeFragment.checkForErrors(false)

            val extractionResult = getFunctionForExtractedFragment(codeFragment, sourcePosition.getFile(), sourcePosition.getLine())
            if (extractionResult == null) {
                throw IllegalStateException("Code fragment cannot be extracted to function")
            }
            val parametersDescriptor = extractionResult.getParametersForDebugger(codeFragment)
            val extractedFunction = extractionResult.declaration as JetNamedFunction

            val classFileFactory = createClassFileFactory(codeFragment, extractedFunction, context, parametersDescriptor)

            val outputFiles = classFileFactory.asList()
                                    .filter { it.relativePath != "$packageInternalName.class" }
                                    .sortBy { it.relativePath.length() }

            val funName = extractedFunction.getName()
            if (funName == null) {
                throw IllegalStateException("Extracted function should have a name: ${extractedFunction.getText()}")
            }

            val additionalFiles = if (outputFiles.size() < 2) emptyList()
                                  else outputFiles.subList(1, outputFiles.size()).map { getClassName(it.relativePath) to it.asByteArray() }

            return CompiledDataDescriptor(
                    outputFiles.first().asByteArray(),
                    additionalFiles,
                    sourcePosition,
                    funName,
                    parametersDescriptor)
        }

        private fun getClassName(fileName: String): String {
            return fileName.substringBeforeLast(".class").replace("/", ".")
        }

        private fun runEval4j(context: EvaluationContextImpl, compiledData: CompiledDataDescriptor): InterpreterResult {
            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            if (compiledData.additionalClasses.isNotEmpty()) {
                loadClasses(context, compiledData.additionalClasses)
            }

            var resultValue: InterpreterResult? = null
            ClassReader(compiledData.bytecodes).accept(object : ClassVisitor(ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    if (name == compiledData.funName) {
                        val argumentTypes = Type.getArgumentTypes(desc)
                        val args = context.getArgumentsForEval4j(compiledData.parameters, argumentTypes)

                        return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                            override fun visitEnd() {
                                val allRequests = virtualMachine.eventRequestManager().breakpointRequests() +
                                                  virtualMachine.eventRequestManager().classPrepareRequests()
                                allRequests.forEach { it.disable() }

                                val eval = JDIEval(virtualMachine,
                                                   context.getClassLoader(),
                                                   context.getSuspendContext().getThread()?.getThreadReference()!!,
                                                   context.getSuspendContext().getInvokePolicy())

                                resultValue = interpreterLoop(
                                        this,
                                        makeInitialFrame(this, args.zip(argumentTypes).map { boxOrUnboxArgumentIfNeeded(eval, it.first, it.second) }),
                                        eval
                                )

                                allRequests.forEach { it.enable() }
                            }
                        }
                    }

                    return super.visitMethod(access, name, desc, signature, exceptions)
                }
            }, 0)

            return resultValue ?: throw IllegalStateException("resultValue is null: cannot find method ${compiledData.funName}")
        }

        private fun boxOrUnboxArgumentIfNeeded(eval: JDIEval, argumentValue: Value, parameterType: Type): Value {
            val argumentType = argumentValue.asmType

            if (AsmUtil.isPrimitive(parameterType) && !AsmUtil.isPrimitive(argumentType)) {
                try {
                    val unboxedType = AsmUtil.unboxType(argumentType)
                    if (parameterType == unboxedType) {
                        return eval.unboxType(argumentValue, parameterType)
                    }
                }
                catch(ignored: UnsupportedOperationException) {
                }
            }

            if (!AsmUtil.isPrimitive(parameterType) && AsmUtil.isPrimitive(argumentType)) {
                if (parameterType == AsmUtil.boxType(argumentType)) {
                    return eval.boxType(argumentValue)
                }
            }

            return argumentValue
        }

        private fun InterpreterResult.toJdiValue(vm: VirtualMachine): com.sun.jdi.Value? {
            val jdiValue = when (this) {
                is ValueReturned -> result
                is ExceptionThrown -> {
                    if (this.kind == ExceptionThrown.ExceptionKind.FROM_EVALUATED_CODE) {
                        exception(InvocationException(this.exception.value as ObjectReference))
                    }
                    else {
                        exception(exception.toString())
                    }
                }
                is AbnormalTermination -> exception(message)
                else -> throw IllegalStateException("Unknown result value produced by eval4j")
            }
            return jdiValue.asJdiValue(vm, jdiValue.asmType)
        }

        private fun ExtractionResult.getParametersForDebugger(fragment: JetCodeFragment): ParametersDescriptor {
            return runReadAction {
                val valuesForLabels = HashMap<String, Value>()

                val contextElementFile = fragment.getContext()?.getContainingFile()
                if (contextElementFile is JetCodeFragment) {
                    contextElementFile.accept(object: JetTreeVisitorVoid() {
                        override fun visitProperty(property: JetProperty) {
                            val value = property.getUserData(KotlinCodeFragmentFactory.LABEL_VARIABLE_VALUE_KEY)
                            if (value != null) {
                                valuesForLabels.put(property.getName()!!, value.asValue())
                            }
                        }
                    })
                }

                val parameters = ParametersDescriptor()
                val receiver = config.descriptor.receiverParameter
                if (receiver != null) {
                    parameters.add(THIS_NAME, receiver.getParameterType(true))
                }

                for (param in config.descriptor.parameters) {
                    val paramName = when {
                        param.argumentText.contains("@") -> param.argumentText.substringBefore("@")
                        param.argumentText.startsWith("::") -> param.argumentText.substring(2)
                        else -> param.argumentText
                    }
                    parameters.add(paramName, param.getParameterType(true), valuesForLabels[paramName])
                }
                parameters
            }
        }

        private fun EvaluationContextImpl.getArgumentsForEval4j(parameters: ParametersDescriptor, parameterTypes: Array<Type>): List<Value> {
            val frameVisitor = FrameVisitor(this)
            return parameters.zip(parameterTypes).map {
                if (it.first.value != null) {
                    it.first.value!!
                }
                else {
                    frameVisitor.findValue(it.first.callText, it.second, checkType = false, failIfNotFound = true)!!
                }
            }
        }

        private fun createClassFileFactory(
                codeFragment: JetCodeFragment,
                extractedFunction: JetNamedFunction,
                context: EvaluationContextImpl,
                parameters: ParametersDescriptor
        ): ClassFileFactory {
            return runReadAction {
                val jetFile = createFileForDebugger(codeFragment, extractedFunction)

                val (bindingContext, moduleDescriptor, files) = jetFile.checkForErrors(true)

                val generateClassFilter = object : GenerationState.GenerateClassFilter {
                    override fun shouldGeneratePackagePart(file: JetFile) = file == jetFile
                    override fun shouldAnnotateClass(classOrObject: JetClassOrObject) = true
                    override fun shouldGenerateClass(classOrObject: JetClassOrObject) = classOrObject.getContainingJetFile() == jetFile
                    override fun shouldGenerateScript(script: JetScript) = false
                }

                val state = GenerationState(
                        jetFile.getProject(),
                        ClassBuilderFactories.BINARIES,
                        Progress.DEAF,
                        moduleDescriptor,
                        bindingContext,
                        files,
                        true, true,
                        generateClassFilter,
                        false, false,
                        null, null,
                        DiagnosticSink.DO_NOTHING,
                        null)

                val frameVisitor = FrameVisitor(context)

                extractedFunction.getReceiverTypeReference()?.let {
                    state.getBindingTrace().recordAnonymousType(it, THIS_NAME, frameVisitor)
                }

                val valueParameters = extractedFunction.getValueParameters()
                var paramIndex = 0
                for (param in parameters) {

                    if (param.callText.contains(THIS_NAME)) continue

                    val valueParameter = valueParameters[paramIndex++]

                    val paramRef = valueParameter.getTypeReference()
                    if (paramRef == null) {
                        logger.error("Each parameter for extracted function should have a type reference",
                                     Attachment("codeFragment.txt", codeFragment.getText()),
                                     Attachment("extractedFunction.txt", extractedFunction.getText()))

                        exception("An exception occurs during Evaluate Expression Action")
                    }

                    state.getBindingTrace().recordAnonymousType(paramRef, param.callText, frameVisitor)
                }

                KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)

                state.getFactory()
            }
        }

        private fun BindingTrace.recordAnonymousType(typeReference: JetTypeReference, localVariableName: String, visitor: FrameVisitor) {
            val paramAnonymousType = typeReference.debugTypeInfo
            if (paramAnonymousType != null) {
                val declarationDescriptor = paramAnonymousType.getConstructor().getDeclarationDescriptor()
                if (declarationDescriptor is ClassDescriptor) {
                    val localVariable = visitor.findValue(localVariableName, asmType = null, checkType = false, failIfNotFound = false)
                    if (localVariable == null) {
                        exception("Couldn't find local variable this in current frame to get classType for anonymous type ${paramAnonymousType}}")
                    }
                    record(CodegenBinding.ASM_TYPE, declarationDescriptor, localVariable.asmType)
                }
            }
        }

        private fun exception(msg: String) = throw EvaluateExceptionUtil.createEvaluateException(msg)

        private fun exception(e: Throwable): Nothing {
            val message = e.getMessage()
            if (message != null) {
                throw EvaluateExceptionUtil.createEvaluateException(message, e)
            }
            throw EvaluateExceptionUtil.createEvaluateException(e)
        }

        private fun JetFile.checkForErrors(analyzeInlineFunctions: Boolean): ExtendedAnalysisResult {
            return runReadAction {
                try {
                    AnalyzingUtils.checkForSyntacticErrors(this)
                }
                catch (e: IllegalArgumentException) {
                    throw EvaluateExceptionUtil.createEvaluateException(e.getMessage())
                }

                val resolutionFacade = KotlinCacheService.getInstance(getProject()).getResolutionFacade(listOf(this, createFlexibleTypesFile()))
                val analysisResult = resolutionFacade.analyzeFullyAndGetResult(Collections.singletonList(this))
                if (analysisResult.isError()) {
                    throw EvaluateExceptionUtil.createEvaluateException(analysisResult.error)
                }

                val bindingContext = analysisResult.bindingContext
                bindingContext.getDiagnostics().firstOrNull { it.getSeverity() == Severity.ERROR }?.let {
                    throw EvaluateExceptionUtil.createEvaluateException(DefaultErrorMessages.render(it))
                }

                if (analyzeInlineFunctions) {
                    val (newBindingContext, files) = DebuggerUtils.analyzeInlinedFunctions(resolutionFacade, bindingContext, this, false)
                    ExtendedAnalysisResult(newBindingContext, analysisResult.moduleDescriptor, files)
                }
                else {
                    ExtendedAnalysisResult(bindingContext, analysisResult.moduleDescriptor, Collections.singletonList(this))
                }
            }
        }

        private data class ExtendedAnalysisResult(val bindingContext: BindingContext, val moduleDescriptor: ModuleDescriptor, val files: List<JetFile>)
    }
}

private val template = """
package packageForDebugger

!IMPORT_LIST!

!FUNCTION!
"""

private val packageInternalName = PackageClassUtils.getPackageClassInternalName(FqName("packageForDebugger"))

private fun createFileForDebugger(codeFragment: JetCodeFragment,
                                  extractedFunction: JetNamedFunction
): JetFile {
    var fileText = template.replace("!IMPORT_LIST!",
                                    codeFragment.importsToString()
                                            .split(JetCodeFragment.IMPORT_SEPARATOR)
                                            .joinToString("\n"))

    val extractedFunctionText = extractedFunction.getText()
    assert(extractedFunctionText != null, "Text of extracted function shouldn't be null")
    fileText = fileText.replace("!FUNCTION!", extractedFunction.getText()!!)

    val jetFile = codeFragment.createJetFile("debugFile.kt", fileText)
    jetFile.suppressDiagnosticsInDebugMode = true

    val list = jetFile.getDeclarations()
    val function = list.get(0) as JetNamedFunction

    function.getReceiverTypeReference()?.debugTypeInfo = extractedFunction.getReceiverTypeReference()?.debugTypeInfo

    for ((newParam, oldParam) in function.getValueParameters().zip(extractedFunction.getValueParameters())) {
        newParam.getTypeReference()?.debugTypeInfo = oldParam.getTypeReference()?.debugTypeInfo
    }

    function.getTypeReference()?.debugTypeInfo = extractedFunction.getTypeReference()?.debugTypeInfo

    return jetFile
}

private fun PsiElement.createFlexibleTypesFile(): JetFile {
    return createJetFile(
            "FLEXIBLE_TYPES.kt",
            """
                package ${Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getPackageFqName()}
                public class ${Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getRelativeClassName()}<L, U>
            """
    )
}

private fun PsiElement.createJetFile(fileName: String, fileText: String): JetFile {
    // Not using JetPsiFactory because we need a virtual file attached to the JetFile
    val virtualFile = LightVirtualFile(fileName, JetLanguage.INSTANCE, fileText)
    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
    val jetFile = (PsiFileFactory.getInstance(getProject()) as PsiFileFactoryImpl)
            .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false) as JetFile
    jetFile.analysisContext = this
    return jetFile
}

private fun SuspendContext.getInvokePolicy(): Int {
    return if (getSuspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD) ObjectReference.INVOKE_SINGLE_THREADED else 0
}

fun Type.getClassDescriptor(project: Project): ClassDescriptor? {
    if (AsmUtil.isPrimitive(this)) return null

    val jvmName = JvmClassName.byInternalName(getInternalName()).getFqNameForClassNameWithoutDollars()

    val platformClasses = JavaToKotlinClassMap.INSTANCE.mapPlatformClass(jvmName)
    if (platformClasses.isNotEmpty()) return platformClasses.first()

    return runReadAction {
        val classes = JavaPsiFacade.getInstance(project).findClasses(jvmName.asString(), GlobalSearchScope.allScope(project))
        if (classes.isEmpty()) null
        else {
            val clazz = classes.first()
            JavaResolveExtension.getResolver(project, clazz).resolveClass(JavaClassImpl(clazz))
        }
    }
}