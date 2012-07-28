/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author svtk
 */
public class PseudocodeTraverser {
    @NotNull
    private static Instruction getStartInstruction(@NotNull Pseudocode pseudocode, boolean directOrder) {
        return directOrder ? pseudocode.getEnterInstruction() : pseudocode.getSinkInstruction();
    }

    public static <D> Map<Instruction, Edges<D>> collectData(
            @NotNull Pseudocode pseudocode, boolean directOrder, boolean lookInside,
            @NotNull D initialDataValue, @NotNull D initialDataValueForEnterInstruction,
            @NotNull InstructionDataMergeStrategy<D> instructionDataMergeStrategy) {

        Map<Instruction, Edges<D>> edgesMap = Maps.newLinkedHashMap();
        initializeEdgesMap(pseudocode, lookInside, edgesMap, initialDataValue);
        edgesMap.put(getStartInstruction(pseudocode, directOrder), Edges.create(initialDataValueForEnterInstruction, initialDataValueForEnterInstruction));

        boolean[] changed = new boolean[1];
        changed[0] = true;
        while (changed[0]) {
            changed[0] = false;
            collectDataFromSubgraph(pseudocode, directOrder, lookInside, edgesMap, instructionDataMergeStrategy,
                                    Collections.<Instruction>emptyList(), changed, false);
        }
        return edgesMap;
    }

    private static <D> void initializeEdgesMap(
            @NotNull Pseudocode pseudocode, boolean lookInside,
            @NotNull Map<Instruction, Edges<D>> edgesMap,
            @NotNull D initialDataValue) {
        List<Instruction> instructions = pseudocode.getInstructions();
        Edges<D> initialEdge = Edges.create(initialDataValue, initialDataValue);
        for (Instruction instruction : instructions) {
            edgesMap.put(instruction, initialEdge);
            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                initializeEdgesMap(((LocalDeclarationInstruction) instruction).getBody(), lookInside, edgesMap, initialDataValue);
            }
        }
    }

    private static <D> void collectDataFromSubgraph(
            @NotNull Pseudocode pseudocode, boolean directOrder, boolean lookInside,
            @NotNull Map<Instruction, Edges<D>> edgesMap,
            @NotNull InstructionDataMergeStrategy<D> instructionDataMergeStrategy,
            @NotNull Collection<Instruction> previousSubGraphInstructions,
            boolean[] changed, boolean isLocal) {

        List<Instruction> instructions = directOrder ? pseudocode.getInstructions() : pseudocode.getReversedInstructions();
        Instruction startInstruction = getStartInstruction(pseudocode, directOrder);

        for (Instruction instruction : instructions) {
            boolean isStart = directOrder ? instruction instanceof SubroutineEnterInstruction : instruction instanceof SubroutineSinkInstruction;
            if (!isLocal && isStart) continue;

            Collection<Instruction> allPreviousInstructions;
            Collection<Instruction> previousInstructions = directOrder ? instruction.getPreviousInstructions() : instruction.getNextInstructions();

            if (instruction == startInstruction && !previousSubGraphInstructions.isEmpty()) {
                allPreviousInstructions = Lists.newArrayList(previousInstructions);
                allPreviousInstructions.addAll(previousSubGraphInstructions);
            }
            else {
                allPreviousInstructions = previousInstructions;
            }

            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                Pseudocode subroutinePseudocode = ((LocalDeclarationInstruction) instruction).getBody();
                collectDataFromSubgraph(subroutinePseudocode, directOrder, lookInside, edgesMap, instructionDataMergeStrategy,
                                        previousInstructions,
                                        changed, true);
                Instruction lastInstruction = directOrder ? subroutinePseudocode.getSinkInstruction() : subroutinePseudocode.getEnterInstruction();
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

    public static void traverse(
            @NotNull Pseudocode pseudocode, boolean directOrder,
            InstructionAnalyzeStrategy instructionAnalyzeStrategy) {

        List<Instruction> instructions = directOrder ? pseudocode.getInstructions() : pseudocode.getReversedInstructions();
        for (Instruction instruction : instructions) {
            if (instruction instanceof LocalDeclarationInstruction) {
                traverse(((LocalDeclarationInstruction) instruction).getBody(), directOrder, instructionAnalyzeStrategy);
            }
            instructionAnalyzeStrategy.execute(instruction);
        }
    }

    public static <D> void traverse(
            @NotNull Pseudocode pseudocode, boolean directOrder, boolean lookInside,
            @NotNull Map<Instruction, Edges<D>> edgesMap,
            @NotNull InstructionDataAnalyzeStrategy<D> instructionDataAnalyzeStrategy) {

        List<Instruction> instructions = directOrder ? pseudocode.getInstructions() : pseudocode.getReversedInstructions();
        for (Instruction instruction : instructions) {
            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                traverse(((LocalDeclarationInstruction) instruction).getBody(), directOrder, lookInside, edgesMap,
                         instructionDataAnalyzeStrategy);
            }
            Edges<D> edges = edgesMap.get(instruction);
            instructionDataAnalyzeStrategy.execute(instruction, edges != null ? edges.in : null, edges != null ? edges.out : null);
        }
    }

    public interface InstructionDataMergeStrategy<D> {
        Edges<D> execute(@NotNull Instruction instruction, @NotNull Collection<D> incomingEdgesData);
    }

    public interface InstructionDataAnalyzeStrategy<D> {
        void execute(@NotNull Instruction instruction, @Nullable D enterData, @Nullable D exitData);
    }

    public interface InstructionAnalyzeStrategy {
        void execute(@NotNull Instruction instruction);
    }

    public static class Edges<T> {
        public final T in;
        public final T out;

        Edges(@NotNull T in, @NotNull T out) {
            this.in = in;
            this.out = out;
        }

        public static <T> Edges<T> create(@NotNull T in, @NotNull T out) {
            return new Edges<T>(in, out);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Edges)) return false;

            Edges edges = (Edges) o;

            if (in != null ? !in.equals(edges.in) : edges.in != null) return false;
            if (out != null ? !out.equals(edges.out) : edges.out != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = in != null ? in.hashCode() : 0;
            result = 31 * result + (out != null ? out.hashCode() : 0);
            return result;
        }
    }
}
