/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinableParameterExpression
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class PsiInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    private val functionDescriptor: FunctionDescriptor,
    signature: JvmMethodSignature,
    typeParameterMappings: TypeParameterMappings<KotlinType>,
    sourceCompiler: SourceCompilerForInline,
    private val methodOwner: Type,
    private val actualDispatchReceiver: Type,
    reportErrorsOn: KtElement,
) : InlineCodegen<ExpressionCodegen>(
    codegen, state, signature, typeParameterMappings, sourceCompiler,
    ReifiedTypeInliner(
        typeParameterMappings, PsiInlineIntrinsicsSupport(state, reportErrorsOn), codegen.typeSystem,
        state.languageVersionSettings, state.config.unifiedNullChecks
    ),
), CallGenerator {
    override fun generateAssertField() =
        codegen.parentCodegen.generateAssertField()

    override fun genCallInner(
        callableMethod: Callable,
        resolvedCall: ResolvedCall<*>?,
        callDefault: Boolean,
        codegen: ExpressionCodegen
    ) {
        (sourceCompiler as PsiSourceCompilerForInline).callDefault = callDefault
        assert(hiddenParameters.isEmpty()) { "putHiddenParamsIntoLocals() should be called after processHiddenParameters()" }
        val psiElement = resolvedCall?.call?.callElement
        val element = psiElement?.let(::PsiInlineFunctionSource)
        if (!state.globalInlineContext.enterIntoInlining(functionDescriptor, element) { reportOn, callee ->
                state.diagnostics.report(Errors.INLINE_CALL_CYCLE.on((reportOn as PsiInlineFunctionSource).psi, callee))
            }) {
            generateStub(psiElement?.text ?: "<no source>", codegen)
            return
        }
        try {
            for (info in closuresToGenerate) {
                // Can't be done immediately in `rememberClosure` for some reason:
                info.generateLambdaBody(sourceCompiler)
                // Requires `generateLambdaBody` first if the closure is non-empty (for bound callable references,
                // or indeed any callable references, it *is* empty, so this was done in `rememberClosure`):
                if (!info.isBoundCallableReference) {
                    putClosureParametersOnStack(info, null)
                }
            }
            performInline(registerLineNumberAfterwards(resolvedCall), functionDescriptor.isInlineOnly())
        } finally {
            state.globalInlineContext.exitFromInlining()
        }
    }

    private fun registerLineNumberAfterwards(resolvedCall: ResolvedCall<*>?): Boolean {
        val callElement = resolvedCall?.call?.callElement ?: return false
        val parentIfCondition = callElement.getParentOfType<KtIfExpression>(true)?.condition ?: return false
        return parentIfCondition.isAncestor(callElement, false)
    }

    private val hiddenParameters = mutableListOf<Pair<ParameterInfo, Int>>()

    override fun processHiddenParameters() {
        if (!DescriptorAsmUtil.isStaticMethod((sourceCompiler as PsiSourceCompilerForInline).context.contextKind, functionDescriptor)) {
            hiddenParameters += invocationParamBuilder.addNextParameter(methodOwner, false, actualDispatchReceiver) to
                    codegen.frameMap.enterTemp(methodOwner)
        }
        for (param in jvmSignature.valueParameters) {
            if (param.kind == JvmMethodParameterKind.VALUE) {
                break
            }
            hiddenParameters += invocationParamBuilder.addNextParameter(param.asmType, false) to
                    codegen.frameMap.enterTemp(param.asmType)
        }
        // TODO: Add context receivers as hiddenParameters
    }

    override fun putHiddenParamsIntoLocals() {
        for (i in hiddenParameters.indices.reversed()) {
            val (param, offset) = hiddenParameters[i]
            StackValue.local(offset, param.type).store(StackValue.onStack(param.typeOnStack), codegen.visitor)
        }
        hiddenParameters.clear()
    }

    override fun genValueAndPut(
        valueParameterDescriptor: ValueParameterDescriptor?,
        argumentExpression: KtExpression,
        parameterType: JvmKotlinType,
        parameterIndex: Int
    ) {
        requireNotNull(valueParameterDescriptor) {
            "Parameter descriptor can only be null in case a @PolymorphicSignature function is called, " +
                    "which cannot be declared in Kotlin and thus be inline: $codegen"
        }

        val isInlineParameter = InlineUtil.isInlineParameter(valueParameterDescriptor)
        //TODO deparenthesize typed
        if (isInlineParameter && isInlinableParameterExpression(KtPsiUtil.deparenthesize(argumentExpression))) {
            rememberClosure(argumentExpression, parameterType.type, valueParameterDescriptor)
        } else {
            val value = codegen.gen(argumentExpression)
            val kind = when {
                isCallSiteIsSuspend(valueParameterDescriptor) && parameterType.kotlinType?.isSuspendFunctionTypeOrSubtype == true ->
                    ValueKind.READ_OF_INLINE_LAMBDA_FOR_INLINE_SUSPEND_PARAMETER
                isInlineSuspendParameter(valueParameterDescriptor) -> ValueKind.READ_OF_OBJECT_FOR_INLINE_SUSPEND_PARAMETER
                else -> ValueKind.GENERAL
            }
            putValueIfNeeded(parameterType, value, kind, parameterIndex)
        }
    }

    private fun isInlineSuspendParameter(descriptor: ValueParameterDescriptor): Boolean =
        functionDescriptor.isInline && !descriptor.isNoinline && descriptor.type.isSuspendFunctionTypeOrSubtype

    private fun isCallSiteIsSuspend(descriptor: ValueParameterDescriptor): Boolean =
        state.bindingContext[CodegenBinding.CALL_SITE_IS_SUSPEND_FOR_CROSSINLINE_LAMBDA, descriptor] == true

    private val closuresToGenerate = mutableListOf<PsiExpressionLambda>()

    private fun rememberClosure(expression: KtExpression, type: Type, parameter: ValueParameterDescriptor) {
        val ktLambda = KtPsiUtil.deparenthesize(expression)
        assert(isInlinableParameterExpression(ktLambda)) { "Couldn't find inline expression in ${expression.text}" }

        val boundReceiver = if (ktLambda is KtCallableReferenceExpression) {
            val resolvedCall = ktLambda.callableReference.getResolvedCallWithAssert(state.bindingContext)
            JvmCodegenUtil.getBoundCallableReferenceReceiver(resolvedCall)
        } else null

        val lambda = PsiExpressionLambda(ktLambda!!, state, parameter.isCrossinline, boundReceiver != null)
        rememberClosure(type, parameter.index, lambda)
        closuresToGenerate += lambda
        if (boundReceiver != null) {
            // Has to be done immediately to preserve evaluation order.
            val receiver = codegen.generateReceiverValue(boundReceiver, false)
            val receiverKotlinType = receiver.kotlinType
            val boxedReceiver =
                if (receiverKotlinType != null)
                    DescriptorAsmUtil.boxType(receiver.type, receiverKotlinType, state.typeMapper)
                else
                    AsmUtil.boxType(receiver.type)
            val receiverValue = StackValue.coercion(receiver, boxedReceiver, receiverKotlinType)
            putClosureParametersOnStack(lambda, receiverValue)
        }
    }

    var activeLambda: PsiExpressionLambda? = null
        private set

    private fun putClosureParametersOnStack(next: PsiExpressionLambda, receiverValue: StackValue?) {
        activeLambda = next
        codegen.pushClosureOnStack(next.classDescriptor, true, this, receiverValue)
        activeLambda = null
    }

    override fun putValueIfNeeded(parameterType: JvmKotlinType, value: StackValue, kind: ValueKind, parameterIndex: Int) =
        putArgumentToLocalVal(parameterType, value, parameterIndex, kind)

    override fun putCapturedValueOnStack(stackValue: StackValue, valueType: Type, paramIndex: Int) =
        putCapturedToLocalVal(stackValue, activeLambda!!.capturedVars[paramIndex], stackValue.kotlinType)

    override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) = Unit

    override fun isInlinedToInlineFunInKotlinRuntime(): Boolean {
        val caller = this.codegen.context.functionDescriptor
        if (!caller.isInline) return false
        val callerPackage = DescriptorUtils.getParentOfType(caller, PackageFragmentDescriptor::class.java) ?: return false
        return callerPackage.fqName.asString().startsWith("kotlin.")
    }

    private class PsiInlineFunctionSource(val psi: PsiElement) : GlobalInlineContext.InlineFunctionSource()
}

