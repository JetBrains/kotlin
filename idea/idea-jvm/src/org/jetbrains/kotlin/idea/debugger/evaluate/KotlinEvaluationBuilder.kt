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

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContext
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.expression.*
import com.intellij.diagnostic.LogMessageEx
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ExceptionUtil
import com.sun.jdi.*
import com.sun.jdi.Value
import com.sun.jdi.request.EventRequest
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.Value as Eval4JValue
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.caches.resolve.util.getJavaClassDescriptor
import org.jetbrains.kotlin.idea.debugger.DebuggerUtils
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.Companion.compileCodeFragmentCacheAware
import org.jetbrains.kotlin.idea.debugger.evaluate.compilation.*
import org.jetbrains.kotlin.idea.debugger.evaluate.compilingEvaluator.loadClassesSafely
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.EvaluatorValueConverter
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.VariableFinder
import org.jetbrains.kotlin.idea.debugger.safeLocation
import org.jetbrains.kotlin.idea.debugger.safeMethod
import org.jetbrains.kotlin.idea.runInReadActionWithWriteActionPriorityWithPCE
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.attachment.attachmentByPsiFile
import org.jetbrains.kotlin.idea.util.attachment.mergeAttachments
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import java.util.*

internal val LOG = Logger.getInstance("#org.jetbrains.kotlin.idea.debugger.evaluate.KotlinEvaluator")
internal const val GENERATED_FUNCTION_NAME = "generated_for_debugger_fun"
internal const val GENERATED_CLASS_NAME = "Generated_for_debugger_class"

object KotlinEvaluationBuilder : EvaluatorBuilder {
    override fun build(codeFragment: PsiElement, position: SourcePosition?): ExpressionEvaluator {
        if (codeFragment !is KtCodeFragment || position == null) {
            return EvaluatorBuilderImpl.getInstance()!!.build(codeFragment, position)
        }

        if (position.line < 0 && position.file !is KtFile) {
            evaluationException("Couldn't evaluate Kotlin expression at $position")
        }

        val file = position.file
        if (file is KtFile && position.line >= 0) {
            val document = PsiDocumentManager.getInstance(file.project).getDocument(file)
            if (document == null || document.lineCount < position.line) {
                evaluationException(
                    "Couldn't evaluate Kotlin expression: breakpoint is placed outside the file. " +
                            "It may happen when you've changed source file after starting a debug process."
                )
            }
        }

        if (codeFragment.context !is KtElement) {
            val attachments = arrayOf(
                attachmentByPsiFile(position.file),
                attachmentByPsiFile(codeFragment),
                Attachment("breakpoint.info", "line: ${position.line}")
            )

            LOG.error(
                "Trying to evaluate ${codeFragment::class.java} with context ${codeFragment.context?.javaClass}",
                mergeAttachments(*attachments)
            )
            evaluationException("Couldn't evaluate Kotlin expression in this context")
        }

        return ExpressionEvaluatorImpl(KotlinEvaluator(codeFragment, position))
    }
}

class KotlinEvaluator(val codeFragment: KtCodeFragment, val sourcePosition: SourcePosition) : Evaluator {
    override fun evaluate(context: EvaluationContextImpl): Any? {
        if (codeFragment.text.isEmpty()) {
            return context.debugProcess.virtualMachineProxy.mirrorOfVoid()
        }

        if (DumbService.getInstance(codeFragment.project).isDumb) {
            evaluationException("Code fragment evaluation is not available in the dumb mode")
        }

        try {
            return evaluateSafe(context)
        } catch (e: EvaluateException) {
            throw e
        } catch (e: ProcessCanceledException) {
            evaluationException(e)
        } catch (e: Eval4JInterpretingException) {
            evaluationException(e.cause)
        } catch (e: Exception) {
            val isSpecialException = isSpecialException(e)
            if (isSpecialException) {
                evaluationException(e)
            }

            val text = runReadAction { codeFragment.context?.text ?: "null" }
            val attachments = arrayOf(
                attachmentByPsiFile(sourcePosition.file),
                attachmentByPsiFile(codeFragment),
                Attachment("breakpoint.info", "line: ${runReadAction { sourcePosition.line }}"),
                Attachment("context.info", text)
            )

            LOG.error(
                @Suppress("DEPRECATION")
                LogMessageEx.createEvent(
                    "Couldn't evaluate expression",
                    ExceptionUtil.getThrowableText(e),
                    mergeAttachments(*attachments)
                )
            )

            val cause = if (e.message != null) ": ${e.message}" else ""
            evaluationException("An exception occurs during Evaluate Expression Action $cause")
        }
    }

