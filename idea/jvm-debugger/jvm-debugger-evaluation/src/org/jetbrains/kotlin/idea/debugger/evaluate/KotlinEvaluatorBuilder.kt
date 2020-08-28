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
import com.intellij.debugger.engine.evaluation.EvaluateException
import com.intellij.debugger.engine.evaluation.EvaluateExceptionUtil
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.engine.evaluation.expression.*
import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.psi.PsiElement
import com.intellij.testFramework.runInEdtAndWait
import com.sun.jdi.*
import com.sun.jdi.Value
import org.jetbrains.eval4j.*
import org.jetbrains.eval4j.Value as Eval4JValue
import org.jetbrains.eval4j.jdi.JDIEval
import org.jetbrains.eval4j.jdi.asJdiValue
import org.jetbrains.eval4j.jdi.asValue
import org.jetbrains.eval4j.jdi.makeInitialFrame
import org.jetbrains.kotlin.caches.resolve.KotlinCacheService
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.util.attachmentByPsiFile
import org.jetbrains.kotlin.idea.core.util.mergeAttachments
import org.jetbrains.kotlin.idea.core.util.runInReadActionWithWriteActionPriorityWithPCE
import org.jetbrains.kotlin.idea.debugger.*
import org.jetbrains.kotlin.idea.debugger.evaluate.KotlinDebuggerCaches.Companion.compileCodeFragmentCacheAware
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.GENERATED_CLASS_NAME
import org.jetbrains.kotlin.idea.debugger.evaluate.classLoading.GENERATED_FUNCTION_NAME
import org.jetbrains.kotlin.idea.debugger.evaluate.compilation.*
import org.jetbrains.kotlin.idea.debugger.evaluate.compilingEvaluator.loadClassesSafely
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.EvaluatorValueConverter
import org.jetbrains.kotlin.idea.debugger.evaluate.variables.VariableFinder
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.AnalyzingUtils
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.tree.ClassNode
import org.jetbrains.kotlin.idea.debugger.evaluate.EvaluationStatus.EvaluationContextLanguage
import org.jetbrains.kotlin.idea.debugger.evaluate.compilingEvaluator.ClassLoadingResult
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import java.util.*

internal val LOG = Logger.getInstance(KotlinEvaluator::class.java)

object KotlinEvaluatorBuilder : EvaluatorBuilder {
    override fun build(codeFragment: PsiElement, position: SourcePosition?): ExpressionEvaluator {
        if (codeFragment !is KtCodeFragment) {
            return EvaluatorBuilderImpl.getInstance().build(codeFragment, position)
        }

        val context = codeFragment.context
        val file = context?.containingFile

        if (file != null && file !is KtFile) {
            reportError(codeFragment, position, "Unknown context${codeFragment.context?.javaClass}")
            evaluationException(KotlinDebuggerEvaluationBundle.message("error.bad.context"))
        }

        return ExpressionEvaluatorImpl(KotlinEvaluator(codeFragment, position))
    }
}

class KotlinEvaluator(val codeFragment: KtCodeFragment, private val sourcePosition: SourcePosition?) : Evaluator {
    override fun evaluate(context: EvaluationContextImpl): Any? {
        if (codeFragment.text.isEmpty()) {
            return context.debugProcess.virtualMachineProxy.mirrorOfVoid()
        }

        val status = EvaluationStatus()

        val evaluationType = codeFragment.getUserData(KotlinCodeFragmentFactory.EVALUATION_TYPE)
        if (evaluationType != null) {
            status.evaluationType(evaluationType)
        }

        val language = runReadAction {
            when {
                codeFragment.getCopyableUserData(KtCodeFragment.FAKE_CONTEXT_FOR_JAVA_FILE) != null -> EvaluationContextLanguage.Java
                codeFragment.context?.language == KotlinLanguage.INSTANCE -> EvaluationContextLanguage.Kotlin
                else -> EvaluationContextLanguage.Other
            }
        }

        status.contextLanguage(language)

        try {
            return evaluateWithStatus(context, status)
        } finally {
            status.send()
        }
    }