private val FunctionDescriptor.explicitParameters
    get() = listOfNotNull(extensionReceiverParameter) + valueParameters

class PsiExpressionLambda(
    expression: KtExpression,
    private val state: GenerationState,
    val isCrossInline: Boolean,
    val isBoundCallableReference: Boolean
) : ExpressionLambda() {
    override val lambdaClassType: Type

    override val invokeMethod: Method

    val invokeMethodDescriptor: FunctionDescriptor

    override val invokeMethodParameters: List<KotlinType?>
        get() {
            val actualInvokeDescriptor = if (invokeMethodDescriptor.isSuspend)
                getOrCreateJvmSuspendFunctionView(invokeMethodDescriptor, state)
            else
                invokeMethodDescriptor
            return actualInvokeDescriptor.explicitParameters.map { it.returnType }
        }

    override val invokeMethodReturnType: KotlinType?
        get() = invokeMethodDescriptor.returnType

    val classDescriptor: ClassDescriptor

    val propertyReferenceInfo: PropertyReferenceInfo?

    val functionWithBodyOrCallableReference: KtExpression = (expression as? KtLambdaExpression)?.functionLiteral ?: expression

    override val returnLabels: Map<String, Label?>

    val closure: CalculatedClosure

    init {
        val bindingContext = state.bindingContext
        val function = bindingContext.get(BindingContext.FUNCTION, functionWithBodyOrCallableReference)
        if (function == null && expression is KtCallableReferenceExpression) {
            val variableDescriptor =
                bindingContext.get(BindingContext.VARIABLE, functionWithBodyOrCallableReference) as? VariableDescriptorWithAccessors
                    ?: throw AssertionError("Reference expression not resolved to variable descriptor with accessors: ${expression.getText()}")
            classDescriptor = bindingContext.get(CodegenBinding.CLASS_FOR_CALLABLE, variableDescriptor)
                ?: throw IllegalStateException("Class for callable not found: $variableDescriptor\n${expression.text}")
            lambdaClassType = state.typeMapper.mapClass(classDescriptor)
            val getFunction = PropertyReferenceCodegen.findGetFunction(variableDescriptor)
            invokeMethodDescriptor = PropertyReferenceCodegen.createFakeOpenDescriptor(getFunction, classDescriptor)
            val resolvedCall = expression.callableReference.getResolvedCallWithAssert(bindingContext)
            propertyReferenceInfo = PropertyReferenceInfo(resolvedCall.resultingDescriptor as VariableDescriptor, getFunction)
        } else {
            propertyReferenceInfo = null
            invokeMethodDescriptor = function ?: throw AssertionError("Function is not resolved to descriptor: " + expression.text)
            classDescriptor = bindingContext.get(CodegenBinding.CLASS_FOR_CALLABLE, invokeMethodDescriptor)
                ?: throw IllegalStateException("Class for invoke method not found: $invokeMethodDescriptor\n${expression.text}")
            lambdaClassType = CodegenBinding.asmTypeForAnonymousClass(bindingContext, invokeMethodDescriptor)
        }

        closure = bindingContext.get(CodegenBinding.CLOSURE, classDescriptor)
            ?: throw AssertionError("null closure for lambda ${expression.text}")
        returnLabels = getDeclarationLabels(expression, invokeMethodDescriptor).associateWith { null }
        invokeMethod = state.typeMapper.mapAsmMethod(invokeMethodDescriptor)
    }

    // This can only be computed after generating the body, hence `lazy`.
    override val capturedVars: List<CapturedParamDesc> by lazy {
        arrayListOf<CapturedParamDesc>().apply {
            val captureThis = closure.capturedOuterClassDescriptor
            if (captureThis != null) {
                add(capturedParamDesc(AsmUtil.CAPTURED_THIS_FIELD, state.typeMapper.mapType(captureThis.defaultType), isSuspend = false))
            }

            val capturedReceiver = closure.capturedReceiverFromOuterContext
            if (capturedReceiver != null) {
                val fieldName = closure.getCapturedReceiverFieldName(state.typeMapper.bindingContext, state.languageVersionSettings)
                val type = if (isBoundCallableReference)
                    state.typeMapper.mapType(capturedReceiver, null, TypeMappingMode.GENERIC_ARGUMENT)
                else
                    state.typeMapper.mapType(capturedReceiver)
                add(capturedParamDesc(fieldName, type, isSuspend = false))
            }

            closure.captureVariables.forEach { (parameter, value) ->
                val isSuspend = parameter is ValueParameterDescriptor && parameter.type.isSuspendFunctionTypeOrSubtype
                add(capturedParamDesc(value.fieldName, value.type, isSuspend))
            }
        }
    }

    val isPropertyReference: Boolean
        get() = propertyReferenceInfo != null
}
