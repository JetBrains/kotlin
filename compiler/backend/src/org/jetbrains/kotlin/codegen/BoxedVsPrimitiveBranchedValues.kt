/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

abstract class NumberLikeCompare(
        left: StackValue,
        right: StackValue,
        operandType: Type,
        protected val opToken: IElementType
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

    override fun putSelector(type: Type, v: InstructionAdapter) {
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
        coerceTo(type, v)
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
        if (jumpIfFalse) {
            jumpIfFalse(v, jumpLabel)
        }
        else {
            jumpIfTrue(v, jumpLabel)
        }
    }

    private fun jumpIfTrue(v: InstructionAdapter, jumpLabel: Label) {
        if (arg2!!.canHaveSideEffects()) {
            jumpIfTrueWithPossibleSideEffects(v, jumpLabel)
            return
        }

        val notNullLabel = Label()
        val endLabel = Label()
        arg1.put(boxedType, v)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        v.goTo(endLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        arg2.put(operandType, v)
        v.visitJumpInsn(patchOpcode(negatedOperations[opcode]!!, v), jumpLabel)

        v.mark(endLabel)
    }

    private fun jumpIfTrueWithPossibleSideEffects(v: InstructionAdapter, jumpLabel: Label) {
        val notNullLabel = Label()
        val endLabel = Label()

        arg1.put(boxedType, v)
        arg2!!.put(operandType, v)

        val tempArg2 = frameMap.enterTemp(operandType)

        v.store(tempArg2, operandType)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        v.goTo(endLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        v.load(tempArg2, operandType)
        v.visitJumpInsn(patchOpcode(negatedOperations[opcode]!!, v), jumpLabel)

        frameMap.leaveTemp(operandType)
        v.mark(endLabel)

    }

    private fun jumpIfFalse(v: InstructionAdapter, jumpLabel: Label) {
        if (arg2!!.canHaveSideEffects()) {
            jumpIfFalseWithPossibleSideEffects(v, jumpLabel)
            return
        }

        val notNullLabel = Label()
        arg1.put(boxedType, v)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        v.goTo(jumpLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        arg2.put(operandType, v)
        v.visitJumpInsn(patchOpcode(opcode, v), jumpLabel)
    }

    private fun jumpIfFalseWithPossibleSideEffects(v: InstructionAdapter, jumpLabel: Label) {
        val notNullLabel = Label()

        arg1.put(boxedType, v)
        arg2!!.put(operandType, v)
        val tempArg2 = frameMap.enterTemp(operandType)
        v.store(tempArg2, operandType)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        v.goTo(jumpLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        v.load(tempArg2, operandType)
        v.visitJumpInsn(patchOpcode(opcode, v), jumpLabel)

        frameMap.leaveTemp(operandType)
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


class PrimitiveToBoxedEquality private constructor(
        leftPrimitive: StackValue,
        rightBoxed: StackValue,
        primitiveType: Type
) : NumberLikeCompare(leftPrimitive, rightBoxed, primitiveType, KtTokens.EQEQ) {
    private val primitiveType = leftPrimitive.type
    private val boxedType = rightBoxed.type

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        if (jumpIfFalse) {
            jumpIfFalse(v, jumpLabel)
        }
        else {
            jumpIfTrue(v, jumpLabel)
        }
    }

    private fun jumpIfFalse(v: InstructionAdapter, jumpLabel: Label) {
        val notNullLabel = Label()

        arg1.put(primitiveType, v)
        arg2!!.put(boxedType, v)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        AsmUtil.pop(v, primitiveType)
        v.goTo(jumpLabel)

        v.mark(notNullLabel)
        coerce(boxedType, primitiveType, v)
        v.visitJumpInsn(patchOpcode(opcode, v), jumpLabel)
    }

    private fun jumpIfTrue(v: InstructionAdapter, jumpLabel: Label) {
        val notNullLabel = Label()
        val endLabel = Label()

        arg1.put(primitiveType, v)
        arg2!!.put(boxedType, v)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        AsmUtil.pop(v, primitiveType)
        v.goTo(endLabel)

        v.mark(notNullLabel)
        coerce(boxedType, primitiveType, v)
        v.visitJumpInsn(patchOpcode(negatedOperations[opcode]!!, v), jumpLabel)

        v.mark(endLabel)
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
) : NumberLikeCompare(leftPrimitive, rightObject, primitiveType, KtTokens.EQEQ) {
    private val primitiveType = leftPrimitive.type
    private val objectType = rightObject.type
    private val boxedType = AsmUtil.boxType(primitiveType)

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        if (jumpIfFalse) {
            jumpIfFalse(v, jumpLabel)
        }
        else {
            jumpIfTrue(v, jumpLabel)
        }
    }

    private fun jumpIfFalse(v: InstructionAdapter, jumpLabel: Label) {
        val isBoxedLabel = Label()

        arg1.put(primitiveType, v)
        arg2!!.put(objectType, v)
        AsmUtil.dup(v, objectType)
        v.instanceOf(boxedType)
        v.ifne(isBoxedLabel)

        AsmUtil.pop(v, objectType)
        AsmUtil.pop(v, primitiveType)
        v.goTo(jumpLabel)

        v.mark(isBoxedLabel)
        coerce(objectType, boxedType, v)
        coerce(boxedType, primitiveType, v)
        v.visitJumpInsn(patchOpcode(opcode, v), jumpLabel)
    }

    private fun jumpIfTrue(v: InstructionAdapter, jumpLabel: Label) {
        val isBoxedLabel = Label()
        val endLabel = Label()

        arg1.put(primitiveType, v)
        arg2!!.put(objectType, v)
        AsmUtil.dup(v, objectType)
        v.instanceOf(boxedType)
        v.ifne(isBoxedLabel)

        AsmUtil.pop(v, objectType)
        AsmUtil.pop(v, primitiveType)
        v.goTo(endLabel)

        v.mark(isBoxedLabel)
        coerce(objectType, boxedType, v)
        coerce(boxedType, primitiveType, v)
        v.visitJumpInsn(patchOpcode(negatedOperations[opcode]!!, v), jumpLabel)

        v.mark(endLabel)
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