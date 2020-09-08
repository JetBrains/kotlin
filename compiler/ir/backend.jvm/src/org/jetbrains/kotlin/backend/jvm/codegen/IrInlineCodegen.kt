/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.lower.suspendFunctionOriginal
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.descriptors.toIrBasedDescriptor
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class IrInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    private val function: IrFunction,
    methodOwner: Type,
    signature: JvmMethodSignature,
    typeParameterMappings: TypeParameterMappings<IrType>,
    sourceCompiler: SourceCompilerForInline,
    reifiedTypeInliner: ReifiedTypeInliner<IrType>
) :
    InlineCodegen<ExpressionCodegen>(
        codegen, state, function.toIrBasedDescriptor(), methodOwner, signature, typeParameterMappings, sourceCompiler, reifiedTypeInliner
    ),
    IrCallGenerator {

    override fun generateAssertFieldIfNeeded(info: RootInliningContext) {
        if (info.generateAssertField) {
            // May be inlining code into `<clinit>`, in which case it's too late to modify the IR and
            // `generateAssertFieldIfNeeded` will return a statement for which we need to emit bytecode.
            codegen.classCodegen.generateAssertFieldIfNeeded()?.accept(codegen, BlockInfo())?.discard()
        }
    }

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        activeLambda = next

        when (next) {
            is IrExpressionLambdaImpl -> next.reference.getArgumentsWithIr().forEachIndexed { index, (_, ir) ->
                putCapturedValueOnStack(ir, next.capturedParamsInDesc[index], index)
            }
            is IrDefaultLambda -> rememberCapturedForDefaultLambda(next)
            else -> throw RuntimeException("Unknown lambda: $next")
        }

        activeLambda = null
    }

    override fun genValueAndPut(
        irValueParameter: IrValueParameter,
        argumentExpression: IrExpression,
        parameterType: Type,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo
    ) {
        if (codegen.irFunction.isInvokeSuspendOfContinuation()) {
            // In order to support java interop of inline suspend functions, we generate continuations for these inline suspend functions.
            // These functions should behave as ordinary suspend functions, i.e. we should not inline the content of the inline function
            // into continuation.
            // Thus, we should put its arguments to stack.
            super.genValueAndPut(irValueParameter, argumentExpression, parameterType, codegen, blockInfo)
        }

        val isInlineParameter = irValueParameter.isInlineParameter()
        if (isInlineParameter && isInlineIrExpression(argumentExpression)) {
            val irReference: IrFunctionReference =
                (argumentExpression as IrBlock).statements.filterIsInstance<IrFunctionReference>().single()
            val boundReceiver = argumentExpression.statements.filterIsInstance<IrVariable>().singleOrNull()
            val lambdaInfo =
                rememberClosure(irReference, parameterType, irValueParameter, boundReceiver) as IrExpressionLambdaImpl

            if (boundReceiver != null) {
                activeLambda = lambdaInfo
                putCapturedValueOnStack(boundReceiver.initializer!!, lambdaInfo.capturedParamsInDesc.single(), 0)
                activeLambda = null
            }
        } else {
            val kind = when (irValueParameter.origin) {
                IrDeclarationOrigin.MASK_FOR_DEFAULT_FUNCTION -> ValueKind.DEFAULT_MASK
                IrDeclarationOrigin.METHOD_HANDLER_IN_DEFAULT_FUNCTION -> ValueKind.METHOD_HANDLE_IN_DEFAULT
                else -> when {
                    argumentExpression is IrContainerExpression && argumentExpression.origin == IrStatementOrigin.DEFAULT_VALUE ->
                        ValueKind.DEFAULT_PARAMETER
                    // TODO ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_PARAMETER_CALLED_IN_SUSPEND?
                    isInlineParameter && irValueParameter.type.isSuspendFunctionTypeOrSubtype() ->
                        ValueKind.NON_INLINEABLE_ARGUMENT_FOR_INLINE_SUSPEND_PARAMETER
                    else -> ValueKind.GENERAL
                }
            }

            val onStack = when {
                kind == ValueKind.METHOD_HANDLE_IN_DEFAULT -> StackValue.constant(null, AsmTypes.OBJECT_TYPE)
                kind == ValueKind.DEFAULT_MASK -> StackValue.constant((argumentExpression as IrConst<*>).value, Type.INT_TYPE)
                kind == ValueKind.DEFAULT_PARAMETER -> StackValue.constant(null, AsmTypes.OBJECT_TYPE)
                irValueParameter.index >= 0
                    // Reuse an existing local if possible. NOTE: when stopping at a breakpoint placed
                    // in an inline function, arguments which reuse an existing local will not be visible
                    // in the debugger.
                -> codegen.genOrGetLocal(argumentExpression, blockInfo)
                else
                    // Do not reuse locals for receivers. While it's actually completely fine, the non-IR
                    // backend does not do it for internal reasons, and here we replicate the debugging
                    // experience.
                -> codegen.gen(argumentExpression, parameterType, irValueParameter.type, blockInfo)
            }


            //TODO support default argument erasure
            if (!processDefaultMaskOrMethodHandler(onStack, kind)) {
                val expectedType = JvmKotlinType(parameterType, irValueParameter.type.toIrBasedKotlinType())
                putArgumentOrCapturedToLocalVal(expectedType, onStack, -1, irValueParameter.index, kind)
            }
        }
    }

    private fun putCapturedValueOnStack(argumentExpression: IrExpression, valueType: Type, capturedParamIndex: Int) {
        val onStack = codegen.genOrGetLocal(argumentExpression, BlockInfo())
        val expectedType = JvmKotlinType(valueType, argumentExpression.type.toIrBasedKotlinType())
        putArgumentOrCapturedToLocalVal(expectedType, onStack, capturedParamIndex, capturedParamIndex, ValueKind.CAPTURED)
    }

    override fun beforeValueParametersStart() {
        invocationParamBuilder.markValueParametersStart()
    }

    override fun genCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression
    ) {
        val element = codegen.context.psiSourceManager.findPsiElement(expression, codegen.irFunction)
            ?: codegen.context.psiSourceManager.findPsiElement(codegen.irFunction)
        if (!state.globalInlineContext.enterIntoInlining(
                expression.symbol.owner.suspendFunctionOriginal().toIrBasedDescriptor(), element)
        ) {
            val message = "Call is a part of inline call cycle: ${expression.render()}"
            AsmUtil.genThrow(codegen.v, "java/lang/UnsupportedOperationException", message)
            return
        }
        try {
            performInline(
                expression.symbol.owner.typeParameters.map { it.symbol },
                // Always look for default lambdas to allow custom default argument handling in compiler plugins.
                true,
                false,
                codegen.typeMapper.typeSystem,
                registerLineNumberAfterwards = false,
                isCallOfFunctionInCorrespondingDefaultDispatch = codegen.irFunction == codegen.context.mapping.defaultArgumentsDispatchFunction[function]
            )
        } finally {
            state.globalInlineContext.exitFromInlining()
        }
    }

    private fun rememberClosure(
        irReference: IrFunctionReference,
        type: Type,
        parameter: IrValueParameter,
        boundReceiver: IrVariable?
    ): LambdaInfo {
        val referencedFunction = irReference.symbol.owner
        return IrExpressionLambdaImpl(
            irReference, referencedFunction, codegen.typeMapper, codegen.methodSignatureMapper, codegen.context, parameter.isCrossinline,
            boundReceiver != null, parameter.type.isExtensionFunctionType
        ).also { lambda ->
            val closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.index)
            closureInfo.functionalArgument = lambda
            expressionMap[closureInfo.index] = lambda
        }
    }

    override fun extractDefaultLambdas(node: MethodNode): List<DefaultLambda> {
        if (maskStartIndex == -1) return listOf()
        return expandMaskConditionsAndUpdateVariableNodes(
            node, maskStartIndex, maskValues, methodHandleInDefaultMethodIndex,
            extractDefaultLambdaOffsetAndDescriptor(jvmSignature, function),
            ::IrDefaultLambda
        )
    }
}

