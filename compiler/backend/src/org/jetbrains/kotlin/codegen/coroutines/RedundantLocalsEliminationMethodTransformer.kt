/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.ControlFlowGraph
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

// Inliner emits a lot of locals during inlining.
// Remove all of them since these locals are
//  1) going to be spilled into continuation object
//  2) breaking tail-call elimination
class RedundantLocalsEliminationMethodTransformer(private val languageVersionSettings: LanguageVersionSettings) : MethodTransformer() {
    lateinit var internalClassName: String
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        this.internalClassName = internalClassName
        do {
            var changed = false
            changed = simpleRemove(methodNode) || changed
            changed = removeWithReplacement(methodNode) || changed
            changed = removeAloadCheckcastContinuationAstore(methodNode, languageVersionSettings) || changed
        } while (changed)
    }

    // Replace
    //  GETSTATIC kotlin/Unit.INSTANCE
    //  ASTORE N
    //  ...
    //  ALOAD N
    // with
    //  ...
    //  GETSTATIC kotlin/Unit.INSTANCE
    // or
    //  ACONST_NULL
    //  ASTORE N
    //  ...
    //  ALOAD N
    // with
    //  ...
    //  ACONST_NULL
    // or
    //  ALOAD K
    //  ASTORE N
    //  ...
    //  ALOAD N
    // with
    //  ...
    //  ALOAD K
    //
    // But do not remove several at a time, since the same local (for example, ALOAD 0) might be loaded and stored multiple times in
    // sequence, like
    //  ALOAD 0
    //  ASTORE 1
    //  ALOAD 1
    //  ASTORE 2
    //  ALOAD 3
    // Here, it is unsafe to replace ALOAD 3 with ALOAD 1, and then already removed ALOAD 1 with ALOAD 0.
    private fun removeWithReplacement(
        methodNode: MethodNode
    ): Boolean {
        val cfg = ControlFlowGraph.build(methodNode)
        val insns = findSafeAstorePredecessors(methodNode, cfg, ignoreLocalVariableTable = false) {
            it.isUnitInstance() || it.opcode == Opcodes.ACONST_NULL || it.opcode == Opcodes.ALOAD
        }
        for ((pred, astore) in insns) {
            val aload = findSingleLoadFromAstore(astore, cfg, methodNode) ?: continue

            methodNode.instructions.removeAll(listOf(pred, astore))
            methodNode.instructions.set(aload, pred.clone())
            return true
        }
        return false
    }

    private fun findSingleLoadFromAstore(
        astore: AbstractInsnNode,
        cfg: ControlFlowGraph,
        methodNode: MethodNode
    ): AbstractInsnNode? {
        val aload = methodNode.instructions.asSequence()
            .singleOrNull { it.opcode == Opcodes.ALOAD && it.localIndex() == astore.localIndex() } ?: return null
        val succ = findImmediateSuccessors(astore, cfg, methodNode).singleOrNull() ?: return null
        return if (aload == succ) aload else null
    }

    private fun AbstractInsnNode.clone() = when (this) {
        is FieldInsnNode -> FieldInsnNode(opcode, owner, name, desc)
        is VarInsnNode -> VarInsnNode(opcode, `var`)
        is InsnNode -> InsnNode(opcode)
        is TypeInsnNode -> TypeInsnNode(opcode, desc)
        else -> error("clone of $this is not implemented yet")
    }

    // Remove
    //  ALOAD N
    //  POP
    // or
    //  ACONST_NULL
    //  POP
    // or
    //  GETSTATIC kotlin/Unit.INSTANCE
    //  POP
    private fun simpleRemove(methodNode: MethodNode): Boolean {
        val insns =
            findPopPredecessors(methodNode) { it.isUnitInstance() || it.opcode == Opcodes.ACONST_NULL || it.opcode == Opcodes.ALOAD }
        for ((pred, pop) in insns) {
            methodNode.instructions.removeAll(listOf(pred, pop))
        }
        return insns.isNotEmpty()
    }

    private fun findPopPredecessors(
        methodNode: MethodNode,
        predicate: (AbstractInsnNode) -> Boolean
    ): Map<AbstractInsnNode, AbstractInsnNode> {
        val insns = methodNode.instructions.asSequence().filter { predicate(it) }.toList()

        val cfg = ControlFlowGraph.build(methodNode)

        val res = hashMapOf<AbstractInsnNode, AbstractInsnNode>()
        for (insn in insns) {
            val succ = findImmediateSuccessors(insn, cfg, methodNode).singleOrNull() ?: continue
            if (succ.opcode != Opcodes.POP) continue
            if (insn.opcode == Opcodes.ALOAD && methodNode.localVariables.firstOrNull { it.index == insn.localIndex() } != null) continue
            val sources = findSourceInstructions(internalClassName, methodNode, listOf(succ), ignoreCopy = false).values.flatten()
            if (sources.size != 1) continue
            res[insn] = succ
        }
        return res
    }

    // Replace
    //  ALOAD K
    //  CHECKCAST Continuation
    //  ASTORE N
    //  ...
    //  ALOAD N
    // with
    //  ...
    //  ALOAD K
    //  CHECKCAST Continuation
    private fun removeAloadCheckcastContinuationAstore(methodNode: MethodNode, languageVersionSettings: LanguageVersionSettings): Boolean {
        // Here we ignore the duplicates of continuation in local variable table,
        // Since it increases performance greatly.
        val cfg = ControlFlowGraph.build(methodNode)
        val insns = findSafeAstorePredecessors(methodNode, cfg, ignoreLocalVariableTable = true) {
            it.opcode == Opcodes.CHECKCAST &&
                    (it as TypeInsnNode).desc == languageVersionSettings.continuationAsmType().internalName &&
                    it.previous?.opcode == Opcodes.ALOAD
        }

        var changed = false

        for ((checkcast, astore) in insns) {
            val aloadk = checkcast.previous
            val aloadn = findSingleLoadFromAstore(astore, cfg, methodNode) ?: continue

            methodNode.instructions.removeAll(listOf(aloadk, checkcast, astore))
            methodNode.instructions.insertBefore(aloadn, aloadk.clone())
            methodNode.instructions.set(aloadn, checkcast.clone())
            changed = true
        }
        return changed
    }

    private fun findSafeAstorePredecessors(
        methodNode: MethodNode,
        cfg: ControlFlowGraph,
        ignoreLocalVariableTable: Boolean,
        predicate: (AbstractInsnNode) -> Boolean
    ): Map<AbstractInsnNode, AbstractInsnNode> {
        val insns = methodNode.instructions.asSequence().filter { predicate(it) }.toList()
        val res = hashMapOf<AbstractInsnNode, AbstractInsnNode>()

        for (insn in insns) {
            val succ = findImmediateSuccessors(insn, cfg, methodNode).singleOrNull() ?: continue
            if (succ.opcode != Opcodes.ASTORE) continue
            if (methodNode.instructions.asSequence().count {
                    it.opcode == Opcodes.ASTORE && it.localIndex() == succ.localIndex()
                } != 1) continue
            if (!ignoreLocalVariableTable && methodNode.localVariables.firstOrNull { it.index == succ.localIndex() } != null) continue
            val sources = findSourceInstructions(internalClassName, methodNode, listOf(succ), ignoreCopy = false).values.flatten()
            if (sources.size > 1) continue
            res[insn] = succ
        }

        return res
    }

    // Find all meaningful successors of insn
    private fun findImmediateSuccessors(
        insn: AbstractInsnNode,
        cfg: ControlFlowGraph,
        methodNode: MethodNode
    ): Collection<AbstractInsnNode> {
        val visited = hashSetOf<AbstractInsnNode>()

        fun dfs(current: AbstractInsnNode): Collection<AbstractInsnNode> {
            if (!visited.add(current)) return emptySet()

            return cfg.getSuccessorsIndices(current).flatMap {
                val succ = methodNode.instructions[it]
                if (!succ.isMeaningful || succ is JumpInsnNode || succ.opcode == Opcodes.NOP) dfs(succ)
                else setOf(succ)
            }
        }

        return dfs(insn)
    }

    private fun AbstractInsnNode.localIndex(): Int {
        assert(this is VarInsnNode)
        return (this as VarInsnNode).`var`
    }
}