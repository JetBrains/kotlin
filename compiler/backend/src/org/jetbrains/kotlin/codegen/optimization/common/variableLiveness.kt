/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.IincInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode
import java.util.*


class VariableLivenessFrame(val maxLocals: Int) : VarFrame<VariableLivenessFrame> {
    private val bitSet = BitSet(maxLocals)

    override fun mergeFrom(other: VariableLivenessFrame) {
        bitSet.or(other.bitSet)
    }

    fun markAlive(varIndex: Int) {
        bitSet.set(varIndex, true)
    }

    fun markDead(varIndex: Int) {
        bitSet.set(varIndex, false)
    }

    fun isAlive(varIndex: Int): Boolean = bitSet.get(varIndex)

    override fun equals(other: Any?): Boolean {
        if (other !is VariableLivenessFrame) return false
        return bitSet == other.bitSet
    }

    override fun hashCode() = bitSet.hashCode()
}

fun analyzeLiveness(node: MethodNode): List<VariableLivenessFrame> =
    analyze(node, object : BackwardAnalysisInterpreter<VariableLivenessFrame> {
        override fun newFrame(maxLocals: Int) = VariableLivenessFrame(maxLocals)
        override fun def(frame: VariableLivenessFrame, insn: AbstractInsnNode) = defVar(frame, insn)
        override fun use(frame: VariableLivenessFrame, insn: AbstractInsnNode) =
            useVar(frame, insn)
    })

private fun defVar(frame: VariableLivenessFrame, insn: AbstractInsnNode) {
    if (insn is VarInsnNode && insn.isStoreOperation()) {
        frame.markDead(insn.`var`)
    }
}

private fun useVar(frame: VariableLivenessFrame, insn: AbstractInsnNode) {
    if (insn is VarInsnNode && insn.isLoadOperation()) {
        frame.markAlive(insn.`var`)
    } else if (insn is IincInsnNode) {
        frame.markAlive(insn.`var`)
    }
}