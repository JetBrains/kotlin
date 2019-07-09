/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class NumberLikeCompare(
    left: StackValue,
    right: StackValue,
    operandType: Type,
    private val opToken: IElementType
) : BranchedValue(left, right, operandType, NumberCompare.getNumberCompareOpcode(opToken)) {
    override fun patchOpcode(opcode: Int, v: InstructionAdapter): Int =
        NumberCompare.patchOpcode(opcode, v, opToken, operandType)
}

abstract class SafeCallFusedWithPrimitiveEqualityBase(
    opToken: IElementType,
    operandType: Type,
    left: StackValue,
    right: StackValue
) : NumberLikeCompare(left, right, operandType, opToken) {
    private val trueIfEqual = opToken == KtTokens.EQEQ || opToken == KtTokens.EQEQEQ

    protected abstract fun cleanupOnNullReceiver(v: InstructionAdapter)

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        val endLabel = Label()

        arg1.put(operandType, v)
        arg2!!.put(operandType, v)
        v.visitJumpInsn(patchOpcode(if (jumpIfFalse) opcode else negatedOperations[opcode]!!, v), jumpLabel)
        v.goTo(endLabel)

        cleanupOnNullReceiver(v)
        if (jumpIfFalse == trueIfEqual) {
            v.goTo(jumpLabel)
        }

        v.mark(endLabel)
    }

    override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
        val falseLabel = Label()
        val endLabel = Label()

        arg1.put(operandType, v)
        arg2!!.put(operandType, v)
        v.visitJumpInsn(patchOpcode(opcode, v), falseLabel)

        if (!trueIfEqual) {
            val trueLabel = Label()
            v.goTo(trueLabel)
            cleanupOnNullReceiver(v)
            v.mark(trueLabel)
        }

        v.iconst(1)
        v.goTo(endLabel)

        if (trueIfEqual) {
            cleanupOnNullReceiver(v)
        }

        v.mark(falseLabel)
        v.iconst(0)

        v.mark(endLabel)
        coerceTo(type, kotlinType, v)
    }
}


class SafeCallToPrimitiveEquality(
    opToken: IElementType,
    operandType: Type,
    left: StackValue,
    right: StackValue,
    private val safeReceiverType: Type,
    private val safeReceiverIsNull: Label
) : SafeCallFusedWithPrimitiveEqualityBase(opToken, operandType, left, right) {
    override fun cleanupOnNullReceiver(v: InstructionAdapter) {
        v.mark(safeReceiverIsNull)
        AsmUtil.pop(v, safeReceiverType)
    }
}


class PrimitiveToSafeCallEquality(
    opToken: IElementType,
    operandType: Type,
    left: StackValue,
    right: StackValue,
    private val safeReceiverType: Type,
    private val safeReceiverIsNull: Label
) : SafeCallFusedWithPrimitiveEqualityBase(opToken, operandType, left, right) {
    override fun cleanupOnNullReceiver(v: InstructionAdapter) {
        v.mark(safeReceiverIsNull)
        AsmUtil.pop(v, safeReceiverType)
        AsmUtil.pop(v, arg1.type)
    }
}


