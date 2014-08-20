/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.debugger.evaluate

import org.jetbrains.jet.plugin.debugger.evaluate.*
import com.intellij.psi.PsiElement
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.evaluation.*
import com.intellij.debugger.engine.evaluation.expression.*
import org.jetbrains.jet.lang.resolve.AnalyzingUtils
import org.jetbrains.jet.codegen.state.GenerationState
import org.jetbrains.jet.codegen.ClassBuilderFactories
import org.jetbrains.jet.codegen.KotlinCodegenFacade
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.jet.plugin.JetLanguage
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.PsiFileFactory
import com.intellij.openapi.vfs.CharsetToolkit
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.Opcodes.ASM5
import org.jetbrains.org.objectweb.asm.*
import com.intellij.openapi.util.Computable
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.jet.codegen.ClassFileFactory
import org.jetbrains.jet.OutputFileCollection
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import org.jetbrains.jet.lang.psi.JetCodeFragment
import org.jetbrains.jet.lang.psi.codeFragmentUtil.skipVisibilityCheck
import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.jet.codegen.CompilationErrorHandler
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages
import com.sun.jdi.request.EventRequest
import com.sun.jdi.ObjectReference
import com.intellij.debugger.engine.SuspendContext
import org.jetbrains.jet.plugin.debugger.evaluate.KotlinEvaluateExpressionCache.*
import org.jetbrains.jet.lang.resolve.BindingContext
import com.sun.jdi.VirtualMachine
import org.jetbrains.jet.codegen.AsmUtil
import com.sun.jdi.InvalidStackFrameException
import org.jetbrains.jet.plugin.refactoring.runReadAction

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

        val packageName = file.getPackageDirective()?.getFqName()?.asString()
        if (packageName != null && packageName.isNotEmpty()) {
            codeFragment.addImportsFromString("import $packageName.*")
        }
        return ExpressionEvaluatorImpl(KotlinEvaluator(codeFragment as JetCodeFragment, position))
    }
}

val logger = Logger.getInstance(javaClass<KotlinEvaluator>())