class IrExpressionLambdaImpl(
    val reference: IrFunctionReference,
    val function: IrFunction,
    private val typeMapper: IrTypeMapper,
    methodSignatureMapper: MethodSignatureMapper,
    context: JvmBackendContext,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean,
    override val isExtensionLambda: Boolean
) : ExpressionLambda(isCrossInline), IrExpressionLambda {

    override val isSuspend: Boolean = function.isSuspend

    override fun isReturnFromMe(labelName: String): Boolean {
        return false //always false
    }

    // This name doesn't actually matter: it is used internally to tell this lambda's captured
    // arguments apart from any other scope's. So long as it's unique, any value is fine.
    // This particular string slightly aids in debugging internal compiler errors as it at least
    // points towards the function containing the lambda.
    override val lambdaClassType: Type =
        context.getLocalClassType(reference) ?: throw AssertionError("callable reference ${reference.dump()} has no name in context")

    private val capturedParameters: Map<CapturedParamDesc, IrValueParameter> =
        reference.getArgumentsWithIr().associate { (param, _) ->
            capturedParamDesc(param.name.asString(), typeMapper.mapType(param.type)) to param
        }

    override val capturedVars: List<CapturedParamDesc> = capturedParameters.keys.toList()

    private val loweredMethod = methodSignatureMapper.mapAsmMethod(function)

    val capturedParamsInDesc: List<Type> = if (isBoundCallableReference) {
        loweredMethod.argumentTypes.take(1)
    } else loweredMethod.argumentTypes.drop(if (isExtensionLambda) 1 else 0).take(capturedVars.size)

    override val invokeMethod: Method = loweredMethod.let {
        Method(
            it.name,
            it.returnType,
            (if (isBoundCallableReference) it.argumentTypes.drop(1)
            else (if (isExtensionLambda) it.argumentTypes.take(1) else emptyList()) +
                    it.argumentTypes.drop((if (isExtensionLambda) 1 else 0) + capturedVars.size)).toTypedArray()
        )
    }

    // Need the descriptor without captured parameters here.
    override val invokeMethodDescriptor: FunctionDescriptor = function.originalFunction.toIrBasedDescriptor()

    override val hasDispatchReceiver: Boolean = false

    override fun getInlineSuspendLambdaViewDescriptor(): FunctionDescriptor = function.toIrBasedDescriptor()

    override fun isCapturedSuspend(desc: CapturedParamDesc): Boolean =
        capturedParameters[desc]?.let { it.isInlineParameter() && it.type.isSuspendFunctionTypeOrSubtype() } == true
}

