/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization

import org.jetbrains.kotlin.codegen.optimization.common.nodeType
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.JumpInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

class NegatedJumpsMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val insnList = methodNode.instructions

        // Replace sequence of instructions such as
        //      IF_ICMPLT L1            = insn
        //      GOTO L2                 = next1
        //    L1:                       = next2
        // with
        //      IF_ICMPGE L2            = negatedJumpInsn
        //    L1:                       = next2
        for (insn in insnList.toArray()) {
            if (insn.nodeType != AbstractInsnNode.JUMP_INSN || insn.opcode == Opcodes.GOTO) continue
            val next1 = insn.next ?: continue
            if (next1.opcode != Opcodes.GOTO) continue
            val next2 = next1.next ?: continue
            if (next2 != (insn as JumpInsnNode).label) continue

            val negatedJumpInsn = JumpInsnNode(negateConditionalJumpOpcode(insn.opcode), (next1 as JumpInsnNode).label)
            insnList.insertBefore(insn, negatedJumpInsn)
            insnList.remove(insn)
            insnList.remove(next1)
        }
    }

    private val negatedConditionalJumpOpcode = IntArray(255).also { a ->
        fun negated(opcode1: Int, opcode2: Int) {
            a[opcode1] = opcode2
            a[opcode2] = opcode1
        }
        negated(Opcodes.IFNULL, Opcodes.IFNONNULL)
        negated(Opcodes.IFEQ, Opcodes.IFNE)
        negated(Opcodes.IFLT, Opcodes.IFGE)
        negated(Opcodes.IFLE, Opcodes.IFGT)
        negated(Opcodes.IF_ICMPEQ, Opcodes.IF_ICMPNE)
        negated(Opcodes.IF_ICMPLT, Opcodes.IF_ICMPGE)
        negated(Opcodes.IF_ICMPLE, Opcodes.IF_ICMPGT)
        negated(Opcodes.IF_ACMPEQ, Opcodes.IF_ACMPNE)
    }

    private fun negateConditionalJumpOpcode(opcode: Int): Int =
        negatedConditionalJumpOpcode[opcode]
}
