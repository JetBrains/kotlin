/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg

import org.jetbrains.jet.lang.cfg.pseudocode.Instruction
import org.jetbrains.jet.lang.cfg.pseudocode.LocalFunctionDeclarationInstruction
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.cfg.PseudocodeTraverser.*

import java.util.*

public class PseudocodeVariableDataCollector(
        private val bindingContext: BindingContext,
        private val pseudocode: Pseudocode
) : PseudocodeTraverser() {

    suppress("UNCHECKED_CAST")
    public fun <D> collectDataJ(
            traversalOrder: TraversalOrder,
            instructionDataMergeStrategy: InstructionDataMergeStrategy<MutableMap<VariableDescriptor, D>>
    ): MutableMap<Instruction, Edges<MutableMap<VariableDescriptor, D>>> {
        //see KT-4605
        return collectData(
                traversalOrder, instructionDataMergeStrategy as InstructionDataMergeStrategy<Map<VariableDescriptor, D>>
        ) as MutableMap<Instruction, Edges<MutableMap<VariableDescriptor, D>>>
    }

    public fun <D> collectData(
            traversalOrder: TraversalOrder,
            instructionDataMergeStrategy: InstructionDataMergeStrategy<Map<VariableDescriptor, D>>
    ): Map<Instruction, Edges<Map<VariableDescriptor, D>>> {
        val initialDataValue : Map<VariableDescriptor, D> = Collections.emptyMap<VariableDescriptor, D>()
        val edgesMap = LinkedHashMap<Instruction, Edges<Map<VariableDescriptor, D>>>()
        initializeEdgesMap(pseudocode, edgesMap, initialDataValue)
        edgesMap.put(getStartInstruction(pseudocode, traversalOrder),
                     Edges.create(initialDataValue, initialDataValue))

        val changed = BooleanArray(1)
        changed[0] = true
        while (changed[0]) {
            changed[0] = false
            collectDataFromSubgraph(
                    pseudocode, traversalOrder, LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS, edgesMap,
                    instructionDataMergeStrategy, Collections.emptyList<Instruction>(), changed, false)
        }
        return edgesMap
    }

    private fun <M> initializeEdgesMap(
            pseudocode: Pseudocode,
            edgesMap: MutableMap<Instruction, Edges<M>>,
            initialDataValue: M
    ) {
        val instructions = pseudocode.getInstructions()
        val initialEdge = Edges.create(initialDataValue, initialDataValue)
        for (instruction in instructions) {
            edgesMap.put(instruction, initialEdge)
            if (PseudocodeTraverser.shouldLookInside(instruction, LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS)) {
                initializeEdgesMap(((instruction as LocalFunctionDeclarationInstruction)).getBody(), edgesMap, initialDataValue)
            }
        }
    }

    private fun <D> collectDataFromSubgraph(
            pseudocode: Pseudocode,
            traversalOrder: TraversalOrder,
            lookInside: LookInsideStrategy,
            edgesMap: MutableMap<Instruction, Edges<Map<VariableDescriptor, D>>>,
            instructionDataMergeStrategy: InstructionDataMergeStrategy<Map<VariableDescriptor, D>>,
            previousSubGraphInstructions: Collection<Instruction>,
            changed: BooleanArray,
            isLocal: Boolean
    ) {
        val instructions = getInstructions(pseudocode, traversalOrder)
        val startInstruction = getStartInstruction(pseudocode, traversalOrder)

        for (instruction in instructions) {
            val isStart = isStartInstruction(instruction, traversalOrder)
            if (!isLocal && isStart)
                continue

            val allPreviousInstructions: MutableCollection<Instruction>
            val previousInstructions = getPreviousInstruction(instruction, traversalOrder)

            if (instruction == startInstruction && !previousSubGraphInstructions.isEmpty()) {
                allPreviousInstructions = ArrayList(previousInstructions)
                allPreviousInstructions.addAll(previousSubGraphInstructions)
            }
            else {
                allPreviousInstructions = previousInstructions
            }

            if (shouldLookInside(instruction, lookInside)) {
                val functionInstruction = (instruction as LocalFunctionDeclarationInstruction)
                val subroutinePseudocode = functionInstruction.getBody()
                collectDataFromSubgraph(
                        subroutinePseudocode, traversalOrder, lookInside, edgesMap, instructionDataMergeStrategy,
                        previousInstructions, changed, true)
                val lastInstruction = getLastInstruction(subroutinePseudocode, traversalOrder)
                val previousValue = edgesMap.get(instruction)
                val newValue = edgesMap.get(lastInstruction)
                if (previousValue != newValue && newValue != null) {
                    changed[0] = true
                    edgesMap.put(instruction, newValue)
                }
                continue
            }
            val previousDataValue = edgesMap.get(instruction)

            val incomingEdgesData = HashSet<Map<VariableDescriptor, D>>()

            for (previousInstruction in allPreviousInstructions) {
                val previousData = edgesMap.get(previousInstruction)
                if (previousData != null) {
                    incomingEdgesData.add(previousData.out)
                }
            }
            val mergedData = instructionDataMergeStrategy.execute(instruction, incomingEdgesData)
            if (!mergedData.equals(previousDataValue)) {
                changed[0] = true
                edgesMap.put(instruction, mergedData)
            }
        }
    }
}
