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
import org.jetbrains.jet.lang.resolve.BindingContext;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class PseudocodeVariableDataCollector extends PseudocodeTraverser {
    private final BindingContext bindingContext;

    public PseudocodeVariableDataCollector(@NotNull BindingContext context) {
        bindingContext = context;
    }

    public <D> Map<Instruction, Edges<D>> collectData(
            @NotNull Pseudocode pseudocode,
            @NotNull TraversalOrder traversalOrder,
            @NotNull LookInsideStrategy lookInside,
            @NotNull D initialDataValue,
            @NotNull D initialDataValueForEnterInstruction,
            @NotNull InstructionDataMergeStrategy<D> instructionDataMergeStrategy
    ) {

        Map<Instruction, Edges<D>> edgesMap = Maps.newLinkedHashMap();
        initializeEdgesMap(pseudocode, lookInside, edgesMap, initialDataValue);
        edgesMap.put(getStartInstruction(pseudocode, traversalOrder), Edges.create(initialDataValueForEnterInstruction, initialDataValueForEnterInstruction));

        boolean[] changed = new boolean[1];
        changed[0] = true;
        while (changed[0]) {
            changed[0] = false;
            collectDataFromSubgraph(pseudocode, traversalOrder, lookInside, edgesMap, instructionDataMergeStrategy,
                                    Collections.<Instruction>emptyList(), changed, false);
        }
        return edgesMap;
    }

    private <D> void initializeEdgesMap(
            @NotNull Pseudocode pseudocode, LookInsideStrategy lookInside,
            @NotNull Map<Instruction, Edges<D>> edgesMap,
            @NotNull D initialDataValue) {
        List<Instruction> instructions = pseudocode.getInstructions();
        Edges<D> initialEdge = Edges.create(initialDataValue, initialDataValue);
        for (Instruction instruction : instructions) {
            edgesMap.put(instruction, initialEdge);
            if (shouldLookInside(instruction, lookInside)) {
                initializeEdgesMap(((LocalFunctionDeclarationInstruction) instruction).getBody(), lookInside, edgesMap, initialDataValue);
            }
        }
    }

    private <D> void collectDataFromSubgraph(
            @NotNull Pseudocode pseudocode,
            @NotNull TraversalOrder traversalOrder,
            @NotNull LookInsideStrategy lookInside,
            @NotNull Map<Instruction, Edges<D>> edgesMap,
            @NotNull InstructionDataMergeStrategy<D> instructionDataMergeStrategy,
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
                Edges<D> previousValue = edgesMap.get(instruction);
                Edges<D> newValue = edgesMap.get(lastInstruction);
                if (!previousValue.equals(newValue)) {
                    changed[0] = true;
                    edgesMap.put(instruction, newValue);
                }
                continue;
            }
            Edges<D> previousDataValue = edgesMap.get(instruction);

            Collection<D> incomingEdgesData = Sets.newHashSet();

            for (Instruction previousInstruction : allPreviousInstructions) {
                Edges<D> previousData = edgesMap.get(previousInstruction);
                if (previousData != null) {
                    incomingEdgesData.add(previousData.out);
                }
            }
            Edges<D> mergedData = instructionDataMergeStrategy.execute(instruction, incomingEdgesData);
            if (!mergedData.equals(previousDataValue)) {
                changed[0] = true;
                edgesMap.put(instruction, mergedData);
            }
        }
    }
}
