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

abstract class SafeCallFusedWithPrimitiveEqualityBase(
        private val opToken: IElementType,
        operandType: Type,
        left: StackValue,
        right: StackValue
) : BranchedValue(left, right, operandType, NumberCompare.getNumberCompareOpcode(opToken)) {
    private val trueIfEqual = opToken == KtTokens.EQEQ || opToken == KtTokens.EQEQEQ

    protected abstract fun cleanupOnNullReceiver(v: InstructionAdapter)

    override fun patchOpcode(opcode: Int, v: InstructionAdapter): Int =
            NumberCompare.patchOpcode(opcode, v, opToken, operandType)

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
        primitiveType: Type
) : BranchedValue(leftBoxed, rightPrimitive, primitiveType, Opcodes.IFNE) {
    private val boxedType = arg1.type

    override fun patchOpcode(opcode: Int, v: InstructionAdapter): Int =
            NumberCompare.patchOpcode(opcode, v, KtTokens.EQEQ, operandType)

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
        AsmUtil.swap(v, operandType, boxedType)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        AsmUtil.pop(v, operandType)
        v.goTo(endLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        v.visitJumpInsn(patchOpcode(negatedOperations[opcode]!!, v), jumpLabel)

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
        AsmUtil.swap(v, operandType, boxedType)
        AsmUtil.dup(v, boxedType)
        v.ifnonnull(notNullLabel)

        AsmUtil.pop(v, boxedType)
        AsmUtil.pop(v, operandType)
        v.goTo(jumpLabel)

        v.mark(notNullLabel)
        coerce(boxedType, operandType, v)
        v.visitJumpInsn(patchOpcode(opcode, v), jumpLabel)
    }

    companion object {
        @JvmStatic
        fun create(opToken: IElementType, leftBoxed: StackValue, leftType: Type, rightPrimitive: StackValue, primitiveType: Type): BranchedValue =
                if (!isApplicable(opToken, leftType, primitiveType))
                    throw IllegalArgumentException("Not applicable for $opToken, $leftType, $primitiveType")
                else when (opToken) {
                    KtTokens.EQEQ -> BoxedToPrimitiveEquality(leftBoxed, rightPrimitive, primitiveType)
                    KtTokens.EXCLEQ -> Invert(BoxedToPrimitiveEquality(leftBoxed, rightPrimitive, primitiveType))
                    else -> throw AssertionError("Unexpected opToken: $opToken")
                }

        @JvmStatic
        fun isApplicable(opToken: IElementType, leftType: Type, rightType: Type) =
                (opToken == KtTokens.EQEQ ||
                 opToken == KtTokens.EXCLEQ
                ) &&
                (rightType == Type.BOOLEAN_TYPE ||
                 rightType == Type.CHAR_TYPE ||
                 rightType == Type.BYTE_TYPE ||
                 rightType == Type.SHORT_TYPE ||
                 rightType == Type.INT_TYPE ||
                 rightType == Type.LONG_TYPE
                ) &&
                AsmUtil.isBoxedTypeOf(leftType, rightType)
    }
}