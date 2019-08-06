/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.ir.isInlineParameter
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.AsmUtil.BOUND_REFERENCE_RECEIVER
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    function: FunctionDescriptor,
    typeParameterMappings: IrTypeParameterMappings,
    sourceCompiler: SourceCompilerForInline
) : InlineCodegen<ExpressionCodegen>(codegen, state, function, typeParameterMappings.toTypeParameterMappings(), sourceCompiler),
    IrCallGenerator {
    override fun generateAssertFieldIfNeeded(info: RootInliningContext) {
        // TODO: JVM assertions are not implemented yet in IR backend
    }

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        val lambdaInfo = next as IrExpressionLambdaImpl
        activeLambda = lambdaInfo

        lambdaInfo.reference.getArguments().forEachIndexed { index, (_, ir) ->
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
            putValueOnStack(argumentExpression, parameterType, irValueParameter.index, blockInfo, irValueParameter.type)
        }
    }

    override fun putValueIfNeeded(
        parameterType: Type,
        value: StackValue,
        kind: ValueKind,
        parameterIndex: Int,
        codegen: ExpressionCodegen
    ) {
        //TODO: support default argument erasure
        //if (processDefaultMaskOrMethodHandler(value, kind)) return
        putArgumentOrCapturedToLocalVal(JvmKotlinType(value.type, value.kotlinType), value, -1, parameterIndex, ValueKind.CAPTURED /*kind*/)
    }

    private fun putCapturedValueOnStack(argumentExpression: IrExpression, valueType: Type, capturedParamIndex: Int) {
        val onStack = codegen.gen(argumentExpression, valueType, argumentExpression.type, BlockInfo())
        putArgumentOrCapturedToLocalVal(
            JvmKotlinType(onStack.type, onStack.kotlinType), onStack, capturedParamIndex, capturedParamIndex, ValueKind.CAPTURED
        )
    }

    private fun putValueOnStack(argumentExpression: IrExpression, valueType: Type, paramIndex: Int, blockInfo: BlockInfo, irType: IrType) {
        val onStack = codegen.gen(argumentExpression, valueType, irType, blockInfo)
        putArgumentOrCapturedToLocalVal(JvmKotlinType(onStack.type, onStack.kotlinType), onStack, -1, paramIndex, ValueKind.CAPTURED)
    }

    override fun beforeValueParametersStart() {
        invocationParamBuilder.markValueParametersStart()
    }

    override fun genCall(
        callableMethod: IrCallableMethod,
        codegen: ExpressionCodegen,
        expression: IrFunctionAccessExpression
    ) {
        val typeArguments = expression.descriptor.typeParameters.keysToMap { expression.getTypeArgumentOrDefault(it) }
        // TODO port inlining cycle detection to IrFunctionAccessExpression & pass it
        state.globalInlineContext.enterIntoInlining(null)
        try {
            performInline(typeArguments, false, codegen)
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
            irReference, referencedFunction, codegen.typeMapper, codegen.methodSignatureMapper, parameter.isCrossinline,
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
    private val methodSignatureMapper: MethodSignatureMapper,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean,
    override val isExtensionLambda: Boolean
) : ExpressionLambda(isCrossInline), IrExpressionLambda {

    override fun isReturnFromMe(labelName: String): Boolean {
        return false //always false
    }

    companion object {
        private var counter: Int = 123//TODO: pass proper type
    }

    override val lambdaClassType: Type = Type.getObjectType("test${counter++}")

    override val capturedVars: List<CapturedParamDesc> =
        arrayListOf<CapturedParamDesc>().apply {
            reference.getArguments().forEachIndexed { _, (_, ir) ->
                add(
                    when (ir) {
                        is IrGetValue -> capturedParamDesc(ir.descriptor.name.asString(), typeMapper.mapType(ir.type))
                        is IrConst<*> -> capturedParamDesc(BOUND_REFERENCE_RECEIVER, typeMapper.mapType(ir.type))
                        is IrGetField -> capturedParamDesc(ir.descriptor.name.asString(), typeMapper.mapType(ir.type))
                        else -> error("Unrecognized expression: ${ir.dump()}")
                    }
                )
            }
        }

    private val loweredMethod = methodSignatureMapper.mapAsmMethod(function)

    val capturedParamsInDesc: List<Type> =
        loweredMethod.argumentTypes.drop(if (isExtensionLambda) 1 else 0).take(capturedVars.size)

    override val invokeMethod: Method = loweredMethod.let {
        Method(
            it.name,
            it.returnType,
            (
                    (if (isExtensionLambda) it.argumentTypes.take(1) else emptyList()) +
                            it.argumentTypes.drop((if (isExtensionLambda) 1 else 0) + capturedVars.size)
                    ).toTypedArray()
        )
    }

    override val invokeMethodDescriptor: FunctionDescriptor = function.descriptor

    override val hasDispatchReceiver: Boolean = false
}

fun isInlineIrExpression(argumentExpression: IrExpression) =
    when (argumentExpression) {
        is IrBlock -> (argumentExpression.origin == IrStatementOrigin.LAMBDA || argumentExpression.origin == IrStatementOrigin.ANONYMOUS_FUNCTION)
        is IrCallableReference -> true.also {
            assert((0 until argumentExpression.valueArgumentsCount).count { argumentExpression.getValueArgument(it) != null } == 0) {
                "Expecting 0 value arguments for bounded callable reference: ${argumentExpression.dump()}"
            }
        }
        else -> false
    }

fun IrFunction.isInlineFunctionCall(context: JvmBackendContext) =
    (!context.state.isInlineDisabled || typeParameters.any { it.isReified }) && isInline