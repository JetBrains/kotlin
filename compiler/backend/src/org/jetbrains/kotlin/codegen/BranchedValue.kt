/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysTrueIfeq
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class BranchedValue(
        val arg1: StackValue,
        val arg2: StackValue? = null,
        val operandType: Type,
        val opcode: Int
) : StackValue(Type.BOOLEAN_TYPE) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        val branchJumpLabel = Label()
        condJump(branchJumpLabel, v, true)
        val endLabel = Label()
        v.iconst(1)
        v.visitJumpInsn(GOTO, endLabel)
        v.visitLabel(branchJumpLabel)
        v.iconst(0)
        v.visitLabel(endLabel)
        coerceTo(type, v)
    }

    open fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        if (arg1 is CondJump) arg1.condJump(jumpLabel, v, jumpIfFalse) else arg1.put(operandType, v)
        arg2?.put(operandType, v)
        v.visitJumpInsn(patchOpcode(if (jumpIfFalse) opcode else negatedOperations[opcode]!!, v), jumpLabel)
    }

    open fun loopJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        condJump(jumpLabel, v, jumpIfFalse)
    }

    protected open fun patchOpcode(opcode: Int, v: InstructionAdapter): Int = opcode

    companion object {
        val negatedOperations = hashMapOf<Int, Int>()

        val TRUE: BranchedValue = object : BranchedValue(StackValue.none()/*not used*/, null, Type.BOOLEAN_TYPE, IFEQ) {
            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (!jumpIfFalse) {
                    v.goTo(jumpLabel)
                }
            }

            override fun loopJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (!jumpIfFalse) {
                    v.fakeAlwaysTrueIfeq(jumpLabel)
                }
                else {
                    v.fakeAlwaysFalseIfeq(jumpLabel)
                }
            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(1)
                coerceTo(type, v)
            }
        }

        val FALSE: BranchedValue = object : BranchedValue(StackValue.none()/*not used*/, null, Type.BOOLEAN_TYPE, IFEQ) {
            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (jumpIfFalse) {
                    v.goTo(jumpLabel)
                }
            }

            override fun loopJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (jumpIfFalse) {
                    v.fakeAlwaysTrueIfeq(jumpLabel)
                }
                else {
                    v.fakeAlwaysFalseIfeq(jumpLabel)
                }
            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(0)
                coerceTo(type, v)
            }
        }

        init {
            registerOperations(IFNE, IFEQ)
            registerOperations(IFLE, IFGT)
            registerOperations(IFLT, IFGE)
            registerOperations(IFGE, IFLT)
            registerOperations(IFGT, IFLE)
            registerOperations(IF_ACMPNE, IF_ACMPEQ)
            registerOperations(IFNULL, IFNONNULL)
        }

        private fun registerOperations(op: Int, negatedOp: Int) {
            negatedOperations.put(op, negatedOp)
            negatedOperations.put(negatedOp, op)
        }

        fun booleanConstant(value: Boolean): BranchedValue = if (value) TRUE else FALSE

        fun createInvertValue(argument: StackValue): StackValue = Invert(condJump(argument))

        fun condJump(condition: StackValue, label: Label, jumpIfFalse: Boolean, iv: InstructionAdapter) {
            condJump(condition).condJump(label, iv, jumpIfFalse)
        }

        fun loopJump(condition: StackValue, label: Label, jumpIfFalse: Boolean, iv: InstructionAdapter) {
            condJump(condition).loopJump(label, iv, jumpIfFalse)
        }

        fun condJump(condition: StackValue): CondJump =
                CondJump(
                        condition as? BranchedValue ?: BranchedValue(condition, null, Type.BOOLEAN_TYPE, IFEQ),
                        IFEQ
                )

        fun cmp(opToken: IElementType, operandType: Type, left: StackValue, right: StackValue): StackValue =
                if (operandType.sort == Type.OBJECT)
                    ObjectCompare(opToken, operandType, left, right)
                else
                    NumberCompare(opToken, operandType, left, right)

    }
}

