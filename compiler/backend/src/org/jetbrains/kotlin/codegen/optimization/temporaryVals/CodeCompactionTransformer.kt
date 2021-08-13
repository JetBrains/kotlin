/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.optimization.temporaryVals

import org.jetbrains.kotlin.codegen.optimization.DeadCodeEliminationMethodTransformer
import org.jetbrains.kotlin.codegen.optimization.common.InsnSequence
import org.jetbrains.kotlin.codegen.optimization.common.removeUnusedLocalVariables
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import kotlin.math.max

class CodeCompactionTransformer : MethodTransformer() {
    private val temporaryValsAnalyzer = TemporaryValsAnalyzer()
    private val deadCodeElimination = DeadCodeEliminationMethodTransformer()

    override fun transform(internalClassName: String, methodNode: MethodNode) {
        simplifyControlFlow(methodNode)

        val cfg = ControlFlowGraph(methodNode)
        processLabels(cfg)

        simplifyKnownSafeCallPatterns(cfg)

        val temporaryVals = temporaryValsAnalyzer.analyze(internalClassName, methodNode)
        if (temporaryVals.isNotEmpty()) {
            optimizeTemporaryVals(cfg, temporaryVals)
        }

        methodNode.stripTemporaryValInitMarkers()
        methodNode.removeUnusedLocalVariables()
    }


    private class ControlFlowGraph(val methodNode: MethodNode) {
        private val nonTrivialPredecessors = HashMap<LabelNode, MutableList<AbstractInsnNode>>()

        fun reset() {
            nonTrivialPredecessors.clear()
        }

        fun addNonTrivialPredecessor(label: LabelNode, pred: AbstractInsnNode) {
            nonTrivialPredecessors.getOrPut(label) { SmartList() }.add(pred)
        }

        fun hasNonTrivialPredecessors(label: LabelNode) =
            nonTrivialPredecessors.containsKey(label)

        fun hasSinglePredecessor(label: LabelNode, expectedPredecessor: AbstractInsnNode): Boolean {
            var trivialPredecessor = label.previous
            if (trivialPredecessor.opcode == Opcodes.GOTO ||
                trivialPredecessor.opcode in Opcodes.IRETURN..Opcodes.RETURN ||
                trivialPredecessor.opcode == Opcodes.ATHROW
            ) {
                // Previous instruction is not a predecessor in CFG
                trivialPredecessor = null
            } else {
                // Check trivial predecessor
                if (trivialPredecessor != expectedPredecessor) return false
            }

            val nonTrivialPredecessors = nonTrivialPredecessors[label]
                ?: return trivialPredecessor != null

            return when {
                nonTrivialPredecessors.size > 1 ->
                    false
                nonTrivialPredecessors.size == 0 ->
                    trivialPredecessor == expectedPredecessor
                else ->
                    trivialPredecessor == null && nonTrivialPredecessors[0] == expectedPredecessor
            }
        }
    }


    private fun processLabels(cfg: ControlFlowGraph) {
        cfg.reset()

        val methodNode = cfg.methodNode
        val insnList = methodNode.instructions

        val usedLabels = HashSet<LabelNode>()
        val first = insnList.first
        if (first is LabelNode) {
            usedLabels.add(first)
        }
        val last = insnList.last
        if (last is LabelNode) {
            usedLabels.add(last)
        }

        fun addCfgEdgeToLabel(from: AbstractInsnNode, label: LabelNode) {
            usedLabels.add(label)
            cfg.addNonTrivialPredecessor(label, from)
        }

        fun addCfgEdgesToLabels(from: AbstractInsnNode, labels: Collection<LabelNode>) {
            usedLabels.addAll(labels)
            for (label in labels) {
                cfg.addNonTrivialPredecessor(label, from)
            }
        }

        for (insn in insnList) {
            when (insn.type) {
                AbstractInsnNode.LINE -> {
                    usedLabels.add((insn as LineNumberNode).start)
                }
                AbstractInsnNode.JUMP_INSN -> {
                    addCfgEdgeToLabel(insn, (insn as JumpInsnNode).label)
                }
                AbstractInsnNode.LOOKUPSWITCH_INSN -> {
                    val switchInsn = insn as LookupSwitchInsnNode
                    addCfgEdgeToLabel(insn, switchInsn.dflt)
                    addCfgEdgesToLabels(insn, switchInsn.labels)
                }
                AbstractInsnNode.TABLESWITCH_INSN -> {
                    val switchInsn = insn as TableSwitchInsnNode
                    addCfgEdgeToLabel(insn, switchInsn.dflt)
                    addCfgEdgesToLabels(insn, switchInsn.labels)
                }
            }
        }
        for (lv in methodNode.localVariables) {
            usedLabels.add(lv.start)
            usedLabels.add(lv.end)
        }
        for (tcb in methodNode.tryCatchBlocks) {
            usedLabels.add(tcb.start)
            usedLabels.add(tcb.end)
            addCfgEdgeToLabel(tcb.start, tcb.handler)
        }

        var insn = insnList.first
        while (insn != null) {
            insn = if (insn is LabelNode && insn !in usedLabels) {
                val next = insn.next
                insnList.remove(insn)
                next
            } else {
                insn.next
            }
        }
    }

