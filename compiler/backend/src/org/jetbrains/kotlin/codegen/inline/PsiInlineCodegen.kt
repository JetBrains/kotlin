/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.builtins.isSuspendFunctionType
import org.jetbrains.kotlin.builtins.isSuspendFunctionTypeOrSubtype
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.getMethodAsmFlags
import org.jetbrains.kotlin.codegen.binding.CalculatedClosure
import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.coroutines.getOrCreateJvmSuspendFunctionView
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCallWithAssert
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.inline.InlineUtil
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlinableParameterExpression
import org.jetbrains.kotlin.resolve.inline.isInlineOnly
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
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
    private val functionDescriptor: FunctionDescriptor,
    signature: JvmMethodSignature,
    typeParameterMappings: TypeParameterMappings<KotlinType>,
    sourceCompiler: SourceCompilerForInline,
    private val methodOwner: Type,
    private val actualDispatchReceiver: Type
) : InlineCodegen<ExpressionCodegen>(
    codegen, state, signature, typeParameterMappings, sourceCompiler,
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
        (sourceCompiler as PsiSourceCompilerForInline).callDefault = callDefault
        assert(hiddenParameters.isEmpty()) { "putHiddenParamsIntoLocals() should be called after processHiddenParameters()" }
        if (!state.globalInlineContext.enterIntoInlining(functionDescriptor, resolvedCall?.call?.callElement)) {
            generateStub(resolvedCall?.call?.callElement?.text ?: "<no source>", codegen)
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
        val contextKind = (sourceCompiler as PsiSourceCompilerForInline).context.contextKind
        if (getMethodAsmFlags(functionDescriptor, contextKind, state) and Opcodes.ACC_STATIC == 0) {
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
        invocationParamBuilder.markValueParametersStart()
    }

    override fun putHiddenParamsIntoLocals() {
        for (i in hiddenParameters.indices.reversed()) {
            val (param, offset) = hiddenParameters[i]
            StackValue.local(offset, param.type).store(StackValue.onStack(param.typeOnStack), codegen.visitor)
        }
        hiddenParameters.clear()
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
            rememberClosure(argumentExpression, parameterType.type, valueParameterDescriptor)
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

    var activeLambda: LambdaInfo? = null
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

    override fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda> =
        extractDefaultLambdas(node, extractDefaultLambdaOffsetAndDescriptor(jvmSignature, functionDescriptor)) { parameter ->
            PsiDefaultLambda(type, capturedArgs, parameter, offset, needReification, sourceCompiler)
        }
}

private val FunctionDescriptor.explicitParameters
    get() = listOfNotNull(extensionReceiverParameter) + valueParameters

class PsiExpressionLambda(
    expression: KtExpression,
    private val state: GenerationState,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean
) : ExpressionLambda(isCrossInline) {
    override val lambdaClassType: Type

    override val invokeMethod: Method

    val invokeMethodDescriptor: FunctionDescriptor

    override val invokeMethodParameters: List<KotlinType?>
        get() {
            val actualInvokeDescriptor = if (isSuspend)
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

    override val isSuspend: Boolean

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
        isSuspend = invokeMethodDescriptor.isSuspend
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

class PsiDefaultLambda(
    lambdaClassType: Type,
    capturedArgs: Array<Type>,
    parameterDescriptor: ValueParameterDescriptor,
    offset: Int,
    needReification: Boolean,
    sourceCompiler: SourceCompilerForInline
) : DefaultLambda(lambdaClassType, capturedArgs, parameterDescriptor.isCrossinline, offset, needReification, sourceCompiler) {
    private val invokeMethodDescriptor: FunctionDescriptor

    override val invokeMethodParameters: List<KotlinType?>
        get() = invokeMethodDescriptor.explicitParameters.map { it.returnType }

    override val invokeMethodReturnType: KotlinType?
        get() = invokeMethodDescriptor.returnType

    init {
        val name = if (isPropertyReference) OperatorNameConventions.GET else OperatorNameConventions.INVOKE
        val descriptor = parameterDescriptor.type.memberScope
            .getContributedFunctions(OperatorNameConventions.INVOKE, NoLookupLocation.FROM_BACKEND)
            .single()
            .let { if (parameterDescriptor.type.isSuspendFunctionType) getOrCreateJvmSuspendFunctionView(it, sourceCompiler.state) else it }
        // This is technically wrong as it always uses `invoke`, but `loadInvoke` will fall back to `get` which is never mangled...
        val asmMethod = sourceCompiler.state.typeMapper.mapAsmMethod(descriptor)
        val invokeIsErased = loadInvoke(sourceCompiler, name.asString(), asmMethod)
        invokeMethodDescriptor = if (invokeIsErased) descriptor.original else descriptor
    }
}
