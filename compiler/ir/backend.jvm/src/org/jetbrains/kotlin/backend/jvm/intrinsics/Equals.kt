/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.isSmartcastFromHigherThanNullable
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.AsmUtil.genAreEqualCall
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysTrueIfeq
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberOrNullableType
import org.jetbrains.kotlin.types.typeUtil.upperBoundedByPrimitiveNumberOrNullableType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class Equals(val operator: IElementType) : IntrinsicMethod() {
    private class BooleanConstantFalseCheck(val value: PromisedValue) : BooleanValue(value.codegen) {
        override fun materialize() = value.discard().let { mv.iconst(0) }
        override fun jumpIfFalse(target: Label) = value.discard().let { mv.fakeAlwaysTrueIfeq(target) }
        override fun jumpIfTrue(target: Label) = value.discard().let { mv.fakeAlwaysFalseIfeq(target) }
    }

    private class BooleanNullCheck(val value: PromisedValue) : BooleanValue(value.codegen) {
        override fun jumpIfFalse(target: Label) = value.materialize().also { mv.ifnonnull(target) }
        override fun jumpIfTrue(target: Label) = value.materialize().also { mv.ifnull(target) }
    }

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val (a, b) = expression.receiverAndArgs()
        if (a.isNullConst() || b.isNullConst()) {
            val value = if (a.isNullConst()) b.accept(codegen, data) else a.accept(codegen, data)
            return if (a.type.isNullable() && b.type.isNullable())
                BooleanNullCheck(value)
            else
                BooleanConstantFalseCheck(value)
        }

        val leftType = with(codegen) { a.asmType }
        val rightType = with(codegen) { b.asmType }
        val opToken = expression.origin
        val useEquals = opToken !== IrStatementOrigin.EQEQEQ && opToken !== IrStatementOrigin.EXCLEQEQ &&
                // `==` is `equals` for objects and floating-point numbers. In the latter case, the difference
                // is that `equals` is a total order (-0 < +0 and NaN == NaN) and `===` is IEEE754-compliant.
                (!isPrimitive(leftType) || leftType != rightType || leftType == Type.FLOAT_TYPE || leftType == Type.DOUBLE_TYPE)
        return if (useEquals) {
            a.accept(codegen, data).coerce(AsmTypes.OBJECT_TYPE, codegen.context.irBuiltIns.anyNType).materialize()
            b.accept(codegen, data).coerce(AsmTypes.OBJECT_TYPE, codegen.context.irBuiltIns.anyNType).materialize()
            genAreEqualCall(codegen.mv)
            MaterialValue(codegen, Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType)
        } else {
            val operandType = if (!isPrimitive(leftType)) AsmTypes.OBJECT_TYPE else leftType
            val aValue = a.accept(codegen, data).coerce(operandType, a.type).materialized
            val bValue = b.accept(codegen, data).coerce(operandType, b.type).materialized
            BooleanComparison(operator, aValue, bValue)
        }
    }
}


class Ieee754Equals(val operandType: Type) : IntrinsicMethod() {
    private val boxedOperandType = AsmUtil.boxType(operandType)

    override fun toCallable(
        expression: IrFunctionAccessExpression,
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

        val arg0 = expression.getValueArgument(0)!!
        val arg1 = expression.getValueArgument(1)!!

        val arg0Type = arg0.type.toKotlinType()
        if (!arg0Type.isPrimitiveNumberOrNullableType() && !arg0Type.upperBoundedByPrimitiveNumberOrNullableType())
            throw AssertionError("Should be primitive or nullable primitive type: $arg0Type")

        val arg1Type = arg1.type.toKotlinType()
        if (!arg1Type.isPrimitiveNumberOrNullableType() && !arg1Type.upperBoundedByPrimitiveNumberOrNullableType())
            throw AssertionError("Should be primitive or nullable primitive type: $arg1Type")

        val arg0isNullable = arg0Type.isNullable()
        val arg1isNullable = arg1Type.isNullable()

        val useNonIEEE754Comparison =
            !context.state.languageVersionSettings.supportsFeature(LanguageFeature.ProperIeee754Comparisons)
                    && (arg0.isSmartcastFromHigherThanNullable(context) || arg1.isSmartcastFromHigherThanNullable(context))

        return when {
            useNonIEEE754Comparison ->
                Ieee754AreEqual(AsmTypes.OBJECT_TYPE, AsmTypes.OBJECT_TYPE)

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

