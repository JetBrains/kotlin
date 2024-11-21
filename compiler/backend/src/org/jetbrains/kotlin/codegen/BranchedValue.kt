/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysFalseIfeq
import org.jetbrains.kotlin.codegen.pseudoInsns.fakeAlwaysTrueIfeq
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.types.KotlinType
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

    override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
        val branchJumpLabel = Label()
        condJump(branchJumpLabel, v, true)
        val endLabel = Label()
        v.iconst(1)
        v.visitJumpInsn(GOTO, endLabel)
        v.visitLabel(branchJumpLabel)
        v.iconst(0)
        v.visitLabel(endLabel)
        coerceTo(type, kotlinType, v)
    }

    open fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
        arg1.put(operandType, v)
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
                v.nop()
                if (!jumpIfFalse) {
                    v.goTo(jumpLabel)
                }
            }

            override fun loopJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                v.nop()
                if (!jumpIfFalse) {
                    v.fakeAlwaysTrueIfeq(jumpLabel)
                } else {
                    v.fakeAlwaysFalseIfeq(jumpLabel)
                }
            }

            override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                v.iconst(1)
                coerceTo(type, kotlinType, v)
            }
        }

        val FALSE: BranchedValue = object : BranchedValue(StackValue.none()/*not used*/, null, Type.BOOLEAN_TYPE, IFEQ) {
            override fun condJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                v.nop()
                if (jumpIfFalse) {
                    v.goTo(jumpLabel)
                }
            }

            override fun loopJump(jumpLabel: Label, v: InstructionAdapter, jumpIfFalse: Boolean) {
                v.nop()
                if (jumpIfFalse) {
                    v.fakeAlwaysTrueIfeq(jumpLabel)
                } else {
                    v.fakeAlwaysFalseIfeq(jumpLabel)
                }
            }

            override fun putSelector(type: Type, kotlinType: KotlinType?, v: InstructionAdapter) {
                v.iconst(0)
                coerceTo(type, kotlinType, v)
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

        fun cmp(opToken: IElementType, operandType: Type, left: StackValue, right: StackValue): BranchedValue =
            if (operandType.sort == Type.OBJECT)
                ObjectCompare(opToken, operandType, left, right)
            else
                NumberCompare(opToken, operandType, left, right)

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
            // "==" and "!=" are here because enum values are compared using reference equality.
            KtTokens.EQEQEQ, KtTokens.EQEQ -> IF_ACMPNE
            KtTokens.EXCLEQEQEQ, KtTokens.EXCLEQ -> IF_ACMPEQ
            else -> throw UnsupportedOperationException("don't know how to generate this condjump")
        }
    }
}

