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
    private final boolean straightDirection;
    private final Map<Instruction, Pair<D, D>> dataMap = Maps.newLinkedHashMap();

    public static <D> JetControlFlowGraphTraverser<D> create(@NotNull Pseudocode pseudocode, boolean lookInside, boolean straightDirection) {
        return new JetControlFlowGraphTraverser<D>(pseudocode, lookInside, straightDirection);
    }

    private JetControlFlowGraphTraverser(@NotNull Pseudocode pseudocode, boolean lookInside, boolean straightDirection) {
        this.pseudocode = pseudocode;
        this.lookInside = lookInside;
        this.straightDirection = straightDirection;
    }

    @NotNull
    private Instruction getStartInstruction(@NotNull Pseudocode pseudocode) {
        return straightDirection ? pseudocode.getEnterInstruction() : pseudocode.getSinkInstruction();
    }

    public void collectInformationFromInstructionGraph(
            @NotNull D initialDataValue,
            @NotNull D initialDataValueForEnterInstruction,
            @NotNull InstructionDataMergeStrategy<D> instructionDataMergeStrategy) {
        initializeDataMap(pseudocode, initialDataValue);
        dataMap.put(getStartInstruction(pseudocode),
                    Pair.create(initialDataValueForEnterInstruction, initialDataValueForEnterInstruction));

        boolean[] changed = new boolean[1];
        changed[0] = true;
        while (changed[0]) {
            changed[0] = false;
            traverseSubGraph(pseudocode, instructionDataMergeStrategy, Collections.<Instruction>emptyList(), changed, false);
        }
    }

    private void initializeDataMap(
            @NotNull Pseudocode pseudocode,
            @NotNull D initialDataValue) {
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
            @NotNull Pseudocode pseudocode,
            @NotNull InstructionDataMergeStrategy<D> instructionDataMergeStrategy,
            @NotNull Collection<Instruction> previousSubGraphInstructions,
            boolean[] changed,
            boolean isLocal) {
        List<Instruction> instructions = pseudocode.getInstructions();
        Instruction startInstruction = getStartInstruction(pseudocode);

        if (!straightDirection) {
            instructions = Lists.newArrayList(instructions);
            Collections.reverse(instructions);
        }
        for (Instruction instruction : instructions) {
            boolean isStart = straightDirection ? instruction instanceof SubroutineEnterInstruction : instruction instanceof SubroutineSinkInstruction;
            if (!isLocal && isStart) continue;

            Collection<Instruction> allPreviousInstructions;
            Collection<Instruction> previousInstructions = straightDirection
                                                           ? instruction.getPreviousInstructions()
                                                           : instruction.getNextInstructions();

            if (instruction == startInstruction && !previousSubGraphInstructions.isEmpty()) {
                allPreviousInstructions = Lists.newArrayList(previousInstructions);
                allPreviousInstructions.addAll(previousSubGraphInstructions);
            }
            else {
                allPreviousInstructions = previousInstructions;
            }

            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                Pseudocode subroutinePseudocode = ((LocalDeclarationInstruction) instruction).getBody();
                traverseSubGraph(subroutinePseudocode, instructionDataMergeStrategy, previousInstructions, changed, true);
                Instruction lastInstruction = straightDirection ? subroutinePseudocode.getSinkInstruction() : subroutinePseudocode.getEnterInstruction();
                Pair<D, D> previousValue = dataMap.get(instruction);
                Pair<D, D> newValue = dataMap.get(lastInstruction);
                if (!previousValue.equals(newValue)) {
                    changed[0] = true;
                    dataMap.put(instruction, newValue);
                }
                continue;
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
            @NotNull InstructionDataAnalyzeStrategy<D> instructionDataAnalyzeStrategy) {
        traverseAndAnalyzeInstructionGraph(pseudocode, instructionDataAnalyzeStrategy);
    }
    
    private void traverseAndAnalyzeInstructionGraph(
            @NotNull Pseudocode pseudocode,
            @NotNull InstructionDataAnalyzeStrategy<D> instructionDataAnalyzeStrategy) {
        List<Instruction> instructions = pseudocode.getInstructions();
        if (!straightDirection) {
            instructions = Lists.newArrayList(instructions);
            Collections.reverse(instructions);
        }
        for (Instruction instruction : instructions) {
            if (instruction.isDead()) continue;
            if (lookInside && instruction instanceof LocalDeclarationInstruction) {
                traverseAndAnalyzeInstructionGraph(((LocalDeclarationInstruction) instruction).getBody(), instructionDataAnalyzeStrategy);
            }
            Pair<D, D> pair = dataMap.get(instruction);
            instructionDataAnalyzeStrategy.execute(instruction,
                                                   pair != null ? pair.getFirst() : null,
                                                   pair != null ? pair.getSecond() : null);
        }
    }

    public D getResultInfo() {
        return dataMap.get(pseudocode.getSinkInstruction()).getFirst();
    }
    
    interface InstructionDataMergeStrategy<D> {
        Pair<D, D> execute(@NotNull Instruction instruction, @NotNull Collection<D> incomingEdgesData);
    }

    interface InstructionDataAnalyzeStrategy<D> {
        void execute(@NotNull Instruction instruction, @Nullable D enterData, @Nullable D exitData);
    }
}
