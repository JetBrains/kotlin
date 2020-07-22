/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_CALL_RESULT_NAME
import org.jetbrains.kotlin.codegen.coroutines.SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME
import org.jetbrains.kotlin.codegen.inline.isFakeLocalVariableForInline
import org.jetbrains.org.objectweb.asm.tree.*
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

    override fun toString(): String = (0 until maxLocals).map { if (bitSet[it]) '@' else '_' }.joinToString(separator = "")
}

fun analyzeLiveness(method: MethodNode): List<VariableLivenessFrame> =
    analyze(method, object : BackwardAnalysisInterpreter<VariableLivenessFrame> {
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

/* We do not want to spill dead variables, thus, we shrink its LVT record to region, where the variable is alive,
 * so, the variable will not be visible in debugger. User can still prolong life span of the variable by using it.
 *
 * This means, that function parameters do not longer span the whole function, including `this`.
 * This might and will break some bytecode processors, including old versions of R8. See KT-24510.
 */
fun updateLvtAccordingToLiveness(method: MethodNode) {
    val liveness = analyzeLiveness(method)

    fun List<LocalVariableNode>.findRecord(insnIndex: Int, variableIndex: Int): LocalVariableNode? {
        for (variable in this) {
            if (variable.index == variableIndex &&
                method.instructions.indexOf(variable.start) <= insnIndex &&
                insnIndex < method.instructions.indexOf(variable.end)
            ) return variable
        }
        return null
    }

    fun isAlive(insnIndex: Int, variableIndex: Int): Boolean =
        liveness[insnIndex].isAlive(variableIndex)

    val oldLvt = arrayListOf<LocalVariableNode>()
    for (record in method.localVariables) {
        oldLvt += record
    }
    method.localVariables.clear()
    for (variableIndex in 0 until method.maxLocals) {
        if (oldLvt.none { it.index == variableIndex }) continue
        var startLabel: LabelNode? = null
        for (insnIndex in 0 until (method.instructions.size() - 1)) {
            val insn = method.instructions[insnIndex]
            if (!isAlive(insnIndex, variableIndex) && isAlive(insnIndex + 1, variableIndex)) {
                startLabel = insn as? LabelNode ?: insn.findNextOrNull { it is LabelNode } as? LabelNode
            }
            if (isAlive(insnIndex, variableIndex) && !isAlive(insnIndex + 1, variableIndex)) {
                // No variable in LVT -> do not add one
                val lvtRecord = oldLvt.findRecord(insnIndex, variableIndex) ?: continue
                val endLabel = insn as? LabelNode ?: insn.findNextOrNull { it is LabelNode } as? LabelNode ?: continue
                // startLabel can be null in case of parameters
                @Suppress("NAME_SHADOWING") val startLabel = startLabel ?: lvtRecord.start
                // No LINENUMBER in range -> no way to put a breakpoint -> do not bother adding a record
                if (InsnSequence(startLabel, endLabel).none { it is LineNumberNode }) continue
                method.localVariables.add(
                    LocalVariableNode(lvtRecord.name, lvtRecord.desc, lvtRecord.signature, startLabel, endLabel, lvtRecord.index)
                )
            }
        }
    }

    for (variable in oldLvt) {
        // $completion and $result are dead, but they are used by debugger, as well as fake inliner variables
        if (variable.name == SUSPEND_FUNCTION_COMPLETION_PARAMETER_NAME ||
            variable.name == SUSPEND_CALL_RESULT_NAME ||
            isFakeLocalVariableForInline(variable.name)
        ) {
            method.localVariables.add(variable)
        }
    }
}
