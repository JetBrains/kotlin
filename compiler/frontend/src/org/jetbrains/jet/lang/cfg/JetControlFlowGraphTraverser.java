package org.jetbrains.jet.lang.cfg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.jet.lang.cfg.pseudocode.Instruction;
import org.jetbrains.jet.lang.cfg.pseudocode.LocalDeclarationInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.cfg.pseudocode.SubroutineEnterInstruction;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author svtk
 */
public class JetControlFlowGraphTraverser {
    public static <D> Map<Instruction, D> traverseInstructionGraphUntilFactsStabilization(
            Pseudocode pseudocode,
            InstructionsMergeStrategy<D> instructionsMergeStrategy,
            D initialDataValue,
            D initialDataValueForEnterInstruction,
            boolean straightDirection) {
        Map<Instruction, D> dataMap = Maps.newHashMap();
        initializeDataMap(dataMap, pseudocode, initialDataValue);
        dataMap.put(pseudocode.getEnterInstruction(), initialDataValueForEnterInstruction);

        boolean[] changed = new boolean[1];
        changed[0] = true;
        while (changed[0]) {
            changed[0] = false;
            traverseSubGraph(pseudocode, instructionsMergeStrategy, Collections.<Instruction>emptyList(), straightDirection, dataMap, changed, false);
        }
        return dataMap;
    }

    private static <D> void initializeDataMap(
            Map<Instruction, D> dataMap,
            Pseudocode pseudocode,
            D initialDataValue) {
        List<Instruction> instructions = pseudocode.getInstructions();
        for (Instruction instruction : instructions) {
            dataMap.put(instruction, initialDataValue);
            if (instruction instanceof LocalDeclarationInstruction) {
                initializeDataMap(dataMap, ((LocalDeclarationInstruction) instruction).getBody(), initialDataValue);
            }
        }
    }

    private static <D> void traverseSubGraph(
            Pseudocode pseudocode,
            InstructionsMergeStrategy<D> instructionsMergeStrategy,
            Collection<Instruction> previousSubGraphInstructions,
            boolean straightDirection,
            Map<Instruction, D> dataMap,
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

            if (instruction instanceof LocalDeclarationInstruction) {
                Pseudocode subroutinePseudocode = ((LocalDeclarationInstruction) instruction).getBody();
                traverseSubGraph(subroutinePseudocode, instructionsMergeStrategy, previousInstructions, straightDirection, dataMap, changed, true);
            }
            D previousDataValue = dataMap.get(instruction);

            Collection<D> incomingEdgesData = Sets.newHashSet();

            for (Instruction previousInstruction : allPreviousInstructions) {
                incomingEdgesData.add(dataMap.get(previousInstruction));
            }
            D mergedData = instructionsMergeStrategy.execute(instruction, incomingEdgesData);
            if (!mergedData.equals(previousDataValue)) {
                changed[0] = true;
                dataMap.put(instruction, mergedData);
            }
        }
    }

    public static void traverseAndAnalyzeInstructionGraph(
            Pseudocode pseudocode,
            InstructionDataAnalyzeStrategy instructionDataAnalyzeStrategy) {
        List<Instruction> instructions = pseudocode.getInstructions();
        for (Instruction instruction : instructions) {
            if (instruction instanceof LocalDeclarationInstruction) {
                traverseAndAnalyzeInstructionGraph(((LocalDeclarationInstruction) instruction).getBody(), instructionDataAnalyzeStrategy);
            }
            instructionDataAnalyzeStrategy.execute(instruction);
        }
    }
    
    interface InstructionsMergeStrategy<D> {
        D execute(Instruction instruction, Collection<D> incomingEdgesData);
    }

    interface InstructionDataAnalyzeStrategy {
        void execute(Instruction instruction);
    }
}
