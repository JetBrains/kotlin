/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg.pseudocodeTraverser

import org.jetbrains.kotlin.cfg.ControlFlowInfo
import org.jetbrains.kotlin.cfg.pseudocode.*
import java.util.*
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineEnterInstruction

fun Pseudocode.traverse(
        traversalOrder: TraversalOrder,
        analyzeInstruction: (Instruction) -> Unit
) {
    val instructions = getInstructions(traversalOrder)
    for (instruction in instructions) {
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.body.traverse(traversalOrder, analyzeInstruction)
        }
        analyzeInstruction(instruction)
    }
}

fun <D> Pseudocode.traverse(
        traversalOrder: TraversalOrder,
        edgesMap: Map<Instruction, Edges<D>>,
        analyzeInstruction: (Instruction, D, D) -> Unit
) {
    val instructions = getInstructions(traversalOrder)
    for (instruction in instructions) {
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.body.traverse(traversalOrder, edgesMap, analyzeInstruction)
        }
        val edges = edgesMap.get(instruction)
        if (edges != null) {
            analyzeInstruction(instruction, edges.incoming, edges.outgoing)
        }
    }
}

fun <I : ControlFlowInfo<*>> Pseudocode.collectData(
        traversalOrder: TraversalOrder,
        mergeDataWithLocalDeclarations: Boolean,
        mergeEdges: (Instruction, Collection<I>) -> Edges<I>,
        updateEdge: (Instruction, Instruction, I) -> I,
        initialInfo: I
): Map<Instruction, Edges<I>> {
    val edgesMap = LinkedHashMap<Instruction, Edges<I>>()
    initializeEdgesMap(edgesMap, initialInfo)
    edgesMap.put(getStartInstruction(traversalOrder), Edges(initialInfo, initialInfo))

    val changed = BooleanArray(1)
    changed[0] = true
    while (changed[0]) {
        changed[0] = false
        collectDataFromSubgraph(
                traversalOrder, mergeDataWithLocalDeclarations, edgesMap,
                mergeEdges, updateEdge, Collections.emptyList<Instruction>(), changed, false)
    }
    return edgesMap
}

private fun <I> Pseudocode.initializeEdgesMap(
        edgesMap: MutableMap<Instruction, Edges<I>>,
        initialInfo: I
) {
    val instructions = instructions
    val initialEdge = Edges(initialInfo, initialInfo)
    for (instruction in instructions) {
        edgesMap.put(instruction, initialEdge)
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.body.initializeEdgesMap(edgesMap, initialInfo)
        }
    }
}

private fun <I : ControlFlowInfo<*>> Pseudocode.collectDataFromSubgraph(
        traversalOrder: TraversalOrder,
        mergeDataWithLocalDeclarations: Boolean,
        edgesMap: MutableMap<Instruction, Edges<I>>,
        mergeEdges: (Instruction, Collection<I>) -> Edges<I>,
        updateEdge: (Instruction, Instruction, I) -> I,
        previousSubGraphInstructions: Collection<Instruction>,
        changed: BooleanArray,
        isLocal: Boolean
) {
    val instructions = getInstructions(traversalOrder)
    val startInstruction = getStartInstruction(traversalOrder)

    for (instruction in instructions) {
        val isStart = instruction.isStartInstruction(traversalOrder)
        if (!isLocal && isStart)
            continue

        fun getPreviousIncludingSubGraphInstructions(): Collection<Instruction> {
            val previous = instruction.getPreviousInstructions(traversalOrder)
            if (instruction != startInstruction || previousSubGraphInstructions.isEmpty()) {
                return previous
            }
            val result = ArrayList(previous)
            result.addAll(previousSubGraphInstructions)
            return result
        }
        val previousInstructions = getPreviousIncludingSubGraphInstructions()

        fun updateEdgeDataForInstruction(
                previousValue: Edges<I>?,
                newValue: Edges<I>?
        ) {
            if (previousValue != newValue && newValue != null) {
                changed[0] = true
                edgesMap.put(instruction, newValue)
            }
        }

        if (instruction is LocalFunctionDeclarationInstruction) {
            val subroutinePseudocode = instruction.body
            val previous = if (mergeDataWithLocalDeclarations) previousInstructions else Collections.emptyList()
            subroutinePseudocode.collectDataFromSubgraph(
                    traversalOrder, mergeDataWithLocalDeclarations,
                    edgesMap, mergeEdges, updateEdge, previous, changed, true)
            if (mergeDataWithLocalDeclarations) {
                val lastInstruction = subroutinePseudocode.getLastInstruction(traversalOrder)
                val previousValue = edgesMap.get(instruction)
                val newValue = edgesMap.get(lastInstruction)
                val updatedValue =
                        if (newValue == null)
                            null
                        else
                            Edges(updateEdge(lastInstruction, instruction, newValue.incoming),
                                  updateEdge(lastInstruction, instruction, newValue.outgoing))
                updateEdgeDataForInstruction(previousValue, updatedValue)
                continue
            }
        }
        val previousDataValue = edgesMap.get(instruction)

        val incomingEdgesData = HashSet<I>()

        for (previousInstruction in previousInstructions) {
            val previousData = edgesMap.get(previousInstruction)
            if (previousData != null) {
                incomingEdgesData.add(updateEdge(
                        previousInstruction, instruction, previousData.outgoing))
            }
        }
        val mergedData = mergeEdges(instruction, incomingEdgesData)
        updateEdgeDataForInstruction(previousDataValue, mergedData)
    }
}

data class Edges<T>(val incoming: T, val outgoing: T)

enum class TraverseInstructionResult {
    CONTINUE,
    SKIP,
    HALT
}

// returns false when interrupted by handler
fun traverseFollowingInstructions(
        rootInstruction: Instruction,
        visited: MutableSet<Instruction>,
        order: TraversalOrder,
        // true to continue traversal
        handler: ((Instruction) -> TraverseInstructionResult)?
): Boolean {
    val stack = ArrayDeque<Instruction>()
    stack.push(rootInstruction)

    while (!stack.isEmpty()) {
        val instruction = stack.pop()
        if (!visited.add(instruction)) continue
        when (handler?.let { it(instruction) } ?: TraverseInstructionResult.CONTINUE) {
            TraverseInstructionResult.CONTINUE -> instruction.getNextInstructions(order).forEach { stack.push(it) }
            TraverseInstructionResult.SKIP -> {}
            TraverseInstructionResult.HALT -> return false
        }
    }
    return true
}

enum class TraversalOrder {
    FORWARD,
    BACKWARD
}

fun Pseudocode.getStartInstruction(traversalOrder: TraversalOrder): Instruction =
        if (traversalOrder == FORWARD) enterInstruction else sinkInstruction

fun Pseudocode.getLastInstruction(traversalOrder: TraversalOrder): Instruction =
        if (traversalOrder == FORWARD) sinkInstruction else enterInstruction

fun Pseudocode.getInstructions(traversalOrder: TraversalOrder): List<Instruction> =
        if (traversalOrder == FORWARD) instructions else reversedInstructions

fun Instruction.getNextInstructions(traversalOrder: TraversalOrder): Collection<Instruction> =
        if (traversalOrder == FORWARD) nextInstructions else previousInstructions

fun Instruction.getPreviousInstructions(traversalOrder: TraversalOrder): Collection<Instruction> =
        if (traversalOrder == FORWARD) previousInstructions else nextInstructions

fun Instruction.isStartInstruction(traversalOrder: TraversalOrder): Boolean =
        if (traversalOrder == FORWARD) this is SubroutineEnterInstruction else this is SubroutineSinkInstruction