class And(
        arg1: StackValue,
        arg2: StackValue
) : BranchedValue(BranchedValue.condJump(arg1), BranchedValue.condJump(arg2), Type.BOOLEAN_TYPE, IFEQ) {

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        val stayLabel = Label()
        (arg1 as CondJump).condJump(if (jumpIfFalse) jumpLabel else stayLabel, v, true)
        (arg2 as CondJump).condJump(jumpLabel, v, jumpIfFalse)
        v.visitLabel(stayLabel)
    }
}

class Or(
        arg1: StackValue,
        arg2: StackValue
) : BranchedValue(BranchedValue.condJump(arg1), BranchedValue.condJump(arg2), Type.BOOLEAN_TYPE, IFEQ) {

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        val stayLabel = Label()
        (arg1 as CondJump).condJump(if (jumpIfFalse) stayLabel else jumpLabel, v, false)
        (arg2 as CondJump).condJump(jumpLabel, v, jumpIfFalse)
        v.visitLabel(stayLabel)
    }
}

class Invert(val condition: BranchedValue) : BranchedValue(condition, null, Type.BOOLEAN_TYPE, IFEQ) {

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        condition.condJump(jumpLabel, v, !jumpIfFalse)
    }
}

class CondJump(val condition: BranchedValue, op: Int) : BranchedValue(condition, null, Type.BOOLEAN_TYPE, op) {

    override fun putSelector(type: Type, v: InstructionAdapter) {
        throw UnsupportedOperationException("Use condJump instead")
    }

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        condition.condJump(jumpLabel, v, jumpIfFalse)
    }

    override fun loopJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        condition.loopJump(jumpLabel, v, jumpIfFalse)
    }
}

class NumberCompare(
        private val opToken: IElementType,
        operandType: Type,
        left: StackValue,
        right: StackValue
) : BranchedValue(left, right, operandType, NumberCompare.getNumberCompareOpcode(opToken)) {

    override fun patchOpcode(opcode: Int, v: InstructionAdapter): Int =
            patchOpcode(opcode, v, opToken, operandType)

    companion object {
        fun getNumberCompareOpcode(opToken: IElementType): Int = when (opToken) {
            KtTokens.EQEQ, KtTokens.EQEQEQ -> IFNE
            KtTokens.EXCLEQ, KtTokens.EXCLEQEQEQ -> IFEQ
            KtTokens.GT -> IFLE
            KtTokens.GTEQ -> IFLT
            KtTokens.LT -> IFGE
            KtTokens.LTEQ -> IFGT
            else -> {
                throw UnsupportedOperationException("Don't know how to generate this condJump: " + opToken)
            }
        }

        fun patchOpcode(opcode: Int, v: InstructionAdapter, opToken: IElementType, operandType: Type): Int {
            assert(opcode in IFEQ..IFLE) {
                "Opcode for comparing must be in range ${IFEQ..IFLE}, but $opcode was found"
            }
            return when (operandType) {
                Type.FLOAT_TYPE, Type.DOUBLE_TYPE -> {
                    if (opToken == KtTokens.GT || opToken == KtTokens.GTEQ)
                        v.cmpl(operandType)
                    else
                        v.cmpg(operandType)
                    opcode
                }
                Type.LONG_TYPE -> {
                    v.lcmp()
                    opcode
                }
                else ->
                    opcode + (IF_ICMPEQ - IFEQ)
            }
        }
    }
}

class ObjectCompare(
        opToken: IElementType,
        operandType: Type,
        left: StackValue,
        right: StackValue
) : BranchedValue(left, right, operandType, ObjectCompare.getObjectCompareOpcode(opToken)) {

    companion object {
        fun getObjectCompareOpcode(opToken: IElementType): Int = when (opToken) {
            KtTokens.EQEQEQ -> IF_ACMPNE
            KtTokens.EXCLEQEQEQ -> IF_ACMPEQ
            else -> throw UnsupportedOperationException("don't know how to generate this condjump")
        }
    }
}

