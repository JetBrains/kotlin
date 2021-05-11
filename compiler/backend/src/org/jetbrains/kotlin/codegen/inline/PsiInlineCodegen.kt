/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.getMethodAsmFlags
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.context.EnclosedValueDescriptor
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.coroutines.isCapturedSuspendLambda
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.isReleaseCoroutines
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinableParameterExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class PsiInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    function: FunctionDescriptor,
    methodOwner: Type,
    signature: JvmMethodSignature,
    typeParameterMappings: TypeParameterMappings<KotlinType>,
    sourceCompiler: SourceCompilerForInline,
    private val actualDispatchReceiver: Type = methodOwner
) : InlineCodegen<ExpressionCodegen>(
    codegen, state, function, methodOwner, signature, typeParameterMappings, sourceCompiler,
    ReifiedTypeInliner(
        typeParameterMappings, PsiInlineIntrinsicsSupport(state), codegen.typeSystem,
        state.languageVersionSettings, state.unifiedNullChecks
    ),
), CallGenerator {
    override fun generateAssertFieldIfNeeded(info: RootInliningContext) {
        if (info.generateAssertField) {
            codegen.parentCodegen.generateAssertField()
        }
    }

    override fun genCallInner(
        callableMethod: Callable,
        resolvedCall: ResolvedCall<*>?,
        callDefault: Boolean,
        codegen: ExpressionCodegen
    ) {
        if (!state.globalInlineContext.enterIntoInlining(functionDescriptor, resolvedCall?.call?.callElement)) {
            generateStub(resolvedCall?.call?.callElement?.text ?: "<no source>", codegen)
            return
        }
        try {
            val registerLineNumber = registerLineNumberAfterwards(resolvedCall)
            performInline(resolvedCall?.typeArguments?.keys?.toList(), callDefault, callDefault, codegen.typeSystem, registerLineNumber)
        } finally {
            state.globalInlineContext.exitFromInlining()
        }
    }

    private fun registerLineNumberAfterwards(resolvedCall: ResolvedCall<*>?): Boolean {
        val callElement = resolvedCall?.call?.callElement ?: return false
        val parentIfCondition = callElement.getParentOfType<KtIfExpression>(true)?.condition ?: return false
        return parentIfCondition.isAncestor(callElement, false)
    }

    override fun processAndPutHiddenParameters(justProcess: Boolean) {
        if (getMethodAsmFlags(functionDescriptor, sourceCompiler.contextKind, state) and Opcodes.ACC_STATIC == 0) {
            invocationParamBuilder.addNextParameter(methodOwner, false, actualDispatchReceiver)
        }

        for (param in jvmSignature.valueParameters) {
            if (param.kind == JvmMethodParameterKind.VALUE) {
                break
            }
            invocationParamBuilder.addNextParameter(param.asmType, false)
        }

        invocationParamBuilder.markValueParametersStart()
        val hiddenParameters = invocationParamBuilder.buildParameters().parameters

        delayedHiddenWriting = recordParameterValueInLocalVal(justProcess, false, *hiddenParameters.toTypedArray())
    }

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        activeLambda = next
        when (next) {
            is PsiExpressionLambda -> codegen.pushClosureOnStack(next.classDescriptor, true, this, functionReferenceReceiver)
            is DefaultLambda -> rememberCapturedForDefaultLambda(next)
            else -> throw RuntimeException("Unknown lambda: $next")
        }
        activeLambda = null
    }

    private fun getBoundCallableReferenceReceiver(argumentExpression: KtExpression): ReceiverValue? {
        val deparenthesized = KtPsiUtil.deparenthesize(argumentExpression) as? KtCallableReferenceExpression ?: return null
        val resolvedCall = deparenthesized.callableReference.getResolvedCallWithAssert(state.bindingContext)
        return JvmCodegenUtil.getBoundCallableReferenceReceiver(resolvedCall)
    }

    /*lambda or callable reference*/
    private fun isInliningParameter(expression: KtExpression, valueParameterDescriptor: ValueParameterDescriptor): Boolean {
        //TODO deparenthesize typed
        val deparenthesized = KtPsiUtil.deparenthesize(expression)

        return InlineUtil.isInlineParameter(valueParameterDescriptor) && isInlinableParameterExpression(deparenthesized)
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

        if (isInliningParameter(argumentExpression, valueParameterDescriptor)) {
            val lambdaInfo = rememberClosure(argumentExpression, parameterType.type, valueParameterDescriptor)

            val receiverValue = getBoundCallableReferenceReceiver(argumentExpression)
            if (receiverValue != null) {
                val receiver = codegen.generateReceiverValue(receiverValue, false)
                val receiverKotlinType = receiver.kotlinType
                val boxedReceiver =
                    if (receiverKotlinType != null)
                        DescriptorAsmUtil.boxType(receiver.type, receiverKotlinType, state.typeMapper)
                    else
                        AsmUtil.boxType(receiver.type)

                putClosureParametersOnStack(
                    lambdaInfo,
                    StackValue.coercion(receiver, boxedReceiver, receiverKotlinType)
                )
            }
        } else {
            val value = codegen.gen(argumentExpression)
            val kind = when {
                isCallSiteIsSuspend(valueParameterDescriptor) && parameterType.kotlinType?.isSuspendFunctionTypeOrSubtype == true ->
                    ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND
                isInlineSuspendParameter(valueParameterDescriptor) -> ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_SUSPEND_PARAMETER
                else -> ValueKind.GENERAL
            }
            putValueIfNeeded(parameterType, value, kind, parameterIndex)
        }
    }

    private fun isInlineSuspendParameter(descriptor: ValueParameterDescriptor): Boolean =
        functionDescriptor.isInline && !descriptor.isNoinline && descriptor.type.isSuspendFunctionTypeOrSubtype

    private fun isCallSiteIsSuspend(descriptor: ValueParameterDescriptor): Boolean =
        state.bindingContext[CodegenBinding.CALL_SITE_IS_SUSPEND_FOR_CROSSINLINE_LAMBDA, descriptor] == true

    private fun rememberClosure(expression: KtExpression, type: Type, parameter: ValueParameterDescriptor): LambdaInfo {
        val ktLambda = KtPsiUtil.deparenthesize(expression)
        assert(isInlinableParameterExpression(ktLambda)) { "Couldn't find inline expression in ${expression.text}" }

        return PsiExpressionLambda(
            ktLambda!!, state.typeMapper, state.languageVersionSettings,
            parameter.isCrossinline, getBoundCallableReferenceReceiver(expression) != null
        ).also { lambda ->
            val closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.index)
            closureInfo.functionalArgument = lambda
            expressionMap[closureInfo.index] = lambda
        }
    }

    override fun putValueIfNeeded(parameterType: JvmKotlinType, value: StackValue, kind: ValueKind, parameterIndex: Int) {
        if (processDefaultMaskOrMethodHandler(value, kind)) return

        assert(maskValues.isEmpty()) { "Additional default call arguments should be last ones, but $value" }

        putArgumentOrCapturedToLocalVal(parameterType, value, -1, parameterIndex, kind)
    }

    override fun putCapturedValueOnStack(stackValue: StackValue, valueType: Type, paramIndex: Int) {
        putArgumentOrCapturedToLocalVal(
            JvmKotlinType(stackValue.type, stackValue.kotlinType), stackValue, paramIndex, paramIndex, ValueKind.CAPTURED
        )
    }

    override fun reorderArgumentsIfNeeded(actualArgsWithDeclIndex: List<ArgumentAndDeclIndex>, valueParameterTypes: List<Type>) = Unit

    override fun putHiddenParamsIntoLocals() {
        assert(delayedHiddenWriting != null) { "processAndPutHiddenParameters(true) should be called before putHiddenParamsIntoLocals" }
        delayedHiddenWriting!!.invoke()
        delayedHiddenWriting = null
    }

    override fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda> {
        return expandMaskConditionsAndUpdateVariableNodes(
            node, maskStartIndex, maskValues, methodHandleInDefaultMethodIndex,
            extractDefaultLambdaOffsetAndDescriptor(jvmSignature, functionDescriptor),
            ::PsiDefaultLambda
        )
    }

    override fun descriptorIsDeserialized(memberDescriptor: CallableMemberDescriptor): Boolean =
        memberDescriptor is DescriptorWithContainerSource
}