    private fun evaluateWithStatus(context: EvaluationContextImpl, status: EvaluationStatus): Any? {
        runReadAction {
            if (DumbService.getInstance(codeFragment.project).isDumb) {
                status.error(EvaluationError.DumbMode)
                evaluationException(KotlinDebuggerEvaluationBundle.message("error.dumb.mode"))
            }
        }

        if (!context.debugProcess.isAttached) {
            status.error(EvaluationError.DebuggerNotAttached)
            throw EvaluateExceptionUtil.PROCESS_EXITED
        }

        val frameProxy = context.frameProxy ?: run {
            status.error(EvaluationError.NoFrameProxy)
            throw EvaluateExceptionUtil.NULL_STACK_FRAME
        }

        val operatingThread = context.suspendContext.thread ?: run {
            status.error(EvaluationError.ThreadNotAvailable)
            evaluationException(KotlinDebuggerEvaluationBundle.message("error.thread.unavailable"))
        }

        if (!operatingThread.isSuspended) {
            status.error(EvaluationError.ThreadNotSuspended)
            evaluationException(KotlinDebuggerEvaluationBundle.message("error.thread.not.suspended"))
        }

        try {
            val executionContext = ExecutionContext(context, frameProxy)
            return evaluateSafe(executionContext, status)
        } catch (e: EvaluateException) {
            val error = if (e.exceptionFromTargetVM != null) {
                EvaluationError.ExceptionFromEvaluatedCode
            } else {
                EvaluationError.EvaluateException
            }

            status.error(error)
            throw e
        } catch (e: ProcessCanceledException) {
            status.error(EvaluationError.ProcessCancelledException)
            evaluationException(e)
        } catch (e: Eval4JInterpretingException) {
            status.error(EvaluationError.InterpretingException)
            evaluationException(e.cause)
        } catch (e: Exception) {
            val isSpecialException = isSpecialException(e)
            if (isSpecialException) {
                status.error(EvaluationError.SpecialException)
                evaluationException(e)
            }

            status.error(EvaluationError.GenericException)
            reportError(codeFragment, sourcePosition, e.message ?: KotlinDebuggerEvaluationBundle.message("error.exception.occurred"), e)

            val cause = if (e.message != null) ": ${e.message}" else ""
            evaluationException(KotlinDebuggerEvaluationBundle.message("error.cant.evaluate") + cause)
        }
    }

    private fun evaluateSafe(context: ExecutionContext, status: EvaluationStatus): Any? {
        fun compilerFactory(): CompiledDataDescriptor = compileCodeFragment(context, status)

        val (compiledData, _) = compileCodeFragmentCacheAware(codeFragment, sourcePosition, ::compilerFactory, force = false)

        val classLoadingResult = loadClassesSafely(context, compiledData.classes)
        val classLoaderRef = (classLoadingResult as? ClassLoadingResult.Success)?.classLoader

        if (classLoadingResult is ClassLoadingResult.Failure) {
            status.classLoadingFailed()
        }

        val result = if (classLoaderRef != null) {
            try {
                status.usedEvaluator(EvaluationStatus.EvaluatorType.Bytecode)
                return evaluateWithCompilation(context, compiledData, classLoaderRef, status)
            } catch (e: Throwable) {
                status.compilingEvaluatorFailed()
                LOG.warn("Compiling evaluator failed", e)

                status.usedEvaluator(EvaluationStatus.EvaluatorType.Eval4j)
                evaluateWithEval4J(context, compiledData, classLoaderRef, status)
            }
        } else {
            status.usedEvaluator(EvaluationStatus.EvaluatorType.Eval4j)
            evaluateWithEval4J(context, compiledData, classLoaderRef, status)
        }

        return result.toJdiValue(context, status)
    }

    private fun compileCodeFragment(context: ExecutionContext, status: EvaluationStatus): CompiledDataDescriptor {
        val debugProcess = context.debugProcess
        var analysisResult = analyze(codeFragment, status, debugProcess)

        if (codeFragment.wrapToStringIfNeeded(analysisResult.bindingContext)) {
            // Repeat analysis with toString() added
            analysisResult = analyze(codeFragment, status, debugProcess)
        }

        val (bindingContext, filesToCompile) = runReadAction {
            val resolutionFacade = getResolutionFacadeForCodeFragment(codeFragment)
            DebuggerUtils.analyzeInlinedFunctions(resolutionFacade, codeFragment, false, analysisResult.bindingContext)
        }

        val moduleDescriptor = analysisResult.moduleDescriptor

        try {
            val result = CodeFragmentCompiler(context, status).compile(codeFragment, filesToCompile, bindingContext, moduleDescriptor)
            return createCompiledDataDescriptor(result, sourcePosition)
        } catch (e: Throwable) {
            status.error(EvaluationError.BackendException)
            throw e
        }
    }

