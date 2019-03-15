/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.AsmUtil.*
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberOrNullableType
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class Equals(val operator: IElementType) : IntrinsicMethod(), ComparisonIntrinsic {

    private fun argumentTypes(
        expression: IrMemberAccessExpression,
        context: JvmBackendContext
    ): Pair<Type, Type> {
        val receiverAndArgs = expression.receiverAndArgs().apply {
            assert(size == 2) { "Equals expects 2 arguments, but ${joinToString()}" }
        }

        var leftType = context.state.typeMapper.mapType(receiverAndArgs.first().type.toKotlinType())
        var rightType = context.state.typeMapper.mapType(receiverAndArgs.last().type.toKotlinType())

        if (isPrimitive(leftType) != isPrimitive(rightType)) {
            leftType = boxType(leftType)
            rightType = boxType(rightType)
        }
        return Pair(leftType, rightType)
    }

    override fun genStackValue(
        expression: IrMemberAccessExpression,
        context: JvmBackendContext
    ): StackValue {
        val (leftType, rightType) = argumentTypes(expression, context)
        val opToken = expression.origin
        return if (opToken === IrStatementOrigin.EQEQEQ || opToken === IrStatementOrigin.EXCLEQEQ) {
            // TODO: always casting to the type of the left operand in case of primitives looks wrong
            val operandType = if (isPrimitive(leftType)) leftType else OBJECT_TYPE
            StackValue.cmp(operator, operandType, StackValue.onStack(leftType), StackValue.onStack(rightType))
        } else {
            genEqualsForExpressionsOnStack(operator, StackValue.onStack(leftType), StackValue.onStack(rightType))
        }
    }

    override fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        var (leftType, rightType) = argumentTypes(expression, context)

        return object : IrIntrinsicFunction(expression, signature, context, listOf(leftType, rightType)) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                val value = genStackValue(expression, context)
                value.put(Type.BOOLEAN_TYPE, v)
            }
        }
    }
}


class Ieee754Equals(val operandType: Type) : IntrinsicMethod() {

    private val boxedOperandType = AsmUtil.boxType(operandType)

    override fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction {
        class Ieee754AreEqual(
            val left: Type,
            val right: Type
        ) : IrIntrinsicFunction(expression, signature, context, listOf(left, right)) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                v.invokestatic(
                    IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual",
                    Type.getMethodDescriptor(Type.BOOLEAN_TYPE, left, right),
                    false
                )
            }
        }

        val arg0Type = expression.getValueArgument(0)!!.type.toKotlinType()
        if (!arg0Type.isPrimitiveNumberOrNullableType()) throw AssertionError("Should be primitive or nullable primitive type: $arg0Type")

        val arg1Type = expression.getValueArgument(1)!!.type.toKotlinType()
        if (!arg1Type.isPrimitiveNumberOrNullableType()) throw AssertionError("Should be primitive or nullable primitive type: $arg1Type")

        val arg0isNullable = arg0Type.isMarkedNullable
        val arg1isNullable = arg1Type.isMarkedNullable

        return when {
            !arg0isNullable && !arg1isNullable ->
                object : IrIntrinsicFunction(expression, signature, context, listOf(operandType, operandType)) {
                    override fun genInvokeInstruction(v: InstructionAdapter) {
                        StackValue.cmp(KtTokens.EQEQ, operandType, StackValue.onStack(operandType), StackValue.onStack(operandType))
                            .put(Type.BOOLEAN_TYPE, v)
                    }
                }

            arg0isNullable && !arg1isNullable ->
                Ieee754AreEqual(boxedOperandType, operandType)

            !arg0isNullable && arg1isNullable ->
                Ieee754AreEqual(operandType, boxedOperandType)

            else ->
                Ieee754AreEqual(boxedOperandType, boxedOperandType)
        }
    }

}

class TotalOrderEquals(operandType: Type) : IntrinsicMethod() {
    private val boxedType = AsmUtil.boxType(operandType)

    override fun toCallable(
        expression: IrMemberAccessExpression,
        signature: JvmMethodSignature,
        context: JvmBackendContext
    ): IrIntrinsicFunction =
        object : IrIntrinsicFunction(expression, signature, context, listOf(boxedType, boxedType)) {
            override fun genInvokeInstruction(v: InstructionAdapter) {
                v.invokevirtual(boxedType.internalName, "equals", Type.getMethodDescriptor(Type.BOOLEAN_TYPE, AsmTypes.OBJECT_TYPE), false)
            }
        }
}
