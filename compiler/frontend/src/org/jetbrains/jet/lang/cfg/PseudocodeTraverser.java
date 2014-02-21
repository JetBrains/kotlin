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

package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.pseudocode.*;

import java.util.*;

import static org.jetbrains.jet.lang.cfg.PseudocodeTraverser.TraversalOrder.FORWARD;

public class PseudocodeTraverser {
    
    public static enum TraversalOrder {
        FORWARD,
        BACKWARD
    }
    
    @NotNull
    /*package*/ static Instruction getStartInstruction(@NotNull Pseudocode pseudocode, @NotNull TraversalOrder traversalOrder) {
        return traversalOrder == FORWARD ? pseudocode.getEnterInstruction() : pseudocode.getSinkInstruction();
    }

    @NotNull
    /*package*/ static Instruction getLastInstruction(@NotNull Pseudocode pseudocode, @NotNull TraversalOrder traversalOrder) {
        return traversalOrder == FORWARD ? pseudocode.getSinkInstruction() : pseudocode.getEnterInstruction();
    }

    @NotNull
    /*package*/ static List<Instruction> getInstructions(@NotNull Pseudocode pseudocode, @NotNull TraversalOrder traversalOrder) {
        return traversalOrder == FORWARD ? pseudocode.getInstructions() : pseudocode.getReversedInstructions();
    }

    @NotNull
    /*packge*/ static Collection<Instruction> getPreviousInstruction(@NotNull Instruction instruction, @NotNull TraversalOrder traversalOrder) {
        return traversalOrder == FORWARD ? instruction.getPreviousInstructions() : instruction.getNextInstructions();
    }

    /*package*/ static boolean isStartInstruction(@NotNull Instruction instruction, @NotNull TraversalOrder traversalOrder) {
        return traversalOrder == FORWARD ? instruction instanceof SubroutineEnterInstruction
                                         : instruction instanceof SubroutineSinkInstruction;
    }

    public static enum LookInsideStrategy {
        ANALYSE_LOCAL_DECLARATIONS,
        SKIP_LOCAL_DECLARATIONS
    }

    protected static boolean shouldLookInside(Instruction instruction, LookInsideStrategy lookInside) {
        return lookInside == LookInsideStrategy.ANALYSE_LOCAL_DECLARATIONS && instruction instanceof LocalFunctionDeclarationInstruction;
    }


    public static void traverse(
            @NotNull Pseudocode pseudocode,
            @NotNull TraversalOrder traversalOrder,
            @NotNull InstructionAnalyzeStrategy instructionAnalyzeStrategy
    ) {
        List<Instruction> instructions = getInstructions(pseudocode, traversalOrder);
        for (Instruction instruction : instructions) {
            if (instruction instanceof LocalFunctionDeclarationInstruction) {
                traverse(((LocalFunctionDeclarationInstruction) instruction).getBody(), traversalOrder, instructionAnalyzeStrategy);
            }
            instructionAnalyzeStrategy.execute(instruction);
        }
    }

    public static <D> void traverse(
            @NotNull Pseudocode pseudocode, TraversalOrder traversalOrder,
            @NotNull Map<Instruction, Edges<D>> edgesMap,
            @NotNull InstructionDataAnalyzeStrategy<D> instructionDataAnalyzeStrategy) {

        List<Instruction> instructions = getInstructions(pseudocode, traversalOrder);
        for (Instruction instruction : instructions) {
            if (instruction instanceof LocalFunctionDeclarationInstruction) {
                traverse(((LocalFunctionDeclarationInstruction) instruction).getBody(), traversalOrder, edgesMap,
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

    public interface InstructionHandler {
        // true to continue traversal
        boolean handle(@NotNull Instruction instruction);
    }

    // returns false when interrupted by handler
    public static boolean traverseFollowingInstructions(
            @NotNull Instruction rootInstruction,
            @NotNull Set<Instruction> visited,
            @NotNull TraversalOrder order,
            @Nullable InstructionHandler handler
    ) {
        Deque<Instruction> stack = Queues.newArrayDeque();
        stack.push(rootInstruction);

        while (!stack.isEmpty()) {
            Instruction instruction = stack.pop();
            visited.add(instruction);

            Collection<Instruction> followingInstructions =
                    order == FORWARD ? instruction.getNextInstructions() : instruction.getPreviousInstructions();

            for (Instruction followingInstruction : followingInstructions) {
                if (followingInstruction != null && !visited.contains(followingInstruction)) {
                    if (handler != null && !handler.handle(instruction)) return false;
                    stack.push(followingInstruction);
                }
            }
        }
        return true;
    }

}
