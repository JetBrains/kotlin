/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.coroutines

import org.jetbrains.kotlin.codegen.inline.isReturnsUnitMarker
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

/*
 * Replace POP with ARETURN iff
 * 1) It is immediately followed by { GETSTATIC Unit.INSTANCE, ARETURN } sequences
 * 2) It is poping Unit
 */
object ReturnUnitMethodTransformer : MethodTransformer() {
    override fun transform(internalClassName: String, methodNode: MethodNode) {
        val unitMarks = findReturnsUnitMarks(methodNode)
        if (unitMarks.isEmpty()) return

        val units = findReturnUnitSequences(methodNode)
        if (units.isEmpty()) {
            cleanUpReturnsUnitMarkers(methodNode, unitMarks)
            return
        }

        val pops = methodNode.instructions.asSequence().filter { it.opcode == Opcodes.POP }.toList()
        val popSuccessors = findSuccessors(methodNode, pops)
        val sourceInsns = findSourceInstructions(internalClassName, methodNode, pops)
        val safePops = filterOutUnsafes(popSuccessors, units, sourceInsns)

        // Replace POP with ARETURN for tail call optimization
        safePops.forEach { methodNode.instructions.set(it, InsnNode(Opcodes.ARETURN)) }
        cleanUpReturnsUnitMarkers(methodNode, unitMarks)
    }

    // Return list of POPs, which can be safely replaced by ARETURNs
    private fun filterOutUnsafes(
        popSuccessors: Map<AbstractInsnNode, Collection<AbstractInsnNode>>,
        units: Collection<AbstractInsnNode>,
        sourceInsns: Map<AbstractInsnNode, Collection<AbstractInsnNode>>
    ): Collection<AbstractInsnNode> {
        return popSuccessors.filter { (pop, successors) ->
            successors.all { it in units } &&
                    sourceInsns[pop].sure { "Sources of $pop cannot be null" }.all(::isSuspendingCallReturningUnit)
        }.keys
    }

    // Find instructions which do something on stack, ignoring markers
    // Return map {insn => list of found instructions}
    private fun findSuccessors(
        methodNode: MethodNode,
        insns: List<AbstractInsnNode>
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

    private fun findSourceInstructions(
        internalClassName: String,
        methodNode: MethodNode,
        pops: Collection<AbstractInsnNode>
    ): Map<AbstractInsnNode, Collection<AbstractInsnNode>> {
        val frames = analyze(internalClassName, methodNode, IgnoringCopyOperationSourceInterpreter())
        return pops.keysToMap {
            val index = methodNode.instructions.indexOf(it)
            if (isUnreachable(index, frames)) return@keysToMap emptySet<AbstractInsnNode>()
            frames[index].getStack(0).insns
        }
    }

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

