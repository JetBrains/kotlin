/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.getMethodAsmFlags
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtPsiUtil
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinableParameterExpression
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
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
    ReifiedTypeInliner(typeParameterMappings, object : ReifiedTypeInliner.IntrinsicsSupport<KotlinType> {
        override fun putClassInstance(v: InstructionAdapter, type: KotlinType) {
            AsmUtil.putJavaLangClassInstance(v, state.typeMapper.mapType(type), type, state.typeMapper)
        }

        override fun toKotlinType(type: KotlinType): KotlinType = type
    }, codegen.typeSystem, state.languageVersionSettings)
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
        val inlineCall = InlineCallImpl.of(resolvedCall)
        if (!state.globalInlineContext.enterIntoInlining(inlineCall)) {
            generateStub(resolvedCall, codegen)
            return
        }
        try {
            val registerLineNumber = registerLineNumberAfterwards(resolvedCall)
            performInline(resolvedCall?.typeArguments?.keys?.toList(), callDefault, callDefault, codegen.typeSystem, registerLineNumber)
        } finally {
            state.globalInlineContext.exitFromInliningOf(inlineCall)
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
                        receiver.type.boxReceiverForBoundReference(receiverKotlinType, state.typeMapper)
                    else
                        receiver.type.boxReceiverForBoundReference()

                putClosureParametersOnStack(
                    lambdaInfo,
                    StackValue.coercion(receiver, boxedReceiver, receiverKotlinType)
                )
            }
        } else {
            val value = codegen.gen(argumentExpression)
            val kind = when {
                isCallSiteIsSuspend(valueParameterDescriptor) -> ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND
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
}
