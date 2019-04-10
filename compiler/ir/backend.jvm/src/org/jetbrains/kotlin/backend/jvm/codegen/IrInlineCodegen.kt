/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.getArguments
import org.jetbrains.kotlin.resolve.inline.InlineUtil.isInlineParameter
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.Method

class IrInlineCodegen(
    codegen: ExpressionCodegen,
    state: GenerationState,
    function: FunctionDescriptor,
    typeParameterMappings: TypeParameterMappings,
    sourceCompiler: SourceCompilerForInline
) : InlineCodegen<ExpressionCodegen>(codegen, state, function, typeParameterMappings, sourceCompiler), IrCallGenerator {
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
        valueParameterDescriptor: ValueParameterDescriptor?,
        argumentExpression: IrExpression,
        parameterType: Type,
        parameterIndex: Int,
        codegen: ExpressionCodegen,
        blockInfo: BlockInfo
    ) {
        if (valueParameterDescriptor?.let { isInlineParameter(it) } == true && isInlineIrExpression(argumentExpression)) {
            val irReference: IrFunctionReference =
                (argumentExpression as IrBlock).statements.filterIsInstance<IrFunctionReference>().single()
            rememberClosure(irReference, parameterType, valueParameterDescriptor) as IrExpressionLambdaImpl
        } else {
            putValueOnStack(argumentExpression, parameterType, valueParameterDescriptor?.index ?: -1)
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
        val onStack = codegen.gen(argumentExpression, valueType, BlockInfo.create())
        putArgumentOrCapturedToLocalVal(
            JvmKotlinType(onStack.type, onStack.kotlinType), onStack, capturedParamIndex, capturedParamIndex, ValueKind.CAPTURED
        )
    }

    private fun putValueOnStack(argumentExpression: IrExpression, valueType: Type, paramIndex: Int) {
        val onStack = codegen.gen(argumentExpression, valueType, BlockInfo.create())
        putArgumentOrCapturedToLocalVal(JvmKotlinType(onStack.type, onStack.kotlinType), onStack, -1, paramIndex, ValueKind.CAPTURED)
    }

    override fun beforeValueParametersStart() {
        invocationParamBuilder.markValueParametersStart()
    }

    override fun genCall(callableMethod: Callable, callDefault: Boolean, codegen: ExpressionCodegen, expression: IrMemberAccessExpression) {
        val typeArguments = expression.descriptor.typeParameters.keysToMap { expression.getTypeArgumentOrDefault(it) }
        performInline(typeArguments, callDefault, codegen)
    }

    private fun rememberClosure(irReference: IrFunctionReference, type: Type, parameter: ValueParameterDescriptor): LambdaInfo {
        //assert(InlineUtil.isInlinableParameterExpression(ktLambda)) { "Couldn't find inline expression in ${expression.text}" }
        val expression = irReference.symbol.owner as IrFunction
        return IrExpressionLambdaImpl(
            irReference, expression, typeMapper, parameter.isCrossinline, false/*TODO*/,
            parameter.type.isExtensionFunctionType
        ).also { lambda ->
            val closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.index)
            closureInfo.lambda = lambda
            expressionMap[closureInfo.index] = lambda
        }
    }
}

class IrExpressionLambdaImpl(
    val reference: IrFunctionReference,
    val function: IrFunction,
    typeMapper: KotlinTypeMapper,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean,
    override val isExtensionLambda: Boolean
) : ExpressionLambda(typeMapper, isCrossInline), IrExpressionLambda {

    override fun isMyLabel(name: String): Boolean {
        //TODO("not implemented")
        return false
    }

    override val lambdaClassType: Type = Type.getObjectType("test123")

    override val capturedVars: List<CapturedParamDesc> =
        arrayListOf<CapturedParamDesc>().apply {
            reference.getArguments().forEachIndexed { _, (_, ir) ->
                val getValue = ir as? IrGetValue ?: error("Unrecognized expression: $ir")
                add(capturedParamDesc(getValue.descriptor.name.asString(), typeMapper.mapType(getValue.descriptor.type)))
            }
        }

    private val loweredMethod = typeMapper.mapAsmMethod(function.descriptor)

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
    argumentExpression is IrBlock && argumentExpression.origin == IrStatementOrigin.LAMBDA