class BoxedToPrimitiveEquality private constructor(
    leftBoxed: StackValue,
    rightPrimitive: StackValue,
    primitiveType: Type,
    private val frameMap: FrameMap
) : NumberLikeCompare(leftBoxed, rightPrimitive, primitiveType, KtTokens.EQEQ) {
    private val boxedType = arg1.type

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        if (arg2!!.canHaveSideEffects()) {
            val tmp = frameMap.enterTemp(operandType)
            doJump(
                v, jumpLabel, jumpIfFalse,
                {
                    arg1.put(boxedType, v)
                    arg2.put(operandType, v)
                    v.store(tmp, operandType)
                },
                { v.load(tmp, operandType) }
            )
            frameMap.leaveTemp(operandType)
        } else {
            doJump(
                v, jumpLabel, jumpIfFalse,
                { arg1.put(boxedType, v) },
                { arg2.put(operandType, v) }
            )
        }
    }

    private inline fun doJump(
        v: InstructionAdapter,
        jumpLabel: Label,
        jumpIfFalse: Boolean,
        putArg1: () -> Unit,
        putArg2: () -> Unit
    ) {
        val notNullLabel = Label()
        val endLabel = Label()

        putArg1()
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        if (jumpIfFalse) v.goTo(jumpLabel) else v.goTo(endLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        putArg2()
        v.visitJumpInsn(patchOpcode(if (jumpIfFalse) opcode else negatedOperations[opcode]!!, v), jumpLabel)

        v.mark(endLabel)
    }

    companion object {
        @JvmStatic
        fun create(
            opToken: IElementType,
            left: StackValue,
            leftType: Type,
            right: StackValue,
            rightType: Type,
            frameMap: FrameMap
        ): BranchedValue =
            if (!isApplicable(opToken, leftType, rightType))
                throw IllegalArgumentException("Not applicable for $opToken, $leftType, $rightType")
            else when (opToken) {
                KtTokens.EQEQ -> BoxedToPrimitiveEquality(left, right, rightType, frameMap)
                KtTokens.EXCLEQ -> Invert(BoxedToPrimitiveEquality(left, right, rightType, frameMap))
                else -> throw AssertionError("Unexpected opToken: $opToken")
            }

        @JvmStatic
        fun isApplicable(opToken: IElementType, leftType: Type, rightType: Type) =
            (opToken == KtTokens.EQEQ || opToken == KtTokens.EXCLEQ) &&
                    AsmUtil.isIntOrLongPrimitive(rightType) &&
                    AsmUtil.isBoxedTypeOf(leftType, rightType)
    }
}

abstract class PrimitiveToSomethingEquality
protected constructor(
    leftPrimitive: StackValue,
    right: StackValue,
    primitiveType: Type
) : NumberLikeCompare(leftPrimitive, right, primitiveType, KtTokens.EQEQ) {
    protected val primitiveType = leftPrimitive.type
    protected val rightType = right.type

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        val notNullLabel = Label()
        val endLabel = Label()

        arg1.put(primitiveType, v)
        arg2!!.put(rightType, v)
        AsmUtil.dup(v, rightType)
        jumpIfCanCompareTopWithPrimitive(v, notNullLabel)

        AsmUtil.pop(v, rightType)
        AsmUtil.pop(v, primitiveType)
        if (jumpIfFalse) v.goTo(jumpLabel) else v.goTo(endLabel)

        v.mark(notNullLabel)
        coerceRightToPrimitive(v)
        v.visitJumpInsn(patchOpcode(if (jumpIfFalse) opcode else negatedOperations[opcode]!!, v), jumpLabel)

        v.mark(endLabel)
    }

    protected abstract fun jumpIfCanCompareTopWithPrimitive(v: InstructionAdapter, label: Label)
    protected abstract fun coerceRightToPrimitive(v: InstructionAdapter)
}


class PrimitiveToBoxedEquality private constructor(
    leftPrimitive: StackValue,
    rightBoxed: StackValue,
    primitiveType: Type
) : PrimitiveToSomethingEquality(leftPrimitive, rightBoxed, primitiveType) {
    private val boxedType = rightBoxed.type

    override fun jumpIfCanCompareTopWithPrimitive(v: InstructionAdapter, label: Label) {
        v.ifnonnull(label)
    }

    override fun coerceRightToPrimitive(v: InstructionAdapter) {
        coerce(boxedType, primitiveType, v)
    }

    companion object {
        @JvmStatic
        fun create(opToken: IElementType, left: StackValue, leftType: Type, right: StackValue, rightType: Type): BranchedValue =
            if (!isApplicable(opToken, leftType, rightType))
                throw IllegalArgumentException("Not applicable for $opToken, $leftType, $rightType")
            else when (opToken) {
                KtTokens.EQEQ -> PrimitiveToBoxedEquality(left, right, leftType)
                KtTokens.EXCLEQ -> Invert(PrimitiveToBoxedEquality(left, right, leftType))
                else -> throw AssertionError("Unexpected opToken: $opToken")
            }

        @JvmStatic
        fun isApplicable(opToken: IElementType, leftType: Type, rightType: Type) =
            (opToken == KtTokens.EQEQ || opToken == KtTokens.EXCLEQ) &&
                    AsmUtil.isIntOrLongPrimitive(leftType) &&
                    AsmUtil.isBoxedTypeOf(rightType, leftType)
    }
}


