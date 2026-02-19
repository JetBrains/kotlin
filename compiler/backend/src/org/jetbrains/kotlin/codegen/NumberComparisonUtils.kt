/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

object NumberComparisonUtils {
    val negatedOperations: Map<Int, Int> = hashMapOf<Int, Int>().apply {
        registerOperations(IFNE, IFEQ)
        registerOperations(IFLE, IFGT)
        registerOperations(IFLT, IFGE)
        registerOperations(IFGE, IFLT)
        registerOperations(IFGT, IFLE)
        registerOperations(IF_ACMPNE, IF_ACMPEQ)
        registerOperations(IFNULL, IFNONNULL)
    }

    private fun MutableMap<Int, Int>.registerOperations(op: Int, negatedOp: Int) {
        put(op, negatedOp)
        put(negatedOp, op)
    }

    fun getNumberCompareOpcode(opToken: IElementType): Int = when (opToken) {
        KtTokens.EQEQ, KtTokens.EQEQEQ -> IFNE
        KtTokens.EXCLEQ, KtTokens.EXCLEQEQEQ -> IFEQ
        KtTokens.GT -> IFLE
        KtTokens.GTEQ -> IFLT
        KtTokens.LT -> IFGE
        KtTokens.LTEQ -> IFGT
        else -> {
            throw UnsupportedOperationException("Don't know how to generate this condJump: $opToken")
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
