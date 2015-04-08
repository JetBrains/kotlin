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
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class BranchedValue(val arg1: StackValue, val arg2: StackValue? = null, val operandType: Type, val opcode: Int) : StackValue(Type.BOOLEAN_TYPE) {

    constructor(or: BranchedValue, opcode: Int) : this(or.arg1, or.arg2, or.operandType, opcode) {
    }

    override fun putSelector(type: Type, v: InstructionAdapter) {
        val branchJumpLabel = Label()
        condJump(branchJumpLabel, v, true)
        val endLabel = Label()
        v.iconst(1)
        v.visitJumpInsn(GOTO, endLabel)
        v.visitLabel(branchJumpLabel)
        v.iconst(0)
        v.visitLabel(endLabel)
        coerceTo(type, v);
    }

    open fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        if (arg1 is CondJump) arg1.condJump(jumpLabel, v, jumpIfFalse) else arg1.put(operandType, v)
        arg2?.put(operandType, v)
        v.visitJumpInsn(patchOpcode(if (jumpIfFalse) opcode else negatedOperations[opcode], v), jumpLabel);
    }

    protected open fun patchOpcode(opcode: Int, v: InstructionAdapter): Int {
        return opcode
    }

    companion object {
        val negatedOperations = hashMapOf<Int, Int>()

        val TRUE: BranchedValue = object : BranchedValue(StackValue.Constant(true, Type.BOOLEAN_TYPE), null, Type.BOOLEAN_TYPE, IFEQ) {

            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (!jumpIfFalse) {
                    v.goTo(jumpLabel)
                }
            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(1)
                coerceTo(type, v);
            }
        }

        val FALSE: BranchedValue = object : BranchedValue(StackValue.Constant(false, Type.BOOLEAN_TYPE), null, Type.BOOLEAN_TYPE, IFEQ) {
            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                if (jumpIfFalse) {
                    v.goTo(jumpLabel)
                }
            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(0)
                coerceTo(type, v);
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

        fun booleanConstant(value: Boolean): BranchedValue {
            return if (value) TRUE else FALSE
        }

        fun createInvertValue(argument: StackValue): StackValue {
            return Invert(condJump(argument))
        }

        fun condJump(condition: StackValue, label: Label, jumpIfFalse: Boolean, iv: InstructionAdapter) {
            condJump(condition).condJump(label, iv, jumpIfFalse)
        }

        fun condJump(condition: StackValue): CondJump {
            return CondJump(if (condition is BranchedValue) {
                condition
            }
            else {
                BranchedValue(condition, null, Type.BOOLEAN_TYPE, IFEQ)
            }, IFEQ)
        }

        public fun cmp(opToken: IElementType, operandType: Type, left: StackValue, right: StackValue): StackValue =
                if (operandType.getSort() == Type.OBJECT)
                    ObjectCompare(opToken, operandType, left, right)
                else
                    NumberCompare(opToken, operandType, left, right)

    }
}

class And(arg1: StackValue, arg2: StackValue) :
        BranchedValue(BranchedValue.condJump(arg1), BranchedValue.condJump(arg2), Type.BOOLEAN_TYPE, IFEQ) {

    override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        val stayLabel = Label()
        (arg1 as CondJump).condJump(if (jumpIfFalse) jumpLabel else stayLabel, v, true)
        (arg2 as CondJump).condJump(jumpLabel, v, jumpIfFalse)
        v.visitLabel(stayLabel)
    }
}

class Or(arg1: StackValue, arg2: StackValue) :
        BranchedValue(BranchedValue.condJump(arg1), BranchedValue.condJump(arg2), Type.BOOLEAN_TYPE, IFEQ) {

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
}

class NumberCompare(val opToken: IElementType, operandType: Type, left: StackValue, right: StackValue) :
        BranchedValue(left, right, operandType, NumberCompare.getNumberCompareOpcode(opToken)) {

    override fun patchOpcode(opcode: Int, v: InstructionAdapter): Int {
        when (operandType) {
            Type.FLOAT_TYPE, Type.DOUBLE_TYPE -> {
                if (opToken == JetTokens.GT || opToken == JetTokens.GTEQ) {
                    v.cmpl(operandType)
                }
                else {
                    v.cmpg(operandType)
                }
            }
            Type.LONG_TYPE -> {
                v.lcmp()
            }
            else -> {
                return opcode + (IF_ICMPEQ - IFEQ)
            }
        }
        return opcode
    }

    companion object {
        fun getNumberCompareOpcode(opToken: IElementType): Int {
            return when (opToken) {
                JetTokens.EQEQ, JetTokens.EQEQEQ -> IFNE
                JetTokens.EXCLEQ, JetTokens.EXCLEQEQEQ -> IFEQ
                JetTokens.GT -> IFLE
                JetTokens.GTEQ -> IFLT
                JetTokens.LT -> IFGE
                JetTokens.LTEQ -> IFGT
                else -> {
                    throw UnsupportedOperationException("Don't know how to generate this condJump: " + opToken)
                }
            }
        }
    }
}

class ObjectCompare(val opToken: IElementType, operandType: Type, left: StackValue, right: StackValue) :
        BranchedValue(left, right, operandType, ObjectCompare.getObjectCompareOpcode(opToken)) {

    companion object {
        fun getObjectCompareOpcode(opToken: IElementType): Int {
            return when (opToken) {
                JetTokens.EQEQEQ -> IF_ACMPNE
                JetTokens.EXCLEQEQEQ -> IF_ACMPEQ
                else -> throw UnsupportedOperationException("don't know how to generate this condjump")
            }
        }
    }
}