private val FunctionDescriptor.explicitParameters
    get() = listOfNotNull(extensionReceiverParameter) + valueParameters

class PsiExpressionLambda(
    expression: KtExpression,
    private val typeMapper: KotlinTypeMapper,
    private val languageVersionSettings: LanguageVersionSettings,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean
) : ExpressionLambda(isCrossInline) {

    override val lambdaClassType: Type

    override val invokeMethod: Method

    val invokeMethodDescriptor: FunctionDescriptor

    override val invokeMethodParameters: List<KotlinType?>
        get() {
            val actualInvokeDescriptor = if (isSuspend)
                getOrCreateJvmSuspendFunctionView(
                    invokeMethodDescriptor, languageVersionSettings.isReleaseCoroutines(), typeMapper.bindingContext
                )
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

    override val isSuspend: Boolean

    val closure: CalculatedClosure

    init {
        val bindingContext = typeMapper.bindingContext
        val function = bindingContext.get(BindingContext.FUNCTION, functionWithBodyOrCallableReference)
        if (function == null && expression is KtCallableReferenceExpression) {
            val variableDescriptor =
                bindingContext.get(BindingContext.VARIABLE, functionWithBodyOrCallableReference) as? VariableDescriptorWithAccessors
                    ?: throw AssertionError("Reference expression not resolved to variable descriptor with accessors: ${expression.getText()}")
            classDescriptor = bindingContext.get(CodegenBinding.CLASS_FOR_CALLABLE, variableDescriptor)
                ?: throw IllegalStateException("Class for callable not found: $variableDescriptor\n${expression.text}")
            lambdaClassType = typeMapper.mapClass(classDescriptor)
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
        returnLabels = InlineCodegen.getDeclarationLabels(expression, invokeMethodDescriptor).associateWith { null }
        invokeMethod = typeMapper.mapAsmMethod(invokeMethodDescriptor)
        isSuspend = invokeMethodDescriptor.isSuspend
    }

    override val capturedVars: List<CapturedParamDesc> by lazy {
        arrayListOf<CapturedParamDesc>().apply {
            val captureThis = closure.capturedOuterClassDescriptor
            if (captureThis != null) {
                val kotlinType = captureThis.defaultType
                val type = typeMapper.mapType(kotlinType)
                val descriptor = EnclosedValueDescriptor(
                    AsmUtil.CAPTURED_THIS_FIELD, null,
                    StackValue.field(type, lambdaClassType, AsmUtil.CAPTURED_THIS_FIELD, false, StackValue.LOCAL_0),
                    type, kotlinType
                )
                add(getCapturedParamInfo(descriptor))
            }

            val capturedReceiver = closure.capturedReceiverFromOuterContext
            if (capturedReceiver != null) {
                val type = typeMapper.mapType(capturedReceiver).let {
                    if (isBoundCallableReference) AsmUtil.boxType(it) else it
                }

                val fieldName = closure.getCapturedReceiverFieldName(typeMapper.bindingContext, languageVersionSettings)
                val descriptor = EnclosedValueDescriptor(
                    fieldName, null,
                    StackValue.field(type, capturedReceiver, lambdaClassType, fieldName, false, StackValue.LOCAL_0),
                    type, capturedReceiver
                )
                add(getCapturedParamInfo(descriptor))
            }

            closure.captureVariables.values.forEach { descriptor ->
                add(getCapturedParamInfo(descriptor))
            }
        }
    }

    val isPropertyReference: Boolean
        get() = propertyReferenceInfo != null

    override fun isCapturedSuspend(desc: CapturedParamDesc): Boolean =
        isCapturedSuspendLambda(closure, desc.fieldName, typeMapper.bindingContext)
}

class PsiDefaultLambda(
    override val lambdaClassType: Type,
    capturedArgs: Array<Type>,
    private val parameterDescriptor: ValueParameterDescriptor,
    offset: Int,
    needReification: Boolean
) : DefaultLambda(capturedArgs, parameterDescriptor.isCrossinline, offset, needReification) {
    override fun mapAsmSignature(sourceCompiler: SourceCompilerForInline, descriptor: FunctionDescriptor): Method =
        sourceCompiler.state.typeMapper.mapSignatureSkipGeneric(descriptor).asmMethod

    override fun findInvokeMethodDescriptor(isPropertyReference: Boolean): FunctionDescriptor =
        parameterDescriptor.type.memberScope
            .getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND)
            .single().let {
                // property reference generates erased 'get' method
                if (isPropertyReference) it.original else it
            }
}