    private fun optimizeTemporaryVals(cfg: ControlFlowGraph, temporaryVals: List<TemporaryVal>) {
        val insnList = cfg.methodNode.instructions

        for (tmp in temporaryVals) {
            if (tmp.loadInsns.size == 1) {
                val storeInsn = tmp.storeInsn
                val loadInsn = tmp.loadInsns[0]

                if (storeInsn.next == loadInsn || InsnSequence(storeInsn.next, loadInsn).none { it.isIntervening(cfg) }) {
                    // If there are no intervening instructions between store and load,
                    // drop both store and load, just keep intermediate value on stack.
                    // This approximately corresponds to some receiver stored in a temporary variable and immediately loaded
                    // (e.g., in safe call)
                    insnList.remove(storeInsn)
                    insnList.remove(loadInsn)
                    continue
                }

                if (storeInsn.matchOpcodes(Opcodes.ASTORE, Opcodes.ALOAD, Opcodes.ALOAD)) {
                    val aLoad1 = storeInsn.next as VarInsnNode
                    val aLoad2 = aLoad1.next as VarInsnNode
                    if (aLoad2 == loadInsn) {
                        // Replace instruction sequence:
                        //      ASTORE tmp
                        //      ALOAD x
                        //      ALOAD tmp
                        // with:
                        //      ALOAD x
                        //      SWAP
                        // This approximately corresponds to some extension receiver stored in a temporary variable and immediately loaded
                        // (e.g., in safe call).
                        insnList.remove(storeInsn)
                        insnList.remove(loadInsn)
                        insnList.insert(aLoad1, InsnNode(Opcodes.SWAP))
                        continue
                    }
                }

                if (storeInsn.matchOpcodes(Opcodes.ASTORE, Opcodes.GETSTATIC, Opcodes.ALOAD)) {
                    val getStaticInsn = storeInsn.next as FieldInsnNode
                    val aLoad2 = getStaticInsn.next as VarInsnNode
                    if (aLoad2 == loadInsn && Type.getType(getStaticInsn.desc).size == 1) {
                        // Replace instruction sequence:
                        //      ASTORE tmp
                        //      GETSTATIC ... // size 1
                        //      ALOAD tmp
                        // with:
                        //      GETSTATIC ... // size 1
                        //      SWAP
                        // This approximately corresponds to some extension receiver stored in a temporary variable and immediately loaded
                        // (e.g., in safe call).
                        insnList.remove(storeInsn)
                        insnList.remove(loadInsn)
                        insnList.insert(getStaticInsn, InsnNode(Opcodes.SWAP))
                        continue
                    }
                }
            }
        }
    }

    private fun simplifyControlFlow(methodNode: MethodNode) {
        val insnList = methodNode.instructions
        var needsDCE = false

        for (insn in insnList.toArray()) {
            if (insn.opcode == Opcodes.NOP) {
                // Remove NOPs not preceded by LABEL or LINENUMBER instructions.
                val prev = insn.previous ?: continue
                if (prev.type == AbstractInsnNode.LABEL || prev.type == AbstractInsnNode.LINE) continue

                insnList.remove(insn)
                continue
            }

            if (insn.opcode == Opcodes.GOTO) {
                // If we have a GOTO instruction that leads to another GOTO, replace corresponding label.
                val jumpInsn = insn as JumpInsnNode
                val newLabel = jumpInsn.getFinalLabel()
                if (newLabel != jumpInsn.label) {
                    needsDCE = true
                }
                jumpInsn.label = newLabel
            }
        }

        if (needsDCE) {
            deadCodeElimination.transform("<fake>", methodNode)
        }
    }