class IrDefaultLambda(
    lambdaClassType: Type,
    capturedArgs: Array<Type>,
    private val irValueParameter: IrValueParameter,
    offset: Int,
    needReification: Boolean
) : DefaultLambda(
    lambdaClassType, capturedArgs, irValueParameter.toIrBasedDescriptor() as ValueParameterDescriptor, offset, needReification
) {

    override fun mapAsmSignature(sourceCompiler: SourceCompilerForInline): Method {
        val invoke =
            irValueParameter.type.classOrNull!!.owner.declarations.filterIsInstance<IrFunction>().single { it.name.asString() == "invoke" }
        return (sourceCompiler as IrSourceCompilerForInline).codegen.context.methodSignatureMapper.mapSignatureSkipGeneric(invoke).asmMethod
    }

    override fun findInvokeMethodDescriptor(): FunctionDescriptor =
        (irValueParameter.type.classifierOrFail.owner as IrClass).functions.single {
            it.name == OperatorNameConventions.INVOKE
        }.toIrBasedDescriptor()
}

fun isInlineIrExpression(argumentExpression: IrExpression) =
    when (argumentExpression) {
        is IrBlock -> argumentExpression.isInlineIrBlock()
        is IrCallableReference<*> -> true.also {
            assert((0 until argumentExpression.valueArgumentsCount).count { argumentExpression.getValueArgument(it) != null } == 0) {
                "Expecting 0 value arguments for bounded callable reference: ${argumentExpression.dump()}"
            }
        }
        else -> false
    }

fun IrBlock.isInlineIrBlock(): Boolean = origin.isLambda

fun IrFunction.isInlineFunctionCall(context: JvmBackendContext) =
    (!context.state.isInlineDisabled || typeParameters.any { it.isReified }) && isInline