class KotlinEvaluator(val codeFragment: JetCodeFragment,
                      val sourcePosition: SourcePosition
) : Evaluator {
    override fun evaluate(context: EvaluationContextImpl): Any? {
        var isCompiledDataFromCache = true
        try {
            val compiledData = KotlinEvaluateExpressionCache.getOrCreateCompiledData(codeFragment, sourcePosition, context) {
                fragment, position ->
                isCompiledDataFromCache = false
                extractAndCompile(fragment, position)
            }
            val result = runEval4j(context, compiledData)

            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            // If bytecode was taken from cache and exception was thrown - recompile bytecode and run eval4j again
            if (isCompiledDataFromCache && result is ExceptionThrown && result.kind == ExceptionThrown.ExceptionKind.BROKEN_CODE) {
                return runEval4j(context, extractAndCompile(codeFragment, sourcePosition)).toJdiValue(virtualMachine)
            }

            return result.toJdiValue(virtualMachine)
        }
        catch(e: EvaluateException) {
            throw e
        }
        catch (e: Exception) {
            logger.error("Couldn't evaluate expression:\nfileText = ${sourcePosition.getFile().getText()}\nline = ${sourcePosition.getLine()}\ncodeFragment = ${codeFragment.getText()}", e)
            val cause = if (e.getMessage() != null) ": ${e.getMessage()}" else ""
            exception("An exception occurs during Evaluate Expression Action $cause")
        }
    }

    override fun getModifier(): Modifier? {
        return null
    }

    class object {
        private fun extractAndCompile(codeFragment: JetCodeFragment, sourcePosition: SourcePosition): CompiledDataDescriptor {
            val extractedFunction = getFunctionForExtractedFragment(codeFragment, sourcePosition.getFile(), sourcePosition.getLine())
            if (extractedFunction == null) {
                throw IllegalStateException("Code fragment cannot be extracted to function: ${sourcePosition.getFile().getText()}:${sourcePosition.getLine()},\ncodeFragment = ${codeFragment.getText()}")
            }

            val classFileFactory = createClassFileFactory(codeFragment, extractedFunction)

            // KT-4509
            val outputFiles = (classFileFactory : OutputFileCollection).asList().filter { it.relativePath != "$packageInternalName.class" }
            if (outputFiles.size() != 1) exception("Expression compiles to more than one class file. Note that lambdas, classes and objects are unsupported yet. List of files: ${outputFiles.makeString(",")}")

            val funName = extractedFunction.getName()
            if (funName == null) {
                throw IllegalStateException("Extracted function should have a name: ${extractedFunction.getText()}")
            }
            return CompiledDataDescriptor(outputFiles.first().asByteArray(), sourcePosition, funName, extractedFunction.getParametersForDebugger())
        }

        private fun runEval4j(context: EvaluationContextImpl, compiledData: CompiledDataDescriptor): InterpreterResult {
            val virtualMachine = context.getDebugProcess().getVirtualMachineProxy().getVirtualMachine()

            var resultValue: InterpreterResult? = null
            ClassReader(compiledData.bytecodes).accept(object : ClassVisitor(ASM5) {
                override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
                    if (name == compiledData.funName) {
                        val args = context.getArgumentsForEval4j(compiledData.parameters.getParameterNames(), Type.getArgumentTypes(desc))
                        return object : MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                            override fun visitEnd() {
                                resultValue = interpreterLoop(
                                        this,
                                        makeInitialFrame(this, args),
                                        JDIEval(virtualMachine,
                                                context.getClassLoader()!!,
                                                context.getSuspendContext().getThread()?.getThreadReference()!!,
                                                context.getSuspendContext().getInvokePolicy())
                                )
                            }
                        }
                    }

                    return super.visitMethod(access, name, desc, signature, exceptions)
                }
            }, 0)

            return resultValue ?: throw IllegalStateException("resultValue is null: cannot find method ${compiledData.funName}")
        }

        private fun InterpreterResult.toJdiValue(vm: VirtualMachine): com.sun.jdi.Value? {
            val jdiValue = when (this) {
                is ValueReturned -> result
                is ExceptionThrown -> exception(exception.toString())
                is AbnormalTermination -> exception(message)
                else -> throw IllegalStateException("Unknown result value produced by eval4j")
            }
            return jdiValue.asJdiValue(vm, jdiValue.asmType)
        }

        private fun JetNamedFunction.getParametersForDebugger(): ParametersDescriptor {
            return runReadAction {
                val parameters = ParametersDescriptor()
                val bindingContext = getAnalysisResults().getBindingContext()
                val descriptor = bindingContext[BindingContext.FUNCTION, this]
                if (descriptor != null) {
                    val receiver = descriptor.getReceiverParameter()
                    if (receiver != null) {
                        parameters.add(THIS_NAME, receiver.getType())
                    }

                    descriptor.getValueParameters().forEach {
                        param ->
                        parameters.add(param.getName().asString(), param.getType())
                    }
                }
                parameters
            }!!
        }

        private fun EvaluationContextImpl.getArgumentsForEval4j(parameterNames: List<String>, parameterTypes: Array<Type>): List<Value> {
            return parameterNames.zip(parameterTypes).map { this.findLocalVariable(it.first, it.second, checkType = false, failIfNotFound = true)!! }
        }

        private fun createClassFileFactory(codeFragment: JetCodeFragment, extractedFunction: JetNamedFunction): ClassFileFactory {
            return runReadAction {
                val file = createFileForDebugger(codeFragment, extractedFunction)

                checkForSyntacticErrors(file)

                val analyzeExhaust = file.getAnalysisResults()
                if (analyzeExhaust.isError()) {
                    exception(analyzeExhaust.getError())
                }

                val bindingContext = analyzeExhaust.getBindingContext()
                bindingContext.getDiagnostics().forEach {
                    diagnostic ->
                    if (diagnostic.getSeverity() == Severity.ERROR) {
                        exception(DefaultErrorMessages.RENDERER.render(diagnostic))
                    }
                }

                val state = GenerationState(
                        file.getProject(),
                        ClassBuilderFactories.BINARIES,
                        analyzeExhaust.getModuleDescriptor(),
                        bindingContext,
                        listOf(file)
                )

                KotlinCodegenFacade.compileCorrectFiles(state, CompilationErrorHandler.THROW_EXCEPTION)

                state.getFactory()
            }!!
        }

        private fun exception(msg: String) = throw EvaluateExceptionUtil.createEvaluateException(msg)

        private fun exception(e: Throwable) {
            val message = e.getMessage()
            if (message != null) {
                exception(message)
            }
            throw EvaluateExceptionUtil.createEvaluateException(e)
        }
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
                                            .makeString("\n"))

    val extractedFunctionText = extractedFunction.getText()
    assert(extractedFunctionText != null, "Text of extracted function shouldn't be null")
    fileText = fileText.replace("!FUNCTION!", extractedFunction.getText()!!)

    val virtualFile = LightVirtualFile("debugFile.kt", JetLanguage.INSTANCE, fileText)
    virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET)
    val jetFile = (PsiFileFactory.getInstance(codeFragment.getProject()) as PsiFileFactoryImpl)
            .trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false) as JetFile
    jetFile.skipVisibilityCheck = true
    return jetFile
}