    @Suppress("DuplicatedCode")
    private fun simplifyKnownSafeCallPatterns(cfg: ControlFlowGraph) {
        val insnList = cfg.methodNode.instructions

        var maxStackIncrement = 0
        for (insn in insnList.toArray()) {
            if (insn.matchOpcodes(Opcodes.ALOAD, Opcodes.IFNONNULL)) {
                // This looks like a start of some safe call:
                // In a safe call, we introduce a temporary variable for a safe receiver, which is usually loaded twice:
                // one time for a null check (1) and another time for an actual call (2):
                //      { ... evaluate receiver ... }
                //      ASTORE v
                //      ALOAD v
                //      IFNONNULL L
                //      { ... if null ... }
                //  L:
                //      [ possible first receiver - ALOAD or GETSTATIC ]
                //      ALOAD v
                //      { ... call ... }
                // Try to remove a load instruction for (2), so that the safe call would look like:
                //      { ... evaluate receiver ... }
                //      ASTORE v
                //      ALOAD v
                //      DUP
                //      IFNONNULL L
                //      POP
                //      { ... if null ... }
                //  L:
                //      [ possible dispatch receiver - ALOAD or GETSTATIC ]
                //      [ SWAP if there was a dispatch receiver ]
                //      { ... call ... }

                val aLoad1 = insn as VarInsnNode
                val ifNonNull = insn.next as JumpInsnNode
                val label1 = ifNonNull.label
                if (!cfg.hasSinglePredecessor(ifNonNull.label, ifNonNull)) continue

                val label1Next = label1.next ?: continue
                if (label1Next.opcode == Opcodes.ALOAD) {
                    val aLoad2 = label1Next as VarInsnNode
                    if (aLoad2.`var` == aLoad1.`var`) {
                        // Rewrite:
                        //      ALOAD v
                        //      IFNONNULL L
                        //      ...
                        //  L:
                        //      ALOAD v
                        // to:
                        //      ALOAD v
                        //      DUP
                        //      IFNONNULL L
                        //      POP
                        //      ...
                        //  L:
                        //      ...
                        insnList.insertBefore(ifNonNull, InsnNode(Opcodes.DUP))
                        insnList.insert(ifNonNull, InsnNode(Opcodes.POP))
                        insnList.remove(aLoad2)
                        maxStackIncrement = max(maxStackIncrement, 1)
                        continue
                    } else {
                        val aLoad2Next = aLoad2.next
                        if (aLoad2Next.opcode == Opcodes.ALOAD) {
                            val aLoad3 = aLoad2Next as VarInsnNode
                            if (aLoad3.`var` == aLoad1.`var`) {
                                // Rewrite:
                                //      ALOAD v
                                //      IFNONNULL L
                                //      ...
                                //  L:
                                //      ALOAD x
                                //      ALOAD v
                                // to:
                                //      ALOAD v
                                //      DUP
                                //      IFNONNULL L
                                //      POP
                                //  L:
                                //      ALOAD x
                                //      SWAP
                                insnList.insertBefore(ifNonNull, InsnNode(Opcodes.DUP))
                                insnList.insert(ifNonNull, InsnNode(Opcodes.POP))
                                insnList.insert(aLoad2, InsnNode(Opcodes.SWAP))
                                insnList.remove(aLoad3)
                                maxStackIncrement = max(maxStackIncrement, 1)
                                continue
                            }
                        }
                    }
                } else if (label1Next.matchOpcodes(Opcodes.GETSTATIC, Opcodes.ALOAD)) {
                    val getStaticInsn = label1Next as FieldInsnNode
                    val aLoad3 = getStaticInsn.next as VarInsnNode
                    if (Type.getType(getStaticInsn.desc).size == 1 && aLoad3.`var` == aLoad1.`var`) {
                        // Rewrite:
                        //      ALOAD v
                        //      IFNONNULL L
                        //      ...
                        //  L:
                        //      GETSTATIC ... // size 1
                        //      ALOAD v
                        // to:
                        //      ALOAD v
                        //      DUP
                        //      IFNONNULL L
                        //      POP
                        //  L:
                        //      GETSTATIC ... // size 1
                        //      SWAP
                        insnList.insertBefore(ifNonNull, InsnNode(Opcodes.DUP))
                        insnList.insert(ifNonNull, InsnNode(Opcodes.POP))
                        insnList.insert(getStaticInsn, InsnNode(Opcodes.SWAP))
                        insnList.remove(aLoad3)
                        maxStackIncrement = max(maxStackIncrement, 1)
                        continue
                    }
                }
            }
        }

        cfg.methodNode.maxStack += maxStackIncrement
    }

    private fun JumpInsnNode.getFinalLabel(): LabelNode {
        var label = this.label
        var insn = label.next
        while (true) {
            when {
                insn.type == AbstractInsnNode.LABEL || insn.type == AbstractInsnNode.LINE -> {
                    insn = insn.next ?: break
                }
                insn.opcode == Opcodes.GOTO -> {
                    val newLabel = (insn as JumpInsnNode).label
                    if (newLabel == label) return newLabel
                    label = newLabel
                    insn = label.next ?: break
                }
                insn.opcode == Opcodes.NOP -> {
                    insn = insn.next ?: break
                }
                else -> break
            }
        }
        return label
    }

    private fun AbstractInsnNode.matchOpcodes(vararg opcodes: Int): Boolean {
        var insn = this
        for (i in opcodes.indices) {
            if (insn.opcode != opcodes[i]) return false
            insn = insn.next ?: return false
        }
        return true
    }

    private fun AbstractInsnNode.isIntervening(context: ControlFlowGraph): Boolean =
        when (this.type) {
            AbstractInsnNode.LINE, AbstractInsnNode.FRAME ->
                false
            AbstractInsnNode.LABEL ->
                context.hasNonTrivialPredecessors(this as LabelNode)
            AbstractInsnNode.INSN ->
                this.opcode != Opcodes.NOP
            else ->
                true
        }

}