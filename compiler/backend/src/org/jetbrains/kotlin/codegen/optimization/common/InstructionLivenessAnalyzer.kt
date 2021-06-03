/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.common

import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

class InstructionLivenessAnalyzer(val method: MethodNode) {
    private val instructions = method.instructions
    private val nInsns = instructions.size()

    private val isLive = BooleanArray(nInsns)

    private val handlers: Array<MutableList<TryCatchBlockNode>?> = arrayOfNulls(nInsns)
    private val queued = BooleanArray(nInsns)
    private val queue = IntArray(nInsns)
    private var top = 0

    private val AbstractInsnNode.indexOf get() = instructions.indexOf(this)

    fun analyze(): BooleanArray {
        if (nInsns == 0) return isLive
        checkAssertions()
        computeExceptionHandlersForEachInsn(method)
        initControlFlowAnalysis()
        traverseCfg()

        // We consider labels referenced by LVs and TCBs always implicitly reachable
        // (so that they don't get accidentally removed, producing corrupted class files),
        // and let the client code decide what to do with redundant LVs and TCBs.
        localVariableAndTryCatchBlockLabelsAreAlwaysLive()

        // Last label in a method is always implicitly reachable (preserving our implicit invariants).
        if (instructions.last is LabelNode) {
            isLive[instructions.last.indexOf] = true
        }

        return isLive
    }

    private fun traverseCfg() {
        while (top > 0) {
            val insn = queue[--top]
            val insnNode = method.instructions[insn]
            val insnOpcode = insnNode.opcode

            when (insnNode.type) {
                AbstractInsnNode.LABEL, AbstractInsnNode.LINE, AbstractInsnNode.FRAME ->
                    visitOpInsn(insn)
                AbstractInsnNode.JUMP_INSN ->
                    visitJumpInsnNode(insnNode as JumpInsnNode, insn, insnOpcode)
                AbstractInsnNode.LOOKUPSWITCH_INSN ->
                    visitLookupSwitchInsnNode(insnNode as LookupSwitchInsnNode)
                AbstractInsnNode.TABLESWITCH_INSN ->
                    visitTableSwitchInsnNode(insnNode as TableSwitchInsnNode)
                else -> {
                    if (insnOpcode != Opcodes.ATHROW && (insnOpcode < Opcodes.IRETURN || insnOpcode > Opcodes.RETURN)) {
                        visitOpInsn(insn)
                    }
                }
            }

            handlers[insn]?.forEach { tcb ->
                visitControlFlowEdge(tcb.handler.indexOf)
            }
        }
    }

    private fun localVariableAndTryCatchBlockLabelsAreAlwaysLive() {
        for (localVariable in method.localVariables) {
            isLive[localVariable.start.indexOf] = true
            isLive[localVariable.end.indexOf] = true
        }

        for (tcb in method.tryCatchBlocks) {
            isLive[tcb.start.indexOf] = true
            isLive[tcb.end.indexOf] = true
            isLive[tcb.handler.indexOf] = true
        }
    }

    private fun checkAssertions() {
        if (instructions.any { it.opcode == Opcodes.JSR || it.opcode == Opcodes.RET })
            throw AssertionError("Subroutines are deprecated since Java 6")
    }

    private fun visitOpInsn(insn: Int) {
        visitControlFlowEdge(insn + 1)
    }

    private fun visitTableSwitchInsnNode(insnNode: TableSwitchInsnNode) {
        var jump = insnNode.dflt.indexOf
        visitControlFlowEdge(jump)
        for (label in insnNode.labels) {
            jump = instructions.indexOf(label)
            visitControlFlowEdge(jump)
        }
    }

    private fun visitLookupSwitchInsnNode(insnNode: LookupSwitchInsnNode) {
        var jump = insnNode.dflt.indexOf
        visitControlFlowEdge(jump)
        for (label in insnNode.labels) {
            jump = label.indexOf
            visitControlFlowEdge(jump)
        }
    }

    private fun visitJumpInsnNode(insnNode: JumpInsnNode, insn: Int, insnOpcode: Int) {
        if (insnOpcode != Opcodes.GOTO && insnOpcode != Opcodes.JSR) {
            visitControlFlowEdge(insn + 1)
        }
        val jump = insnNode.label.indexOf
        visitControlFlowEdge(jump)
    }

    private fun initControlFlowAnalysis() {
        visitControlFlowEdge(0)
    }

    private fun computeExceptionHandlersForEachInsn(m: MethodNode) {
        for (tcb in m.tryCatchBlocks) {
            val begin = tcb.start.indexOf
            val end = tcb.end.indexOf
            for (j in begin until end) {
                var insnHandlers = handlers[j]
                if (insnHandlers == null) {
                    insnHandlers = ArrayList<TryCatchBlockNode>()
                    handlers[j] = insnHandlers
                }
                insnHandlers.add(tcb)
            }
        }
    }

    private fun visitControlFlowEdge(insn: Int) {
        val changes = !isLive[insn]
        isLive[insn] = true
        if (changes && !queued[insn]) {
            queued[insn] = true
            queue[top++] = insn
        }
    }

}
