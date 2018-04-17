/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.ValueKind
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

    override fun putClosureParametersOnStack(next: LambdaInfo, functionReferenceReceiver: StackValue?) {
        val lambdaInfo = next as IrExpressionLambda
        activeLambda = lambdaInfo

        val argumentTypes = lambdaInfo.loweredMethod.argumentTypes
        lambdaInfo.reference.getArguments().forEachIndexed { index, (_, ir) ->
            putCapturedValueOnStack(ir, argumentTypes[index], index)
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
            rememberClosure(irReference, parameterType, valueParameterDescriptor) as IrExpressionLambda
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
        putArgumentOrCapturedToLocalVal(JvmKotlinType(value.type, value.kotlinType), value, -1, parameterIndex, ValueKind.CAPTURED)
    }

    private fun putCapturedValueOnStack(argumentExpression: IrExpression, valueType: Type, capturedParamindex: Int) {
        val onStack = codegen.gen(argumentExpression, valueType, BlockInfo.create())
        putArgumentOrCapturedToLocalVal(
            JvmKotlinType(onStack.type, onStack.kotlinType), onStack, capturedParamindex, capturedParamindex, ValueKind.CAPTURED
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
        return IrExpressionLambda(
            irReference, expression, typeMapper, parameter.isCrossinline, false/*TODO*/
        ).also { lambda ->
            val closureInfo = invocationParamBuilder.addNextValueParameter(type, true, null, parameter.index)
            closureInfo.lambda = lambda
            expressionMap[closureInfo.index] = lambda
        }
    }
}

class IrExpressionLambda(
    val reference: IrFunctionReference,
    val function: IrFunction,
    typeMapper: KotlinTypeMapper,
    isCrossInline: Boolean,
    override val isBoundCallableReference: Boolean
) : ExpressionLambda(typeMapper, isCrossInline) {

    override fun isMyLabel(name: String): Boolean {
        //TODO("not implemented")
        return false
    }

    override val lambdaClassType: Type
        get() = Type.getObjectType("test123")

    override val capturedVars: List<CapturedParamDesc> by lazy {
        arrayListOf<CapturedParamDesc>().apply {
            reference.getArguments().forEachIndexed { _, (_, ir) ->
                val getValue = ir as? IrGetValue ?: error("Unrecognized expression: $ir")
                add(capturedParamDesc(getValue.descriptor.name.asString(), typeMapper.mapType(getValue.descriptor.type)))
            }
        }
    }

    val loweredMethod: Method
        get() = typeMapper.mapAsmMethod(function.descriptor)

    override val invokeMethod: Method = loweredMethod.let {
        Method(it.name, it.returnType, it.argumentTypes.drop(capturedVars.size).toTypedArray())
    }

    override val invokeMethodDescriptor: FunctionDescriptor
        get() = function.descriptor

    override val hasDispatchReceiver: Boolean
        get() = false
}

fun isInlineIrExpression(argumentExpression: IrExpression) =
    argumentExpression is IrBlock && argumentExpression.origin == IrStatementOrigin.LAMBDA