class PrimitiveToObjectEquality private constructor(
    leftPrimitive: StackValue,
    rightObject: StackValue,
    primitiveType: Type
) : PrimitiveToSomethingEquality(leftPrimitive, rightObject, primitiveType) {
    private val boxedType = AsmUtil.boxType(primitiveType)

    override fun jumpIfCanCompareTopWithPrimitive(v: InstructionAdapter, label: Label) {
        v.instanceOf(boxedType)
        v.ifne(label)
    }

    override fun coerceRightToPrimitive(v: InstructionAdapter) {
        coerce(rightType, boxedType, v)
        coerce(boxedType, primitiveType, v)
    }

    companion object {
        @JvmStatic
        fun create(opToken: IElementType, left: StackValue, leftType: Type, right: StackValue, rightType: Type): BranchedValue =
            if (!isApplicable(opToken, leftType, rightType))
                throw IllegalArgumentException("Not applicable for $opToken, $leftType, $rightType")
            else when (opToken) {
                KtTokens.EQEQ -> PrimitiveToObjectEquality(left, right, leftType)
                KtTokens.EXCLEQ -> Invert(PrimitiveToObjectEquality(left, right, leftType))
                else -> throw AssertionError("Unexpected opToken: $opToken")
            }

        @JvmStatic
        fun isApplicable(opToken: IElementType, leftType: Type, rightType: Type) =
            (opToken == KtTokens.EQEQ || opToken == KtTokens.EXCLEQ) &&
                    AsmUtil.isIntOrLongPrimitive(leftType) &&
                    rightType.sort == Type.OBJECT
    }
}


class Ieee754Equality private constructor(
    private val frameMap: FrameMap,
    left: StackValue,
    right: StackValue,
    operandType: Type
) : NumberLikeCompare(left, right, operandType, KtTokens.EQEQ) {

    init {
        assert(operandType == Type.FLOAT_TYPE || operandType == Type.DOUBLE_TYPE) {
            "Unexpected operandType for IEEE 754 equality (should be F or D): $operandType"
        }
    }

    private val leftType = left.type
    private val rightType = right.type

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        val leftIsBoxed = !AsmUtil.isPrimitive(leftType)
        val rightIsBoxed = !AsmUtil.isPrimitive(rightType)

        frameMap.evaluateOnce(arg1, leftType, v) { left ->
            frameMap.evaluateOnce(arg2!!, rightType, v) { right ->
                val endLabel = Label()
                val bothNonNullLabel = Label()

                when {
                    leftIsBoxed && rightIsBoxed -> {
                        val leftNonNullLabel = Label()
                        left.put(leftType, v)
                        v.ifnonnull(leftNonNullLabel)
                        // left == null
                        right.put(rightType, v)
                        v.ifnonnull(if (jumpIfFalse) jumpLabel else endLabel)
                        // left == null && right == null
                        if (jumpIfFalse) v.goTo(endLabel) else v.goTo(jumpLabel)

                        v.mark(leftNonNullLabel)
                        // left != null
                        right.put(rightType, v)
                        v.ifnull(if (jumpIfFalse) jumpLabel else endLabel)
                    }
                    leftIsBoxed -> {
                        left.put(leftType, v)
                        v.ifnull(if (jumpIfFalse) jumpLabel else endLabel)
                    }
                    rightIsBoxed -> {
                        right.put(rightType, v)
                        v.ifnull(if (jumpIfFalse) jumpLabel else endLabel)
                    }
                }

                v.mark(bothNonNullLabel)
                left.put(operandType, v)
                right.put(operandType, v)
                v.cmpg(operandType)
                if (jumpIfFalse) {
                    v.ifne(jumpLabel)
                } else {
                    v.ifeq(jumpLabel)
                }

                v.mark(endLabel)
            }
        }
    }

    companion object {
        @JvmStatic
        fun create(frameMap: FrameMap, left: StackValue, right: StackValue, comparisonType: Type, opToken: IElementType) =
            Ieee754Equality(frameMap, left, right, comparisonType).let {
                when (opToken) {
                    KtTokens.EQEQ -> it
                    KtTokens.EXCLEQ -> Invert(it)
                    else -> throw AssertionError("Unexpected operator: $opToken")
                }
            }
    }
}