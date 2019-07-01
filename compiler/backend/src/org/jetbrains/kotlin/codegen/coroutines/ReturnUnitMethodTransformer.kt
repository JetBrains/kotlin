/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.codegen.optimization.boxing.isUnitInstance
import org.jetbrains.kotlin.codegen.optimization.common.ControlFlowGraph
import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.codegen.optimization.common.removeAll
import org.jetbrains.kotlin.codegen.optimization.transformer.MethodTransformer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.kotlin.utils.keysToMap
import org.jetbrains.kotlin.utils.sure
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.tree.analysis.SourceInterpreter

/*
 * Replace POP with ARETURN iff
 * 1) It is immediately followed by { GETSTATIC Unit.INSTANCE, ARETURN } sequences
 * 2) It is poping Unit
 *
 * Replace CHECKCAST Unit whit ARETURN iff
 * It is successed by ARETURN and it casts Unit
 */
object ReturnUnitMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val unitMarks = findReturnsUnitMarks(methodNode)
        if (unitMarks.isEmpty()) return

        replaceCheckcastUnitWithAreturn(methodNode, internalClassName)
        replacePopWithAreturn(methodNode, internalClassName)

        cleanUpReturnsUnitMarkers(methodNode, unitMarks)
    }

    private fun replacePopWithAreturn(
        methodNode: MethodNode,
        internalClassName: String
    ) {
        val units = findReturnUnitSequences(methodNode)
        if (units.isEmpty()) return

        replaceSafeInsnsWithUnit(methodNode, internalClassName, units) { it.opcode == Opcodes.POP }
    }

    private fun replaceCheckcastUnitWithAreturn(methodNode: MethodNode, internalClassName: String) {
        val areturns = methodNode.instructions.asSequence().filter { it.opcode == Opcodes.ARETURN }.toList()

        replaceSafeInsnsWithUnit(methodNode, internalClassName, areturns) { it.opcode == Opcodes.CHECKCAST }
    }

    // Find all instructions, which can be safely replaced with ARETURN and replace them with ARETURN for tail-call optimization
    private fun replaceSafeInsnsWithUnit(
        methodNode: MethodNode,
        internalClassName: String,
        safeSuccessors: Collection<AbstractInsnNode>,
        predicate: (AbstractInsnNode) -> Boolean
    ) {
        val insns = methodNode.instructions.asSequence().filter(predicate).toList()
        val successors = findSuccessors(methodNode, insns)
        val sourceInsns = findSourceInstructions(internalClassName, methodNode, insns, ignoreCopy = true)
        val safeInsns = filterOutUnsafes(successors, safeSuccessors, sourceInsns)
        safeInsns.forEach { methodNode.instructions.set(it, InsnNode(Opcodes.ARETURN)) }
    }

    // Return list of instructions, which can be safely replaced by ARETURNs
    private fun filterOutUnsafes(
        successors: Map<AbstractInsnNode, Collection<AbstractInsnNode>>,
        safeSuccessors: Collection<AbstractInsnNode>,
        sources: Map<AbstractInsnNode, Collection<AbstractInsnNode>>
    ): Collection<AbstractInsnNode> {
        return successors.filter { (insn, successors) ->
            successors.all { it in safeSuccessors } &&
                    sources[insn].sure { "Sources of $insn cannot be null" }.all(::isSuspendingCallReturningUnit)
        }.keys
    }

    // Find instructions which do something on stack, ignoring markers
    // Return map {insn => list of found instructions}
    private fun findSuccessors(
        methodNode: MethodNode,
        insns: Collection<AbstractInsnNode>
    ): Map<AbstractInsnNode, Collection<AbstractInsnNode>> {
        val cfg = ControlFlowGraph.build(methodNode)

        return insns.keysToMap { findSuccessorsDFS(it, cfg, methodNode) }
    }

    // Find all meaningful successors of insn
    private fun findSuccessorsDFS(insn: AbstractInsnNode, cfg: ControlFlowGraph, methodNode: MethodNode): Collection<AbstractInsnNode> {
        val visited = hashSetOf<AbstractInsnNode>()

        fun dfs(current: AbstractInsnNode): Collection<AbstractInsnNode> {
            if (!visited.add(current)) return emptySet()

            return cfg.getSuccessorsIndices(current).flatMap {
                val succ = methodNode.instructions[it]
                when {
                    !succ.isMeaningful || succ is JumpInsnNode || succ.opcode == Opcodes.NOP -> dfs(succ)
                    succ.isUnitInstance() -> {
                        // There can be multiple chains of { UnitInstance, POP } after inlining. Ignore them
                        val newSuccessors = dfs(succ)
                        if (newSuccessors.all { it.opcode == Opcodes.POP }) newSuccessors.flatMap { dfs(it) }
                        else setOf(succ)
                    }
                    else -> setOf(succ)
                }
            }
        }

        return dfs(insn)
    }

    private fun isSuspendingCallReturningUnit(node: AbstractInsnNode): Boolean =
        node.safeAs<MethodInsnNode>()?.next?.next?.let(::isReturnsUnitMarker) == true

    // Find { GETSTATIC kotlin/Unit.INSTANCE, ARETURN } sequences
    // Result is list of GETSTATIC kotlin/Unit.INSTANCE instructions
    private fun findReturnUnitSequences(methodNode: MethodNode): Collection<AbstractInsnNode> =
        methodNode.instructions.asSequence().filter { it.isUnitInstance() && it.next?.opcode == Opcodes.ARETURN }.toList()

    internal fun findReturnsUnitMarks(methodNode: MethodNode): Collection<AbstractInsnNode> =
        methodNode.instructions.asSequence().filter(::isReturnsUnitMarker).toList()

    internal fun cleanUpReturnsUnitMarkers(methodNode: MethodNode, unitMarks: Collection<AbstractInsnNode>) {
        unitMarks.forEach { methodNode.instructions.removeAll(listOf(it.previous, it)) }
    }
}

internal fun findSourceInstructions(
    internalClassName: String,
    methodNode: MethodNode,
    insns: Collection<AbstractInsnNode>,
    ignoreCopy: Boolean
): Map<AbstractInsnNode, Collection<AbstractInsnNode>> {
    val frames = MethodTransformer.analyze(
        internalClassName,
        methodNode,
        if (ignoreCopy) IgnoringCopyOperationSourceInterpreter() else SourceInterpreter()
    )
    return insns.keysToMap {
        val index = methodNode.instructions.indexOf(it)
        if (isUnreachable(index, frames)) return@keysToMap emptySet<AbstractInsnNode>()
        frames[index].getStack(0).insns
    }
}
