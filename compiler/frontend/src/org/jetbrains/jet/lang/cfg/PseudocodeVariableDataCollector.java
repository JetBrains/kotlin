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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.pseudocode.Instruction;
import org.jetbrains.jet.lang.cfg.pseudocode.LocalFunctionDeclarationInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.*;

public class PseudocodeVariableDataCollector extends PseudocodeTraverser {
    private final BindingContext bindingContext;

    public PseudocodeVariableDataCollector(@NotNull BindingContext context) {
        bindingContext = context;
    }

    public <D> Map<Instruction, Edges<Map<VariableDescriptor, D>>> collectData(
            @NotNull Pseudocode pseudocode,
            @NotNull TraversalOrder traversalOrder,
            @NotNull InstructionDataMergeStrategy<Map<VariableDescriptor, D>> instructionDataMergeStrategy
    ) {
        Map<VariableDescriptor, D> initialDataValue = Collections.emptyMap();
        Map<Instruction, Edges<Map<VariableDescriptor, D>>> edgesMap = Maps.newLinkedHashMap();
        initializeEdgesMap(pseudocode, edgesMap, initialDataValue);
        edgesMap.put(getStartInstruction(pseudocode, traversalOrder), Edges.create(initialDataValue, initialDataValue));

        boolean[] changed = new boolean[1];
        changed[0] = true;
        while (changed[0]) {
            changed[0] = false;
            collectDataFromSubgraph(pseudocode, traversalOrder, LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS, edgesMap,
                                    instructionDataMergeStrategy, Collections.<Instruction>emptyList(), changed, false);
        }
        return edgesMap;
    }

    private static <M> void initializeEdgesMap(
            @NotNull Pseudocode pseudocode,
            @NotNull Map<Instruction, Edges<M>> edgesMap,
            @NotNull M initialDataValue
    ) {
        List<Instruction> instructions = pseudocode.getInstructions();
        Edges<M> initialEdge = Edges.create(initialDataValue, initialDataValue);
        for (Instruction instruction : instructions) {
            edgesMap.put(instruction, initialEdge);
            if (shouldLookInside(instruction, LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS)) {
                initializeEdgesMap(((LocalFunctionDeclarationInstruction) instruction).getBody(), edgesMap, initialDataValue);
            }
        }
    }

    private <D> void collectDataFromSubgraph(
            @NotNull Pseudocode pseudocode,
            @NotNull TraversalOrder traversalOrder,
            @NotNull LookInsideStrategy lookInside,
            @NotNull Map<Instruction, Edges<Map<VariableDescriptor, D>>> edgesMap,
            @NotNull InstructionDataMergeStrategy<Map<VariableDescriptor, D>> instructionDataMergeStrategy,
            @NotNull Collection<Instruction> previousSubGraphInstructions,
            boolean[] changed,
            boolean isLocal
    ) {
        List<Instruction> instructions = getInstructions(pseudocode, traversalOrder);
        Instruction startInstruction = getStartInstruction(pseudocode, traversalOrder);

        for (Instruction instruction : instructions) {
            boolean isStart = isStartInstruction(instruction, traversalOrder);
            if (!isLocal && isStart) continue;

            Collection<Instruction> allPreviousInstructions;
            Collection<Instruction> previousInstructions = getPreviousInstruction(instruction, traversalOrder);

            if (instruction == startInstruction && !previousSubGraphInstructions.isEmpty()) {
                allPreviousInstructions = Lists.newArrayList(previousInstructions);
                allPreviousInstructions.addAll(previousSubGraphInstructions);
            }
            else {
                allPreviousInstructions = previousInstructions;
            }

            if (shouldLookInside(instruction, lookInside)) {
                LocalFunctionDeclarationInstruction functionInstruction = (LocalFunctionDeclarationInstruction) instruction;
                Pseudocode subroutinePseudocode = functionInstruction.getBody();
                collectDataFromSubgraph(subroutinePseudocode, traversalOrder, lookInside, edgesMap, instructionDataMergeStrategy,
                                        previousInstructions,
                                        changed, true);
                Instruction lastInstruction = getLastInstruction(subroutinePseudocode, traversalOrder);
                Edges<Map<VariableDescriptor, D>> previousValue = edgesMap.get(instruction);
                Edges<Map<VariableDescriptor, D>> newValue = edgesMap.get(lastInstruction);
                if (!previousValue.equals(newValue)) {
                    changed[0] = true;
                    edgesMap.put(instruction, newValue);
                }
                continue;
            }
            Edges<Map<VariableDescriptor, D>> previousDataValue = edgesMap.get(instruction);

            Collection<Map<VariableDescriptor, D>> incomingEdgesData = Sets.newHashSet();

            for (Instruction previousInstruction : allPreviousInstructions) {
                Edges<Map<VariableDescriptor, D>> previousData = edgesMap.get(previousInstruction);
                if (previousData != null) {
                    incomingEdgesData.add(previousData.out);
                }
            }
            Edges<Map<VariableDescriptor, D>> mergedData = instructionDataMergeStrategy.execute(instruction, incomingEdgesData);
            if (!mergedData.equals(previousDataValue)) {
                changed[0] = true;
                edgesMap.put(instruction, mergedData);
            }
        }
    }
}