fun checkForSyntacticErrors(file: JetFile) {
    try {
        AnalyzingUtils.checkForSyntacticErrors(file)
    }
    catch (e: IllegalArgumentException) {
        throw EvaluateExceptionUtil.createEvaluateException(e.getMessage())
    }
}

private fun SuspendContext.getInvokePolicy(): Int {
    return if (getSuspendPolicy() == EventRequest.SUSPEND_EVENT_THREAD) ObjectReference.INVOKE_SINGLE_THREADED else 0
}

fun EvaluationContextImpl.findLocalVariable(name: String, asmType: Type?, checkType: Boolean, failIfNotFound: Boolean): Value? {
    val frame = getFrameProxy()?.getStackFrame()
    if (frame == null) return null
    try {
        when (name) {
            THIS_NAME -> {
                val thisObject = frame.thisObject()
                if (thisObject != null) {
                    val eval4jValue = thisObject.asValue()
                    if (isValueOfCorrectType(eval4jValue, asmType, true)) return eval4jValue
                }

                val receiver = findLocalVariable(RECEIVER_NAME, asmType, checkType = true, failIfNotFound = false)
                if (receiver != null) return receiver

                val this0 = findLocalVariable(AsmUtil.CAPTURED_THIS_FIELD, asmType, checkType = true, failIfNotFound = false)
                if (this0 != null) return this0
            }
            else -> {
                val localVariable = frame.visibleVariableByName(name)
                if (localVariable != null) {
                    val eval4jValue = frame.getValue(localVariable).asValue()
                    if (isValueOfCorrectType(eval4jValue, asmType, checkType)) return eval4jValue
                }

                val eval4j = JDIEval(frame.virtualMachine()!!,
                                     getClassLoader()!!,
                                     getSuspendContext().getThread()?.getThreadReference()!!,
                                     getSuspendContext().getInvokePolicy())

                fun JDIEval.getField(owner: Value, name: String, asmType: Type?): Value? {
                    val fieldDescription = FieldDescription(owner.asmType.getInternalName(), name, asmType?.getDescriptor() ?: "", isStatic = false)
                    try {
                        val fieldValue = getField(owner, fieldDescription)
                        if (isValueOfCorrectType(fieldValue, asmType, checkType)) return fieldValue
                        return null
                    }
                    catch (e: Exception) {
                        return null
                    }
                }

                fun findCapturedVal(name: String): Value? {
                    var result: Value? = null
                    var thisObj: Value? = frame.thisObject().asValue()

                    while (result == null && thisObj != null) {
                        result = eval4j.getField(thisObj!!, name, asmType)
                        if (result == null) {
                            thisObj = eval4j.getField(thisObj!!, AsmUtil.CAPTURED_THIS_FIELD, null)
                        }
                    }
                    return result
                }

                val capturedValName = getCapturedFieldName(name)
                val capturedVal = findCapturedVal(capturedValName)
                if (capturedVal != null) return capturedVal
            }
        }

        return if (!failIfNotFound)
            null
        else
            throw EvaluateExceptionUtil.createEvaluateException("Cannot find local variable: name = $name${if (checkType) ", type = " + asmType.toString() else ""}")
    }
    catch(e: InvalidStackFrameException) {
        throw EvaluateExceptionUtil.createEvaluateException("Local variable $name is unavailable in current frame")
    }
}

private fun getCapturedFieldName(name: String) = when (name) {
    RECEIVER_NAME -> AsmUtil.CAPTURED_RECEIVER_FIELD
    THIS_NAME -> AsmUtil.CAPTURED_THIS_FIELD
    AsmUtil.CAPTURED_RECEIVER_FIELD -> name
    AsmUtil.CAPTURED_THIS_FIELD -> name
    else -> "$$name"
}

private fun isValueOfCorrectType(value: Value, asmType: Type?, shouldCheckType: Boolean) = !shouldCheckType || asmType == null || value.asmType == asmType