    private fun evaluateSafe(context: EvaluationContextImpl): Any? {
        fun compilerFactory(): CompiledDataDescriptor = compileCodeFragment(context)

        val (compiledData, isCompiledDataFromCache) = compileCodeFragmentCacheAware(codeFragment, sourcePosition, ::compilerFactory)
        val classLoaderRef = loadClassesSafely(context, compiledData.classes)

        val thread = context.suspendContext.thread?.threadReference ?: error("Can not find a thread to run evaluation on")
        val invokePolicy = context.suspendContext.getInvokePolicy()
        val executionContext = ExecutionContext(context, thread, invokePolicy)

        val result = if (classLoaderRef != null) {
            evaluateWithCompilation(executionContext, compiledData, classLoaderRef)
                ?: evaluateWithEval4J(executionContext, compiledData, classLoaderRef)
        } else {
            evaluateWithEval4J(executionContext, compiledData, classLoaderRef)
        }

        // If bytecode was taken from cache and exception was thrown - recompile bytecode and run eval4j again
        if (isCompiledDataFromCache && result is ExceptionThrown && result.kind == ExceptionThrown.ExceptionKind.BROKEN_CODE) {
            val (recompiledData, _) = compileCodeFragmentCacheAware(codeFragment, sourcePosition, ::compilerFactory, force = true)
            return evaluateWithEval4J(executionContext, recompiledData, classLoaderRef).toJdiValue(executionContext)
        }

        return when (result) {
            is InterpreterResult -> result.toJdiValue(executionContext)
            else -> result
        }
    }

    private fun compileCodeFragment(evaluationContext: EvaluationContextImpl): CompiledDataDescriptor {
        val debugProcess = evaluationContext.debugProcess
        var analysisResult = checkForErrors(codeFragment, debugProcess)

        if (codeFragment.wrapToStringIfNeeded(analysisResult.bindingContext)) {
            // Repeat analysis with toString() added
            analysisResult = checkForErrors(codeFragment, debugProcess)
        }

        val (bindingContext) = runReadAction {
            DebuggerUtils.analyzeInlinedFunctions(
                KotlinCacheService.getInstance(codeFragment.project).getResolutionFacade(listOf(codeFragment)),
                codeFragment, false, analysisResult.bindingContext
            )
        }

        val moduleDescriptor = analysisResult.moduleDescriptor

        val result = CodeFragmentCompiler(evaluationContext).compile(codeFragment, bindingContext, moduleDescriptor)
        return CompiledDataDescriptor.from(result, sourcePosition)
    }

    private fun KtCodeFragment.wrapToStringIfNeeded(bindingContext: BindingContext): Boolean {
        if (this !is KtExpressionCodeFragment) {
            return false
        }

        val contentElement = runReadAction { getContentElement() }
        val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, contentElement]?.type
        if (contentElement != null && expressionType?.isInlineClassType() == true) {
            val newExpression = runReadAction {
                val expressionText = contentElement.text
                KtPsiFactory(project).createExpression("($expressionText).toString()")
            }
            runInEdtAndWait {
                project.executeWriteCommand("Wrap with 'toString()'") {
                    contentElement.replace(newExpression)
                }
            }
            return true
        }

