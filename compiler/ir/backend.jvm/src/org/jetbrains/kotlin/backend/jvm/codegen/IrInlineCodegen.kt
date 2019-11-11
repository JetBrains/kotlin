/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.ir.isLambda
import org.jetbrains.kotlin.codegen.IrExpressionLambda
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.ValueKind
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.isSuspend
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    function: FunctionDescriptor,
    methodOwner: Type,
    signature: JvmMethodSignature,
    typeParameterMappings: TypeParameterMappings<IrType>,
    sourceCompiler: SourceCompilerForInline,
    reifiedTypeInliner: ReifiedTypeInliner<IrType>
) : InlineCodegen<ExpressionCodegen>(
    codegen, state, function, methodOwner, signature, typeParameterMappings, sourceCompiler, reifiedTypeInliner
), IrCallGenerator {
    override fun generateAssertFieldIfNeeded(info: RootInliningContext) {
        if (info.generateAssertField && (sourceCompiler as IrSourceCompilerForInline).isPrimaryCopy) {
            codegen.classCodegen.generateAssertFieldIfNeeded()?.let {
                // Generating <clinit> right now, so no longer can insert the initializer into it.
                // Instead, ask ExpressionCodegen to generate the code for it directly.
                it.accept(codegen, BlockInfo()).discard()
            }
        }
    }

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        val lambdaInfo = next as IrExpressionLambdaImpl
        activeLambda = lambdaInfo

        lambdaInfo.reference.getArgumentsWithIr().forEachIndexed { index, (_, ir) ->
            putCapturedValueOnStack(ir, lambdaInfo.capturedParamsInDesc[index], index)
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
        if (irValueParameter.isInlineParameter() && isInlineIrExpression(argumentExpression)) {
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
            val onStack = if (irValueParameter.index >= 0)
                // Reuse an existing local if possible. NOTE: when stopping at a breakpoint placed
                // in an inline function, arguments which reuse an existing local will not be visible
                // in the debugger.
                codegen.genOrGetLocal(argumentExpression, blockInfo)
            else
                // Do not reuse locals for receivers. While it's actually completely fine, the non-IR
                // backend does not do it for internal reasons, and here we replicate the debugging
                // experience.
                codegen.gen(argumentExpression, parameterType, irValueParameter.type, blockInfo)
            // TODO support default argument erasure: do nothing if the parameter is a default mask, the argument is a constant int,
            //      and processDefaultMaskOrMethodHandler(StackValue.constant(...), ValueKind.DEFAULT_MASK) is true.
            val expectedType = JvmKotlinType(parameterType, irValueParameter.type.toKotlinType())
            putArgumentOrCapturedToLocalVal(expectedType, onStack, -1, irValueParameter.index, ValueKind.CAPTURED)
        }
    }

    private fun putCapturedValueOnStack(argumentExpression: IrExpression, valueType: Type, capturedParamIndex: Int) {
        val onStack = codegen.genOrGetLocal(argumentExpression, BlockInfo())
        val expectedType = JvmKotlinType(valueType, argumentExpression.type.toKotlinType())
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
        // TODO port inlining cycle detection to IrFunctionAccessExpression & pass it
        state.globalInlineContext.enterIntoInlining(null)
        try {
            performInline(expression.symbol.owner.typeParameters.map { it.symbol }, false, codegen.typeMapper.typeSystem, codegen)
        } finally {
            state.globalInlineContext.exitFromInliningOf(null)
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
}

class IrExpressionLambdaImpl(
    val reference: IrFunctionReference,
    val function: IrFunction,
    private val typeMapper: IrTypeMapper,
    methodSignatureMapper: MethodSignatureMapper,
    private val context: JvmBackendContext,
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

    override val capturedVars: List<CapturedParamDesc> =
        reference.getArgumentsWithIr().map { (param, _) ->
            capturedParamDesc(param.name.asString(), typeMapper.mapType(param.type))
        }

    private val loweredMethod = methodSignatureMapper.mapAsmMethod(function.getOrCreateSuspendFunctionViewIfNeeded(context))

    val capturedParamsInDesc: List<Type> =
        loweredMethod.argumentTypes.drop(if (isExtensionLambda) 1 else 0).take(capturedVars.size)

    override val invokeMethod: Method = loweredMethod.let {
        Method(
            it.name,
            it.returnType,
            ((if (isExtensionLambda) it.argumentTypes.take(1) else emptyList()) +
                    it.argumentTypes.drop((if (isExtensionLambda) 1 else 0) + capturedVars.size)).toTypedArray()
        )
    }

    override val invokeMethodDescriptor: FunctionDescriptor =
        // Need the descriptor without captured parameters here.
        (function.descriptor as? WrappedSimpleFunctionDescriptor)?.originalDescriptor ?: function.descriptor

    override val hasDispatchReceiver: Boolean = false

    override fun getInlineSuspendLambdaViewDescriptor(): FunctionDescriptor {
        return function.getOrCreateSuspendFunctionViewIfNeeded(context).descriptor
    }
}

fun isInlineIrExpression(argumentExpression: IrExpression) =
    when (argumentExpression) {
        is IrBlock -> argumentExpression.isInlineIrBlock()
        is IrCallableReference -> true.also {
            assert((0 until argumentExpression.valueArgumentsCount).count { argumentExpression.getValueArgument(it) != null } == 0) {
                "Expecting 0 value arguments for bounded callable reference: ${argumentExpression.dump()}"
            }
        }
        else -> false
    }

fun IrBlock.isInlineIrBlock(): Boolean = origin.isLambda

fun IrFunction.isInlineFunctionCall(context: JvmBackendContext) =
    (!context.state.isInlineDisabled || typeParameters.any { it.isReified }) && isInline