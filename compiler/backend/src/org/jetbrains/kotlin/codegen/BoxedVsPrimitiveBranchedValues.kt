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

