package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.Pair;
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
public class JetControlFlowGraphTraverser<D> {
    private final Pseudocode pseudocode;
    private final boolean lookInside;
    private final Map<Instruction, Pair<D, D>> dataMap = Maps.newLinkedHashMap();

    public static <D> JetControlFlowGraphTraverser<D> create(Pseudocode pseudocode, boolean lookInside) {
        return new JetControlFlowGraphTraverser<D>(pseudocode, lookInside);
    }

    private JetControlFlowGraphTraverser(Pseudocode pseudocode, boolean lookInside) {
        this.pseudocode = pseudocode;
        this.lookInside = lookInside;
    }

    public void collectInformationFromInstructionGraph(
            InstructionDataMergeStrategy<D> instructionDataMergeStrategy,
            D initialDataValue,
            D initialDataValueForEnterInstruction,
            boolean straightDirection) {
        initializeDataMap(pseudocode, initialDataValue);
        dataMap.put(straightDirection ? pseudocode.getEnterInstruction() : pseudocode.getSinkInstruction(),
                    Pair.create(initialDataValueForEnterInstruction, initialDataValueForEnterInstruction));

        boolean[] changed = new boolean[1];
        changed[0] = true;
        while (changed[0]) {
            changed[0] = false;
            traverseSubGraph(pseudocode, instructionDataMergeStrategy, Collections.<Instruction>emptyList(), straightDirection, changed, false);
        }
    }

    private void initializeDataMap(
            Pseudocode pseudocode,
            D initialDataValue) {
        List<Instruction> instructions = pseudocode.getInstructions();
        Pair<D, D> initialPair = Pair.create(initialDataValue, initialDataValue);
        for (Instruction instruction : instructions) {
            dataMap.put(instruction, initialPair);
            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                initializeDataMap(((LocalDeclarationInstruction) instruction).getBody(), initialDataValue);
            }
        }
    }

    private void traverseSubGraph(
            Pseudocode pseudocode,
            InstructionDataMergeStrategy<D> instructionDataMergeStrategy,
            Collection<Instruction> previousSubGraphInstructions,
            boolean straightDirection,
            boolean[] changed,
            boolean isLocal) {
        List<Instruction> instructions = pseudocode.getInstructions();
        SubroutineEnterInstruction enterInstruction = pseudocode.getEnterInstruction();
        for (Instruction instruction : instructions) {
            if (!isLocal && instruction instanceof SubroutineEnterInstruction) continue;

            Collection<Instruction> allPreviousInstructions;
            Collection<Instruction> previousInstructions = straightDirection
                                                           ? instruction.getPreviousInstructions()
                                                           : instruction.getNextInstructions();

            if (instruction == enterInstruction && !previousSubGraphInstructions.isEmpty()) {
                allPreviousInstructions = Lists.newArrayList(previousInstructions);
                allPreviousInstructions.addAll(previousSubGraphInstructions);
            }
            else {
                allPreviousInstructions = previousInstructions;
            }

            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                Pseudocode subroutinePseudocode = ((LocalDeclarationInstruction) instruction).getBody();
                traverseSubGraph(subroutinePseudocode, instructionDataMergeStrategy, previousInstructions, straightDirection, changed, true);
            }
            Pair<D, D> previousDataValue = dataMap.get(instruction);

            Collection<D> incomingEdgesData = Sets.newHashSet();

            for (Instruction previousInstruction : allPreviousInstructions) {
                Pair<D, D> previousData = dataMap.get(previousInstruction);
                if (previousData != null) {
                    incomingEdgesData.add(previousData.getSecond());
                }
            }
            Pair<D, D> mergedData = instructionDataMergeStrategy.execute(instruction, incomingEdgesData);
            if (!mergedData.equals(previousDataValue)) {
                changed[0] = true;
                dataMap.put(instruction, mergedData);
            }
        }
    }

    public void traverseAndAnalyzeInstructionGraph(
            InstructionDataAnalyzeStrategy<D> instructionDataAnalyzeStrategy) {
        traverseAndAnalyzeInstructionGraph(pseudocode, instructionDataAnalyzeStrategy);
    }
    
    private void traverseAndAnalyzeInstructionGraph(
            Pseudocode pseudocode,
            InstructionDataAnalyzeStrategy<D> instructionDataAnalyzeStrategy) {
        List<Instruction> instructions = pseudocode.getInstructions();
        for (Instruction instruction : instructions) {
            if (instruction.isDead()) continue;
            Pair<D, D> pair = dataMap.get(instruction);
            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                traverseAndAnalyzeInstructionGraph(((LocalDeclarationInstruction) instruction).getBody(), instructionDataAnalyzeStrategy);
            }
            instructionDataAnalyzeStrategy.execute(instruction,
                                                   pair != null ? pair.getFirst() : null,
                                                   pair != null ? pair.getSecond() : null);
        }
    }
    
    public D getResultInfo() {
        return dataMap.get(pseudocode.getSinkInstruction()).getFirst();
    }
    
    interface InstructionDataMergeStrategy<D> {
        Pair<D, D> execute(Instruction instruction, @NotNull Collection<D> incomingEdgesData);
    }

    interface InstructionDataAnalyzeStrategy<D> {
        void execute(Instruction instruction, @Nullable D enterData, @Nullable D exitData);
    }
}