        return false
    }

    private data class ErrorCheckingResult(
        val bindingContext: BindingContext,
        val moduleDescriptor: ModuleDescriptor,
        val files: List<KtFile>
    )

    private fun checkForErrors(codeFragment: KtCodeFragment, debugProcess: DebugProcessImpl): ErrorCheckingResult {
        return runInReadActionWithWriteActionPriorityWithPCE {
            try {
                AnalyzingUtils.checkForSyntacticErrors(codeFragment)
            } catch (e: IllegalArgumentException) {
                evaluationException(e.message ?: e.toString())
            }

            val filesToAnalyze = listOf(codeFragment)
            val resolutionFacade = KotlinCacheService.getInstance(codeFragment.project).getResolutionFacade(filesToAnalyze)

            DebugLabelPropertyDescriptorProvider(codeFragment, debugProcess).supplyDebugLabels()

            val analysisResult = resolutionFacade.analyzeWithAllCompilerChecks(filesToAnalyze)

            if (analysisResult.isError()) {
                evaluationException(analysisResult.error)
            }

            val bindingContext = analysisResult.bindingContext

            bindingContext.diagnostics
                .filter { it.factory !in IGNORED_DIAGNOSTICS }
                .firstOrNull { it.severity == Severity.ERROR && it.psiElement.containingFile == codeFragment }
                ?.let { evaluationException(DefaultErrorMessages.render(it)) }

            ErrorCheckingResult(bindingContext, analysisResult.moduleDescriptor, Collections.singletonList(codeFragment))
        }
    }

    private fun evaluateWithCompilation(
        context: ExecutionContext,
        compiledData: CompiledDataDescriptor,
        classLoader: ClassLoaderReference
    ): Value? {
        return try {
            runEvaluation(context, compiledData, classLoader) { args ->
                val mainClassType = context.loadClassType(Type.getObjectType(GENERATED_CLASS_NAME), classLoader) as? ClassType
                    ?: error("Can not find class \"$GENERATED_CLASS_NAME\"")
                val mainMethod = mainClassType.methods().single { it.name() == GENERATED_FUNCTION_NAME }
                val returnValue = mainClassType.invokeMethod(context.thread, mainMethod, args, context.invokePolicy)
                EvaluatorValueConverter(context).unref(returnValue)
            }
        } catch (e: Throwable) {
            LOG.error("Unable to evaluate expression with compilation", e)
            return null
        }
    }

    private fun evaluateWithEval4J(
        context: ExecutionContext,
        compiledData: CompiledDataDescriptor,
        classLoader: ClassLoaderReference?
    ): InterpreterResult {
        val mainClassBytecode = compiledData.mainClass.bytes
        val mainClassAsmNode = ClassNode().apply { ClassReader(mainClassBytecode).accept(this, 0) }
        val mainMethod = mainClassAsmNode.methods.first { it.name == GENERATED_FUNCTION_NAME }

        return runEvaluation(context, compiledData, classLoader ?: context.evaluationContext.classLoader) { args ->
            val eval = JDIEval(context.vm, classLoader, context.thread, context.invokePolicy)
            interpreterLoop(mainMethod, makeInitialFrame(mainMethod, args.map { it.asValue() }), eval)
        }
    }

    private fun <T> runEvaluation(
        context: ExecutionContext,
        compiledData: CompiledDataDescriptor,
        classLoader: ClassLoaderReference?,
        block: (List<Value?>) -> T
    ): T {
        // Preload additional classes
        compiledData.classes
            .filter { !it.isMainClass }
            .forEach { context.loadClassType(Type.getObjectType(it.className), classLoader) }

        return context.vm.executeWithBreakpointsDisabled {
            for (parameterType in compiledData.mainMethodSignature.parameterTypes) {
                context.loadClassType(parameterType, classLoader)
            }
            val args = context.calculateMainMethodCallArguments(compiledData)
            block(args)
        }
    }

    private fun ExecutionContext.calculateMainMethodCallArguments(compiledData: CompiledDataDescriptor): List<Value?> {
        val asmValueParameters = compiledData.mainMethodSignature.parameterTypes
        val valueParameters = compiledData.parameters
        require(asmValueParameters.size == valueParameters.size)

        val args = valueParameters.zip(asmValueParameters)
        val variableFinder = VariableFinder.instance(this) ?: error("Frame map is not available")

        return args.map { (parameter, asmType) ->
            val result = variableFinder.find(parameter, asmType)

            if (result == null) {
                val name = parameter.debugString

                fun isInsideDefaultInterfaceMethod(): Boolean {
                    val method = evaluationContext.frameProxy?.safeLocation()?.safeMethod() ?: return false
                    val desc = method.signature()
                    return method.name().endsWith("\$default") && DEFAULT_METHOD_MARKERS.any { desc.contains("I${it.descriptor})") }
                }

                if (parameter in compiledData.crossingBounds) {
                    evaluationException("'$name' is not captured")
                } else if (parameter.kind == CodeFragmentParameter.Kind.FIELD_VAR) {
                    evaluationException("Cannot find the backing field '${parameter.name}'")
                } else if (parameter.kind == CodeFragmentParameter.Kind.ORDINARY && isInsideDefaultInterfaceMethod()) {
                    evaluationException("Parameter evaluation is not supported for '\$default' methods")
                } else {
                    throw VariableFinder.variableNotFound(evaluationContext, buildString {
                        append("Cannot find local variable: name = '").append(name).append("', type = ").append(asmType.className)
                    })
                }
            }

            result.value
        }
    }

    override fun getModifier() = null

    companion object {
        private val IGNORED_DIAGNOSTICS: Set<DiagnosticFactory<*>> = Errors.INVISIBLE_REFERENCE_DIAGNOSTICS

        private val DEFAULT_METHOD_MARKERS = listOf(AsmTypes.OBJECT_TYPE, AsmTypes.DEFAULT_CONSTRUCTOR_MARKER)

        private fun InterpreterResult.toJdiValue(context: ExecutionContext): com.sun.jdi.Value? {
            val jdiValue = when (this) {
                is ValueReturned -> result
                is ExceptionThrown -> {
                    when {
                        this.kind == ExceptionThrown.ExceptionKind.FROM_EVALUATED_CODE ->
                            evaluationException(InvocationException(this.exception.value as ObjectReference))
                        this.kind == ExceptionThrown.ExceptionKind.BROKEN_CODE ->
                            throw exception.value as Throwable
                        else ->
                            evaluationException(exception.toString())
                    }
                }
                is AbnormalTermination -> evaluationException(message)
                else -> throw IllegalStateException("Unknown result value produced by eval4j")
            }

            val sharedVar = if ((jdiValue is AbstractValue<*>)) getValueIfSharedVar(jdiValue, context) else null
            return sharedVar?.value ?: jdiValue.asJdiValue(context.vm, jdiValue.asmType)
        }

        private fun getValueIfSharedVar(value: Eval4JValue, context: ExecutionContext): VariableFinder.Result? {
            val obj = value.obj(value.asmType) as? ObjectReference ?: return null
            return VariableFinder.Result(EvaluatorValueConverter(context).unref(obj))
        }
    }
}

