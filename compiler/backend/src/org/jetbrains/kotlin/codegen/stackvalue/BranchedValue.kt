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

package org.jetbrains.kotlin.codegen.stackvalue

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
        condJump(branchJumpLabel, v)
        val endLabel = Label()
        v.iconst(1)
        v.visitJumpInsn(GOTO, endLabel)
        v.visitLabel(branchJumpLabel)
        v.iconst(0)
        v.visitLabel(endLabel)
        coerceTo(type, v);
    }

    open fun condJump(jumpIfFalse: Label, v: InstructionAdapter) {
        arg1.put(operandType, v)
        arg2?.put(operandType, v)
        v.visitJumpInsn(patchOpcode(opcode, v), jumpIfFalse);
    }

    protected open fun invert(): BranchedValue {
        return BranchedValue(this, negatedOperations.get(opcode))
    }

    protected open fun patchOpcode(opcode: Int, v: InstructionAdapter): Int {
        return opcode
    }

    companion object {
        val negatedOperations = hashMapOf<Int, Int>()

        val TRUE: BranchedValue = object : BranchedValue(StackValue.Constant(true, Type.BOOLEAN_TYPE), null, Type.BOOLEAN_TYPE, IFEQ) {
            override fun invert(): BranchedValue {
                return FALSE
            }

            override fun condJump(jumpIfFalse: Label, v: InstructionAdapter) {

            }

            override fun putSelector(type: Type, v: InstructionAdapter) {
                v.iconst(1)
                coerceTo(type, v);
            }
        }
        val FALSE: BranchedValue = object : BranchedValue(StackValue.Constant(false, Type.BOOLEAN_TYPE), null, Type.BOOLEAN_TYPE, IFEQ) {
            override fun invert(): BranchedValue {
                return TRUE
            }

            override fun condJump(jumpIfFalse: Label, v: InstructionAdapter) {
                v.goTo(jumpIfFalse)
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
        }

        private fun registerOperations(op: Int, negatedOp: Int) {
            negatedOperations.put(op, negatedOp)
            negatedOperations.put(negatedOp, op)
        }

        fun booleanConstant(value: Boolean): BranchedValue {
            return if (value) TRUE else FALSE
        }

        fun createInvertValue(argument: StackValue): StackValue {
            if (argument.type != Type.BOOLEAN_TYPE) {
                throw UnsupportedOperationException("operand of ! must be boolean")
            }

            if (argument is BranchedValue) {
                return argument.invert()
            }
            return BranchedValue(argument, null, Type.BOOLEAN_TYPE, IFNE)
        }

        fun condJump(condition: StackValue, label: Label, jumpIfFalse: Boolean, iv: InstructionAdapter) {
            var condJump: BranchedValue = if (condition is BranchedValue) {
                condition
            }
            else {
                BranchedValue(condition, null, Type.BOOLEAN_TYPE, IFEQ)
            }
            condJump = if (jumpIfFalse) condJump else condJump.invert()
            condJump.condJump(label, iv)
        }

        public fun cmp(opToken: IElementType, operandType: Type, left: StackValue, right: StackValue): StackValue =
                if (operandType.getSort() == Type.OBJECT)
                    ObjectCompare(opToken, ObjectCompare.getObjectCompareOpcode(opToken), operandType, left, right)
                else
                    NumberCompare(opToken, NumberCompare.getNumberCompareOpcode(opToken), operandType, left, right)

    }
}


class NumberCompare(val opToken: IElementType, opcode: Int, operandType: Type, left: StackValue, right: StackValue) :
        BranchedValue(left, right, operandType, opcode) {

    override fun invert(): BranchedValue {
        return NumberCompare(opToken, BranchedValue.negatedOperations[opcode], operandType, arg1, arg2!!)
    }

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

class ObjectCompare(val opToken: IElementType, opcode: Int, operandType: Type, left: StackValue, right: StackValue) :
        BranchedValue(left, right, operandType, opcode) {

    override fun invert(): BranchedValue {
        return ObjectCompare(opToken, BranchedValue.negatedOperations[opcode], operandType, arg1, arg2!!)
    }

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