    private fun KtCodeFragment.wrapToStringIfNeeded(bindingContext: BindingContext): Boolean {
        val expression = runReadAction {
            when (this) {
                is KtExpressionCodeFragment -> getContentElement()
                is KtBlockCodeFragment -> getContentElement().statements.lastOrNull()
                else -> {
                    LOG.error("Invalid code fragment type: ${this.javaClass}")
                    null
                }
            }
        } ?: return false

        return wrapToStringIfNeeded(expression, bindingContext)
    }

    private fun wrapToStringIfNeeded(expression: KtExpression, bindingContext: BindingContext): Boolean {
        val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE_INFO, expression]?.type ?: return false
        if (expressionType.isInlineClassType()) {
            val newExpression = runReadAction {
                val expressionText = expression.text
                KtPsiFactory(expression.project).createExpression("($expressionText).toString()")
            }
            runInEdtAndWait {
                expression.project.executeWriteCommand(KotlinDebuggerEvaluationBundle.message("wrap.with.tostring")) {
                    expression.replace(newExpression)
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

    private fun analyze(codeFragment: KtCodeFragment, status: EvaluationStatus, debugProcess: DebugProcessImpl): ErrorCheckingResult {
        return runInReadActionWithWriteActionPriorityWithPCE {
            try {
                AnalyzingUtils.checkForSyntacticErrors(codeFragment)
            } catch (e: IllegalArgumentException) {
                status.error(EvaluationError.ErrorElementOccurred)
                evaluationException(e.message ?: e.toString())
            }

            val resolutionFacade = getResolutionFacadeForCodeFragment(codeFragment)

            DebugLabelPropertyDescriptorProvider(codeFragment, debugProcess).supplyDebugLabels()

            val analysisResult = resolutionFacade.analyzeWithAllCompilerChecks(listOf(codeFragment))

            if (analysisResult.isError()) {
                status.error(EvaluationError.FrontendException)
                evaluationException(analysisResult.error)
            }

            val bindingContext = analysisResult.bindingContext

            bindingContext.diagnostics
                .filter { it.factory !in IGNORED_DIAGNOSTICS }
                .firstOrNull { it.severity == Severity.ERROR && it.psiElement.containingFile == codeFragment }
                ?.let {
                    status.error(EvaluationError.ErrorsInCode)
                    evaluationException(DefaultErrorMessages.render(it))
                }

            ErrorCheckingResult(bindingContext, analysisResult.moduleDescriptor, Collections.singletonList(codeFragment))
        }
    }

    private fun evaluateWithCompilation(
        context: ExecutionContext,
        compiledData: CompiledDataDescriptor,
        classLoader: ClassLoaderReference,
        status: EvaluationStatus
    ): Value? {
        return runEvaluation(context, compiledData, classLoader, status) { args ->
            val mainClassType = context.findClass(GENERATED_CLASS_NAME, classLoader) as? ClassType
                ?: error("Can not find class \"$GENERATED_CLASS_NAME\"")
            val mainMethod = mainClassType.methods().single { it.name() == GENERATED_FUNCTION_NAME }
            val returnValue = context.invokeMethod(mainClassType, mainMethod, args)
            EvaluatorValueConverter(context).unref(returnValue)
        }
    }

    private fun evaluateWithEval4J(
        context: ExecutionContext,
        compiledData: CompiledDataDescriptor,
        classLoader: ClassLoaderReference?,
        status: EvaluationStatus
    ): InterpreterResult {
        val mainClassBytecode = compiledData.mainClass.bytes
        val mainClassAsmNode = ClassNode().apply { ClassReader(mainClassBytecode).accept(this, 0) }
        val mainMethod = mainClassAsmNode.methods.first { it.name == GENERATED_FUNCTION_NAME }

        return runEvaluation(context, compiledData, classLoader ?: context.evaluationContext.classLoader, status) { args ->
            val vm = context.vm.virtualMachine
            val thread = context.suspendContext.thread?.threadReference?.takeIf { it.isSuspended }
                ?: error("Can not find a thread to run evaluation on")

            val eval = object : JDIEval(vm, classLoader, thread, context.invokePolicy) {
                override fun jdiInvokeStaticMethod(type: ClassType, method: Method, args: List<Value?>, invokePolicy: Int): Value? {
                    return context.invokeMethod(type, method, args)
                }

                override fun jdiInvokeStaticMethod(type: InterfaceType, method: Method, args: List<Value?>, invokePolicy: Int): Value? {
                    return context.invokeMethod(type, method, args)
                }

                override fun jdiInvokeMethod(obj: ObjectReference, method: Method, args: List<Value?>, policy: Int): Value? {
                    return context.invokeMethod(obj, method, args, ObjectReference.INVOKE_NONVIRTUAL)
                }
            }
            interpreterLoop(mainMethod, makeInitialFrame(mainMethod, args.map { it.asValue() }), eval)
        }
    }

    private fun <T> runEvaluation(
        context: ExecutionContext,
        compiledData: CompiledDataDescriptor,
        classLoader: ClassLoaderReference?,
        status: EvaluationStatus,
        block: (List<Value?>) -> T
    ): T {
        // Preload additional classes
        compiledData.classes
            .filter { !it.isMainClass }
            .forEach { context.findClass(it.className, classLoader) }

        for (parameterType in compiledData.mainMethodSignature.parameterTypes) {
            context.findClass(parameterType, classLoader)
        }

        val variableFinder = VariableFinder(context)
        val args = calculateMainMethodCallArguments(variableFinder, compiledData, status)

        val result = block(args)

        for (wrapper in variableFinder.refWrappers) {
            updateLocalVariableValue(variableFinder.evaluatorValueConverter, wrapper)
        }

        return result
    }

    private fun updateLocalVariableValue(converter: EvaluatorValueConverter, ref: VariableFinder.RefWrapper) {
        val frameProxy = converter.context.frameProxy
        val variable = frameProxy.safeVisibleVariableByName(ref.localVariableName) ?: return
        val newValue = converter.unref(ref.wrapper)

        try {
            frameProxy.setValue(variable, newValue)
        } catch (e: InvalidTypeException) {
            LOG.error("Cannot update local variable value: expected type ${variable.type}, actual type ${newValue?.type()}", e)
        }
    }

    private fun calculateMainMethodCallArguments(
        variableFinder: VariableFinder,
        compiledData: CompiledDataDescriptor,
        status: EvaluationStatus
    ): List<Value?> {
        val asmValueParameters = compiledData.mainMethodSignature.parameterTypes
        val valueParameters = compiledData.parameters
        require(asmValueParameters.size == valueParameters.size)

        val args = valueParameters.zip(asmValueParameters)

        return args.map { (parameter, asmType) ->
            val result = variableFinder.find(parameter, asmType)

            if (result == null) {
                val name = parameter.debugString

                fun isInsideDefaultInterfaceMethod(): Boolean {
                    val method = variableFinder.context.frameProxy.safeLocation()?.safeMethod() ?: return false
                    val desc = method.signature()
                    return method.name().endsWith("\$default") && DEFAULT_METHOD_MARKERS.any { desc.contains("I${it.descriptor})") }
                }

                if (parameter.kind == CodeFragmentParameter.Kind.COROUTINE_CONTEXT) {
                    status.error(EvaluationError.CoroutineContextUnavailable)
                    evaluationException(KotlinDebuggerEvaluationBundle.message("error.coroutine.context.unavailable"))
                } else if (parameter in compiledData.crossingBounds) {
                    status.error(EvaluationError.ParameterNotCaptured)
                    evaluationException(KotlinDebuggerEvaluationBundle.message("error.not.captured", name))
                } else if (parameter.kind == CodeFragmentParameter.Kind.FIELD_VAR) {
                    status.error(EvaluationError.BackingFieldNotFound)
                    evaluationException(KotlinDebuggerEvaluationBundle.message("error.cant.find.backing.field", parameter.name))
                } else if (parameter.kind == CodeFragmentParameter.Kind.ORDINARY && isInsideDefaultInterfaceMethod()) {
                    status.error(EvaluationError.InsideDefaultMethod)
                    evaluationException(KotlinDebuggerEvaluationBundle.message("error.parameter.evaluation.default.methods"))
                } else {
                    status.error(EvaluationError.CannotFindVariable)
                    evaluationException(KotlinDebuggerEvaluationBundle.message("error.cant.find.variable", name, asmType.className))
                }
            }

            result.value
        }
    }

    override fun getModifier() = null

    companion object {
        private val IGNORED_DIAGNOSTICS: Set<DiagnosticFactory<*>> =
            Errors.INVISIBLE_REFERENCE_DIAGNOSTICS + setOf(Errors.EXPERIMENTAL_API_USAGE_ERROR)

        private val DEFAULT_METHOD_MARKERS = listOf(AsmTypes.OBJECT_TYPE, AsmTypes.DEFAULT_CONSTRUCTOR_MARKER)

        private fun InterpreterResult.toJdiValue(context: ExecutionContext, status: EvaluationStatus): Value? {
            val jdiValue = when (this) {
                is ValueReturned -> result
                is ExceptionThrown -> {
                    when {
                        this.kind == ExceptionThrown.ExceptionKind.FROM_EVALUATED_CODE -> {
                            status.error(EvaluationError.ExceptionFromEvaluatedCode)
                            evaluationException(InvocationException(this.exception.value as ObjectReference))
                        }
                        this.kind == ExceptionThrown.ExceptionKind.BROKEN_CODE ->
                            throw exception.value as Throwable
                        else -> {
                            status.error(EvaluationError.Eval4JUnknownException)
                            evaluationException(exception.toString())
                        }
                    }
                }
                is AbnormalTermination -> {
                    status.error(EvaluationError.Eval4JAbnormalTermination)
                    evaluationException(message)
                }
                else -> throw IllegalStateException("Unknown result value produced by eval4j")
            }

            val sharedVar = if ((jdiValue is AbstractValue<*>)) getValueIfSharedVar(jdiValue, context) else null
            return sharedVar?.value ?: jdiValue.asJdiValue(context.vm.virtualMachine, jdiValue.asmType)
        }

        private fun getValueIfSharedVar(value: Eval4JValue, context: ExecutionContext): VariableFinder.Result? {
            val obj = value.obj(value.asmType) as? ObjectReference ?: return null
            return VariableFinder.Result(EvaluatorValueConverter(context).unref(obj))
        }
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

private fun reportError(codeFragment: KtCodeFragment, position: SourcePosition?, message: String, throwable: Throwable? = null) {
    runReadAction {
        val contextFile = codeFragment.context?.containingFile

        val attachments = arrayOf(
            attachmentByPsiFile(contextFile),
            attachmentByPsiFile(codeFragment),
            Attachment("breakpoint.info", "Position: " + position?.run { "${file.name}:$line" }),
            Attachment("context.info", runReadAction { codeFragment.context?.text ?: "null" })
        )

        LOG.error(
            "Cannot evaluate a code fragment of type " + codeFragment::class.java + ": " + message.decapitalize(),
            throwable,
            mergeAttachments(*attachments)
        )
    }
}

fun createCompiledDataDescriptor(result: CodeFragmentCompiler.CompilationResult, sourcePosition: SourcePosition?): CompiledDataDescriptor {
    val localFunctionSuffixes = result.localFunctionSuffixes

    val dumbParameters = ArrayList<CodeFragmentParameter.Dumb>(result.parameterInfo.parameters.size)
    for (parameter in result.parameterInfo.parameters) {
        val dumb = parameter.dumb
        if (dumb.kind == CodeFragmentParameter.Kind.LOCAL_FUNCTION) {
            val suffix = localFunctionSuffixes[dumb]
            if (suffix != null) {
                dumbParameters += dumb.copy(name = dumb.name + suffix)
                continue
            }
        }

        dumbParameters += dumb
    }

    return CompiledDataDescriptor(
        result.classes,
        dumbParameters,
        result.parameterInfo.crossingBounds,
        result.mainMethodSignature,
        sourcePosition
    )
}

private fun evaluationException(msg: String): Nothing = throw EvaluateExceptionUtil.createEvaluateException(msg)
private fun evaluationException(e: Throwable): Nothing = throw EvaluateExceptionUtil.createEvaluateException(e)

internal fun getResolutionFacadeForCodeFragment(codeFragment: KtCodeFragment): ResolutionFacade {
    val filesToAnalyze = listOf(codeFragment)
    val kotlinCacheService = KotlinCacheService.getInstance(codeFragment.project)
    return kotlinCacheService.getResolutionFacade(filesToAnalyze, JvmPlatforms.unspecifiedJvmPlatform)
}