internal fun SuspendContext.getInvokePolicy(): Int {
    return if (suspendPolicy == EventRequest.SUSPEND_EVENT_THREAD) ObjectReference.INVOKE_SINGLE_THREADED else 0
}

fun Type.getClassDescriptor(
    scope: GlobalSearchScope,
    mapBuiltIns: Boolean = true,
    moduleDescriptor: ModuleDescriptor = DefaultBuiltIns.Instance.builtInsModule
): ClassDescriptor? {
    if (AsmUtil.isPrimitive(this)) return null

    val jvmName = JvmClassName.byInternalName(internalName).fqNameForClassNameWithoutDollars

    if (mapBuiltIns) {
        val mappedName = JavaToKotlinClassMap.mapJavaToKotlin(jvmName)
        if (mappedName != null) {
            moduleDescriptor.findClassAcrossModuleDependencies(mappedName)?.let { return it }
        }
    }

    return runReadAction {
        val classes = JavaPsiFacade.getInstance(scope.project).findClasses(jvmName.asString(), scope)
        if (classes.isEmpty()) null
        else {
            classes.first().getJavaClassDescriptor()
        }
    }
}

private fun <T> VirtualMachine.executeWithBreakpointsDisabled(block: () -> T): T {
    val allRequests = eventRequestManager().breakpointRequests() + eventRequestManager().classPrepareRequests()

    try {
        allRequests.forEach { it.disable() }
        return block()
    } finally {
        allRequests.forEach { it.enable() }
    }
}

private fun isSpecialException(th: Throwable): Boolean {
    return when (th) {
        is ClassNotPreparedException,
        is InternalException,
        is AbsentInformationException,
        is ClassNotLoadedException,
        is IncompatibleThreadStateException,
        is InconsistentDebugInfoException,
        is ObjectCollectedException,
        is VMDisconnectedException -> true
        else -> false
    }
}

private fun evaluationException(msg: String): Nothing = throw EvaluateExceptionUtil.createEvaluateException(msg)
private fun evaluationException(e: Throwable): Nothing = throw EvaluateExceptionUtil.createEvaluateException(e)