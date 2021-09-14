/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.intrinsics

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.ir.isSmartcastFromHigherThanNullable
import org.jetbrains.kotlin.backend.jvm.ir.receiverAndArgs
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.AsmUtil.isPrimitive
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.genAreEqualCall
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.ir.descriptors.toIrBasedKotlinType
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.isNullable
import org.jetbrains.kotlin.ir.util.isEnumClass
import org.jetbrains.kotlin.ir.util.isEnumEntry
import org.jetbrains.kotlin.ir.util.isIntegerConst
import org.jetbrains.kotlin.ir.util.isNullConst
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.types.isNullable
import org.jetbrains.kotlin.types.typeUtil.isPrimitiveNumberOrNullableType
import org.jetbrains.kotlin.types.typeUtil.upperBoundedByPrimitiveNumberOrNullableType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class ExplicitEquals : IntrinsicMethod() {
    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val (a, b) = expression.receiverAndArgs()

        // TODO use specialized boxed type - this might require types like 'java.lang.Integer' in IR
        a.accept(codegen, data).materializeAt(AsmTypes.OBJECT_TYPE, codegen.context.irBuiltIns.anyNType)
        b.accept(codegen, data).materializeAt(AsmTypes.OBJECT_TYPE, codegen.context.irBuiltIns.anyNType)
        codegen.mv.visitMethodInsn(
            Opcodes.INVOKEVIRTUAL,
            AsmTypes.OBJECT_TYPE.internalName,
            "equals",
            Type.getMethodDescriptor(Type.BOOLEAN_TYPE, AsmTypes.OBJECT_TYPE),
            false
        )

        return MaterialValue(codegen, Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType)
    }
}

class Equals(val operator: IElementType) : IntrinsicMethod() {

    private class BooleanNullCheck(val value: PromisedValue) : BooleanValue(value.codegen) {
        override fun jumpIfFalse(target: Label) = value.materialize().also { mv.ifnonnull(target) }
        override fun jumpIfTrue(target: Label) = value.materialize().also { mv.ifnull(target) }
        override fun discard() {
            value.discard()
        }
    }

    override fun invoke(expression: IrFunctionAccessExpression, codegen: ExpressionCodegen, data: BlockInfo): PromisedValue? {
        val (a, b) = expression.receiverAndArgs()
        if (a.isNullConst() || b.isNullConst()) {
            val irValue = if (a.isNullConst()) b else a
            val value = irValue.accept(codegen, data)
            return if (!isPrimitive(value.type) && (irValue.type.classOrNull?.owner?.isInline != true || irValue.type.isNullable()))
                BooleanNullCheck(value)
            else {
                value.discard()
                BooleanConstant(codegen, false)
            }
        }

        val leftType = with(codegen) { a.asmType }
        val rightType = with(codegen) { b.asmType }
        val opToken = expression.origin

        // Avoid boxing for `primitive == object` and `boxed primitive == primitive` where we know
        // what comparison means. The optimization does not apply to `object == primitive` as equals
        // could be overridden for the object.
        if ((opToken == IrStatementOrigin.EQEQ || opToken == IrStatementOrigin.EXCLEQ) &&
            ((AsmUtil.isIntOrLongPrimitive(leftType) && !isPrimitive(rightType)) ||
                    (AsmUtil.isIntOrLongPrimitive(rightType) && AsmUtil.isBoxedPrimitiveType(leftType)))
        ) {
            val aValue = a.accept(codegen, data).materializedAt(leftType, a.type)
            val bValue = b.accept(codegen, data).materializedAt(rightType, b.type)
            return PrimitiveToObjectComparison(operator, AsmUtil.isIntOrLongPrimitive(leftType), aValue, bValue)
        }

        val aIsEnum = a.type.classOrNull?.owner?.run { isEnumClass || isEnumEntry } == true
        val bIsEnum = b.type.classOrNull?.owner?.run { isEnumClass || isEnumEntry } == true
        val useEquals = opToken !== IrStatementOrigin.EQEQEQ && opToken !== IrStatementOrigin.EXCLEQEQ &&
                // `==` is `equals` for objects and floating-point numbers. In the latter case, the difference
                // is that `equals` is a total order (-0 < +0 and NaN == NaN) and `===` is IEEE754-compliant.
                (!isPrimitive(leftType) || leftType != rightType || leftType == Type.FLOAT_TYPE || leftType == Type.DOUBLE_TYPE)
                // Reference equality can be used for enums.
                && !aIsEnum && !bIsEnum
        return if (useEquals) {
            a.accept(codegen, data).materializeAt(AsmTypes.OBJECT_TYPE, codegen.context.irBuiltIns.anyNType)
            b.accept(codegen, data).materializeAt(AsmTypes.OBJECT_TYPE, codegen.context.irBuiltIns.anyNType)
            genAreEqualCall(codegen.mv)
            MaterialValue(codegen, Type.BOOLEAN_TYPE, codegen.context.irBuiltIns.booleanType)
        } else {
            val operandType = if (!isPrimitive(leftType)) AsmTypes.OBJECT_TYPE else leftType
            if (operandType == Type.INT_TYPE && (a.isIntegerConst(0) || b.isIntegerConst(0))) {
                val nonZero = if (a.isIntegerConst(0)) b else a
                IntegerZeroComparison(operator, nonZero.accept(codegen, data).materializedAt(operandType, nonZero.type))
            } else {
                val aValue = a.accept(codegen, data).materializedAt(operandType, a.type)
                val bValue = b.accept(codegen, data).materializedAt(operandType, b.type)
                BooleanComparison(operator, aValue, bValue)
            }
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

        val arg0Type = arg0.type.toIrBasedKotlinType()
        if (!arg0Type.isPrimitiveNumberOrNullableType() && !arg0Type.upperBoundedByPrimitiveNumberOrNullableType())
            throw AssertionError("Should be primitive or nullable primitive type: $arg0Type")

        val arg1Type = arg1.type.toIrBasedKotlinType()
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
