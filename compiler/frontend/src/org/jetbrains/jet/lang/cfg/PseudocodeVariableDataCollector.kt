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
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.*
import org.jetbrains.jet.lang.cfg.pseudocode.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.VariableDeclarationInstruction
import org.jetbrains.jet.utils.addToStdlib.*

import kotlin.properties.Delegates

import java.util.*

public class PseudocodeVariableDataCollector(
        private val bindingContext: BindingContext,
        private val pseudocode: Pseudocode
) {
    val lexicalScopeVariableInfo = computeLexicalScopeVariableInfo(pseudocode)

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
        edgesMap.put(pseudocode.getStartInstruction(traversalOrder), Edges(initialDataValue, initialDataValue))

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
        val initialEdge = Edges(initialDataValue, initialDataValue)
        for (instruction in instructions) {
            edgesMap.put(instruction, initialEdge)
            if (instruction.shouldLookInside(LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS)) {
                initializeEdgesMap((instruction as LocalFunctionDeclarationInstruction).getBody(), edgesMap, initialDataValue)
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
        val instructions = pseudocode.getInstructions(traversalOrder)
        val startInstruction = pseudocode.getStartInstruction(traversalOrder)

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
                    previousValue: Edges<Map<VariableDescriptor, D>>?,
                    newValue: Edges<Map<VariableDescriptor, D>>?
            ) {
                if (previousValue != newValue && newValue != null) {
                    changed[0] = true
                    edgesMap.put(instruction, newValue)
                }
            }

            if (instruction.shouldLookInside(lookInside)) {
                val functionInstruction = (instruction as LocalFunctionDeclarationInstruction)
                val subroutinePseudocode = functionInstruction.getBody()
                collectDataFromSubgraph(
                        subroutinePseudocode, traversalOrder, lookInside, edgesMap, instructionDataMergeStrategy,
                        previousInstructions, changed, true)
                val lastInstruction = subroutinePseudocode.getLastInstruction(traversalOrder)
                val previousValue = edgesMap.get(instruction)
                val newValue = edgesMap.get(lastInstruction)
                val updatedValue = if (newValue == null) null else
                    Edges(filterOutVariablesOutOfScope(lastInstruction, instruction, newValue.`in`),
                          filterOutVariablesOutOfScope(lastInstruction, instruction, newValue.out))
                updateEdgeDataForInstruction(previousValue, updatedValue)
                continue
            }
            val previousDataValue = edgesMap.get(instruction)

            val incomingEdgesData = HashSet<Map<VariableDescriptor, D>>()

            for (previousInstruction in previousInstructions) {
                val previousData = edgesMap.get(previousInstruction)
                if (previousData != null) {
                    incomingEdgesData.add(filterOutVariablesOutOfScope(
                            previousInstruction, instruction, previousData.out))
                }
            }
            val mergedData = instructionDataMergeStrategy(instruction, incomingEdgesData)
            updateEdgeDataForInstruction(previousDataValue, mergedData)
        }
    }

    private fun <D> filterOutVariablesOutOfScope(
            from: Instruction,
            to: Instruction,
            data: Map<VariableDescriptor, D>
    ): Map<VariableDescriptor, D> {
        // If an edge goes from deeper lexical scope to a less deep one, this means that it points outside of the deeper scope.
        val toDepth = to.getLexicalScope().depth
        if (toDepth >= from.getLexicalScope().depth) return data

        // Variables declared in an inner (deeper) scope can't be accessed from an outer scope.
        // Thus they can be filtered out upon leaving the inner scope.
        return data.filterKeys { variable ->
            val lexicalScope = lexicalScopeVariableInfo.declaredIn[variable]
            // '-1' for variables declared outside this pseudocode
            val depth = lexicalScope?.depth ?: -1
            depth <= toDepth
        }
    }

    fun computeLexicalScopeVariableInfo(pseudocode: Pseudocode): LexicalScopeVariableInfo {
        val lexicalScopeVariableInfo = LexicalScopeVariableInfoImpl()
        pseudocode.traverse(TraversalOrder.FORWARD, { instruction ->
            if (instruction is VariableDeclarationInstruction) {
                val variableDeclarationElement = instruction.getVariableDeclarationElement()
                val descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, variableDeclarationElement)
                assert(descriptor is VariableDescriptor,
                       "Variable descriptor should correspond to the instruction for ${instruction.getElement().getText()}.\n" +
                       "Descriptor : $descriptor")
                lexicalScopeVariableInfo.registerVariableDeclaredInScope(
                        descriptor as VariableDescriptor, instruction.getLexicalScope())
            }
        })
        return lexicalScopeVariableInfo
    }
}

public trait LexicalScopeVariableInfo {
    val declaredIn : Map<VariableDescriptor, LexicalScope>
    val scopeVariables : Map<LexicalScope, Collection<VariableDescriptor>>
}

public class LexicalScopeVariableInfoImpl : LexicalScopeVariableInfo {
    override val declaredIn = HashMap<VariableDescriptor, LexicalScope>()
    override val scopeVariables = HashMap<LexicalScope, MutableCollection<VariableDescriptor>>()

    fun registerVariableDeclaredInScope(variable: VariableDescriptor, lexicalScope: LexicalScope) {
        declaredIn[variable] = lexicalScope
        val variablesInScope = scopeVariables.getOrPut(lexicalScope, { ArrayList<VariableDescriptor>() })
        variablesInScope.add(variable)
    }
}
