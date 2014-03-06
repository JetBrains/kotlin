/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocodeTraverser

import org.jetbrains.jet.lang.cfg.pseudocode.*
import java.util.*
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.TraversalOrder.FORWARD


enum class TraversalOrder {
    FORWARD
    BACKWARD
}

fun Pseudocode.getStartInstruction(traversalOrder: TraversalOrder): Instruction =
    if (traversalOrder == FORWARD) getEnterInstruction() else getSinkInstruction()

fun Pseudocode.getLastInstruction(traversalOrder: TraversalOrder): Instruction =
    if (traversalOrder == FORWARD) getSinkInstruction() else getEnterInstruction()

fun Pseudocode.getInstructions(traversalOrder: TraversalOrder): MutableList<Instruction> =
    if (traversalOrder == FORWARD) getInstructions() else getReversedInstructions()

fun Instruction.getNextInstructions(traversalOrder: TraversalOrder): Collection<Instruction> =
    if (traversalOrder == FORWARD) getNextInstructions() else getPreviousInstructions()

fun Instruction.getPreviousInstructions(traversalOrder: TraversalOrder): Collection<Instruction> =
    if (traversalOrder == FORWARD) getPreviousInstructions() else getNextInstructions()

fun Instruction.isStartInstruction(traversalOrder: TraversalOrder): Boolean =
    if (traversalOrder == FORWARD) this is SubroutineEnterInstruction else this is SubroutineSinkInstruction

enum class LookInsideStrategy {
    ANALYSE_LOCAL_DECLARATIONS
    SKIP_LOCAL_DECLARATIONS
}

fun Instruction.shouldLookInside(lookInside: LookInsideStrategy): Boolean =
    lookInside == LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS && this is LocalFunctionDeclarationInstruction


fun Pseudocode.traverse(
        traversalOrder: TraversalOrder,
        analyzeInstruction: (Instruction) -> Unit
) {
    val instructions = getInstructions(traversalOrder)
    for (instruction in instructions) {
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.getBody().traverse(traversalOrder, analyzeInstruction)
        }
        analyzeInstruction(instruction)
    }
}

fun <D> Pseudocode.traverse(
        traversalOrder: TraversalOrder,
        edgesMap: Map<Instruction, Edges<D>>,
        instructionDataAnalyzeStrategy: InstructionDataAnalyzeStrategy<D>
) {
    val instructions = getInstructions(traversalOrder)
    for (instruction in instructions) {
        if (instruction is LocalFunctionDeclarationInstruction) {
            instruction.getBody().traverse(traversalOrder, edgesMap, instructionDataAnalyzeStrategy)
        }
        val edges = edgesMap.get(instruction)
        if (edges != null) {
            instructionDataAnalyzeStrategy(instruction, edges.`in`, edges.out)
        }
    }
}

trait InstructionDataMergeStrategy<D> : (Instruction, Collection<D>) -> Edges<D>
trait InstructionDataAnalyzeStrategy<D> : (Instruction, D, D) -> Unit

data class Edges<T>(val `in`: T, val out: T)
fun <T> createEdges(`in`: T, out: T) = Edges(`in`, out)


// returns false when interrupted by handler
fun traverseFollowingInstructions(
        rootInstruction: Instruction,
        visited: MutableSet<Instruction>,
        order: TraversalOrder,
        // true to continue traversal
        handler: ((Instruction)->Boolean)?
): Boolean {
    val stack = ArrayDeque<Instruction>()
    stack.push(rootInstruction)

    while (!stack.isEmpty()) {
        val instruction = stack.pop()
        visited.add(instruction)

        val followingInstructions = instruction.getNextInstructions(order)

        for (followingInstruction in followingInstructions) {
            if (!visited.contains(followingInstruction)) {
                if (handler != null && !handler(instruction)) {
                    return false
                }
                stack.push(followingInstruction)
            }
        }
    }
    return true
}
