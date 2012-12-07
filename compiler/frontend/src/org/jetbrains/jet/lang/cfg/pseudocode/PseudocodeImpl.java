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

package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.cfg.LoopInfo;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.*;

/**
* @author abreslav
* @author svtk
*/
public class PseudocodeImpl implements Pseudocode {

    public class PseudocodeLabel implements Label {
        private final String name;
        private final boolean allowDead;
        private Integer targetInstructionIndex;


        private PseudocodeLabel(String name, boolean allowDead) {
            this.name = name;
            this.allowDead = allowDead;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean allowDead() {
            return allowDead;
        }

        @Override
        public String toString() {
            return name;
        }

        public Integer getTargetInstructionIndex() {
            return targetInstructionIndex;
        }

        public void setTargetInstructionIndex(int targetInstructionIndex) {
            this.targetInstructionIndex = targetInstructionIndex;
        }

        @Nullable
        private List<Instruction> resolve() {
            assert targetInstructionIndex != null;
            return mutableInstructionList.subList(getTargetInstructionIndex(), mutableInstructionList.size());
        }

        public Instruction resolveToInstruction() {
            assert targetInstructionIndex != null;
            return mutableInstructionList.get(targetInstructionIndex);
        }

    }

    private final List<Instruction> mutableInstructionList = new ArrayList<Instruction>();
    private final List<Instruction> instructions = new ArrayList<Instruction>();
    private List<Instruction> reversedInstructions = null;
    private List<Instruction> deadInstructions;

    private Set<LocalDeclarationInstruction> localDeclarations = null;
    //todo getters
    private final Map<JetElement, Instruction> representativeInstructions = new HashMap<JetElement, Instruction>();
    private final Map<JetExpression, LoopInfo> loopInfo = Maps.newHashMap();
    
    private final List<PseudocodeLabel> labels = new ArrayList<PseudocodeLabel>();
    private final Map<PseudocodeLabel, Collection<Label>> stopAllowDeadLabels = Maps.newHashMap();

    private final JetElement correspondingElement;
    private SubroutineExitInstruction exitInstruction;
    private SubroutineSinkInstruction sinkInstruction;
    private SubroutineExitInstruction errorInstruction;
    private boolean postPrecessed = false;

    public PseudocodeImpl(JetElement correspondingElement) {
        this.correspondingElement = correspondingElement;
    }

    @NotNull
    @Override
    public JetElement getCorrespondingElement() {
        return correspondingElement;
    }

    @NotNull
    @Override
    public Set<LocalDeclarationInstruction> getLocalDeclarations() {
        if (localDeclarations == null) {
            localDeclarations = getLocalDeclarations(this);
        }
        return localDeclarations;
    }

    @NotNull
    private static Set<LocalDeclarationInstruction> getLocalDeclarations(@NotNull Pseudocode pseudocode) {
        Set<LocalDeclarationInstruction> localDeclarations = Sets.newLinkedHashSet();
        for (Instruction instruction : pseudocode.getInstructions()) {
            if (instruction instanceof LocalDeclarationInstruction) {
                localDeclarations.add((LocalDeclarationInstruction) instruction);
                localDeclarations.addAll(getLocalDeclarations(((LocalDeclarationInstruction)instruction).getBody()));
            }
        }
        return localDeclarations;
    }

    /*package*/ PseudocodeLabel createLabel(String name) {
        return createLabel(name, false);
    }

    private PseudocodeLabel createLabel(String name, boolean allowDead) {
        PseudocodeLabel label = new PseudocodeLabel(name, allowDead);
        labels.add(label);
        return label;
    }
    
    /*package*/ Label createAllowDeadLabel(String name) {
        return createLabel(name, true);
    }
    
    /*package*/ void stopAllowDead(Label label, Collection<Label> allowDeadLabels) {
        stopAllowDeadLabels.put((PseudocodeLabel) label, allowDeadLabels);
    }

    @Override
    @NotNull
    public List<Instruction> getInstructions() {
        return instructions;
    }

    @NotNull
    @Override
    public List<Instruction> getReversedInstructions() {
        if (reversedInstructions == null) {
            LinkedHashSet<Instruction> traversedInstructions = Sets.newLinkedHashSet();
            traverseInstructionsInReverseOrder(sinkInstruction, traversedInstructions);
            if (traversedInstructions.size() < instructions.size()) {
                List<Instruction> simplyReversedInstructions = Lists.newArrayList(instructions);
                Collections.reverse(simplyReversedInstructions);
                for (Instruction instruction : simplyReversedInstructions) {
                    if (!traversedInstructions.contains(instruction)) {
                        traverseInstructionsInReverseOrder(instruction, traversedInstructions);
                    }
                }
            }
            reversedInstructions = Lists.newArrayList(traversedInstructions);
        }
        return reversedInstructions;
    }

    private static void traverseInstructionsInReverseOrder(@NotNull Instruction instruction,
            @NotNull LinkedHashSet<Instruction> instructions) {
        if (instructions.contains(instruction)) return;
        instructions.add(instruction);
        for (Instruction previousInstruction : instruction.getPreviousInstructions()) {
            traverseInstructionsInReverseOrder(previousInstruction, instructions);
        }
    }


    //for tests only
    @NotNull
    public List<Instruction> getAllInstructions() {
        return mutableInstructionList;
    }

    @Override
    @NotNull
    public List<Instruction> getDeadInstructions() {
        if (deadInstructions != null) {
            return deadInstructions;
        }
        deadInstructions = Lists.newArrayList();
        Collection<Instruction> allowedDeadInstructions = collectAllowedDeadInstructions();

        for (Instruction instruction : mutableInstructionList) {
            if (((InstructionImpl)instruction).isDead()) {
                if (!allowedDeadInstructions.contains(instruction)) {
                    deadInstructions.add(instruction);
                }
            }
        }
        return deadInstructions;
    }

    //for tests only
    @NotNull
    public List<PseudocodeLabel> getLabels() {
        return labels;
    }

    /*package*/ void addExitInstruction(SubroutineExitInstruction exitInstruction) {
        addInstruction(exitInstruction);
        assert this.exitInstruction == null;
        this.exitInstruction = exitInstruction;
    }
    
    /*package*/ void addSinkInstruction(SubroutineSinkInstruction sinkInstruction) {
        addInstruction(sinkInstruction);
        assert this.sinkInstruction == null;
        this.sinkInstruction = sinkInstruction;
    }

    /*package*/ void addErrorInstruction(SubroutineExitInstruction errorInstruction) {
        addInstruction(errorInstruction);
        assert this.errorInstruction == null;
        this.errorInstruction = errorInstruction;
    }

    /*package*/ void addInstruction(Instruction instruction) {
        mutableInstructionList.add(instruction);
        instruction.setOwner(this);

        if (instruction instanceof JetElementInstruction) {
            JetElementInstruction elementInstruction = (JetElementInstruction) instruction;
            representativeInstructions.put(elementInstruction.getElement(), instruction);
        }
    }

    /*package*/ void recordLoopInfo(JetExpression expression, LoopInfo blockInfo) {
        loopInfo.put(expression, blockInfo);
    }

    @Override
    @NotNull
    public SubroutineExitInstruction getExitInstruction() {
        return exitInstruction;
    }

    @Override
    @NotNull
    public SubroutineSinkInstruction getSinkInstruction() {
        return sinkInstruction;
    }

    @Override
    @NotNull
    public SubroutineEnterInstruction getEnterInstruction() {
        return (SubroutineEnterInstruction) mutableInstructionList.get(0);
    }

    /*package*/ void bindLabel(Label label) {
        ((PseudocodeLabel) label).setTargetInstructionIndex(mutableInstructionList.size());
    }

    public void postProcess() {
        if (postPrecessed) return;
        postPrecessed = true;
        errorInstruction.setSink(getSinkInstruction());
        exitInstruction.setSink(getSinkInstruction());
        for (int i = 0, instructionsSize = mutableInstructionList.size(); i < instructionsSize; i++) {
            processInstruction(mutableInstructionList.get(i), i);
        }
        Set<Instruction> reachableInstructions = collectReachableInstructions();
        for (Instruction instruction : mutableInstructionList) {
            if (reachableInstructions.contains(instruction)) {
                instructions.add(instruction);
            }
        }
        markDeadInstructions();
    }

    private void processInstruction(Instruction instruction, final int currentPosition) {
        instruction.accept(new InstructionVisitor() {
            @Override
            public void visitInstructionWithNext(InstructionWithNext instruction) {
                instruction.setNext(getNextPosition(currentPosition));
            }

            @Override
            public void visitJump(AbstractJumpInstruction instruction) {
                instruction.setResolvedTarget(getJumpTarget(instruction.getTargetLabel()));
            }

            @Override
            public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                instruction.setNext(getNextPosition(currentPosition));
                List<Label> targetLabels = instruction.getTargetLabels();
                for (Label targetLabel : targetLabels) {
                    instruction.setResolvedTarget(targetLabel, getJumpTarget(targetLabel));
                }
            }

            @Override
            public void visitConditionalJump(ConditionalJumpInstruction instruction) {
                Instruction nextInstruction = getNextPosition(currentPosition);
                Instruction jumpTarget = getJumpTarget(instruction.getTargetLabel());
                if (instruction.onTrue()) {
                    instruction.setNextOnFalse(nextInstruction);
                    instruction.setNextOnTrue(jumpTarget);
                }
                else {
                    instruction.setNextOnFalse(jumpTarget);
                    instruction.setNextOnTrue(nextInstruction);
                }
                visitJump(instruction);
            }

            @Override
            public void visitLocalDeclarationInstruction(LocalDeclarationInstruction instruction) {
                ((PseudocodeImpl)instruction.getBody()).postProcess();
                instruction.setNext(getSinkInstruction());
            }

            @Override
            public void visitSubroutineExit(SubroutineExitInstruction instruction) {
                // Nothing
            }

            @Override
            public void visitSubroutineSink(SubroutineSinkInstruction instruction) {
                // Nothing
            }

            @Override
            public void visitInstruction(Instruction instruction) {
                throw new UnsupportedOperationException(instruction.toString());
            }
        });
    }

    private Set<Instruction> collectReachableInstructions() {
        Set<Instruction> visited = Sets.newHashSet();
        traverseNextInstructions(getEnterInstruction(), visited);
        if (!visited.contains(getExitInstruction())) {
            visited.add(getExitInstruction());
        }
        if (!visited.contains(errorInstruction)) {
            visited.add(errorInstruction);
        }
        if (!visited.contains(getSinkInstruction())) {
            visited.add(getSinkInstruction());
        }
        return visited;
    }
    
    private void traverseNextInstructions(@NotNull Instruction instruction, @NotNull Set<Instruction> visited) {
        if (visited.contains(instruction)) return;
        visited.add(instruction);
        for (Instruction nextInstruction : instruction.getNextInstructions()) {
            if (nextInstruction == null) continue; //todo it might be null on incomplete code
            traverseNextInstructions(nextInstruction, visited);
        }
    }

    private void markDeadInstructions() {
        Set<Instruction> instructionSet = Sets.newHashSet(instructions);
        for (Instruction instruction : mutableInstructionList) {
            if (!instructionSet.contains(instruction)) {
                ((InstructionImpl)instruction).die();
                for (Instruction nextInstruction : instruction.getNextInstructions()) {
                    nextInstruction.getPreviousInstructions().remove(instruction);
                }
            }
        }
    }

    @NotNull
    private Map<Instruction, Label> prepareAllowDeadStarters() {
        Map<Instruction, Label> allowedDeadBeginners = Maps.newHashMap();
        for (PseudocodeLabel label : labels) {
            if (!label.allowDead()) continue;
            Instruction allowDeadPossibleBeginner = getJumpTarget(label);
            if (((InstructionImpl)allowDeadPossibleBeginner).isDead()) {
                allowedDeadBeginners.put(allowDeadPossibleBeginner, label);
            }
        }
        return allowedDeadBeginners;
    }
    
    @NotNull
    private Map<Instruction, Collection<Label>> prepareAllowDeadStoppers() {
        Map<Instruction, Collection<Label>> stopAllowedDeadInstructions = Maps.newHashMap();
        for (Map.Entry<PseudocodeLabel, Collection<Label>> entry : stopAllowDeadLabels.entrySet()) {
            PseudocodeLabel stopAllowedDeadLabel = entry.getKey();
            Instruction stopAllowDeadInsruction = getJumpTarget(stopAllowedDeadLabel);
            if (((InstructionImpl)stopAllowDeadInsruction).isDead()) {
                stopAllowedDeadInstructions.put(stopAllowDeadInsruction, entry.getValue());
            }
        }
        return stopAllowedDeadInstructions;
    }

    @NotNull
    private Collection<Instruction> collectAllowedDeadInstructions() {
        // Allow dead instruction block can have several starters and
        // one stopper that knows all corresponding starters by labels
        Map<Instruction, Label> allowDeadStarters = prepareAllowDeadStarters();
        Map<Instruction, Collection<Label>> allowDeadStoppers = prepareAllowDeadStoppers();
        Set<Instruction> allowedDeadInstructions = Sets.newHashSet();

        for (Map.Entry<Instruction, Label> entry : allowDeadStarters.entrySet()) {
            Instruction starter = entry.getKey();

            Set<Instruction> allowedDeadFromThisInstruction = Sets.newHashSet();
            Label starterLabel = entry.getValue();
            collectAllowedDeadInstructions(starter, starterLabel, allowedDeadFromThisInstruction, allowDeadStoppers);
            allowedDeadInstructions.addAll(allowedDeadFromThisInstruction);
        }
        return allowedDeadInstructions;
    }
    
    private static void collectAllowedDeadInstructions(
            @NotNull Instruction current,
            @NotNull Label starterLabel,
            @NotNull Set<Instruction> collectedDeadInstructions,
            @NotNull Map<Instruction, Collection<Label>> allowDeadStoppers
    ) {

        if (collectedDeadInstructions.contains(current)) return;
        Collection<Label> allowDeadLabels = allowDeadStoppers.get(current);
        boolean isStopper = allowDeadLabels != null && allowDeadLabels.contains(starterLabel);
        if (isStopper) return;

        if (((InstructionImpl)current).isDead()) {
            collectedDeadInstructions.add(current);
            for (Instruction instruction : current.getNextInstructions()) {
                collectAllowedDeadInstructions(instruction, starterLabel, collectedDeadInstructions, allowDeadStoppers);
            }
        }
    }

    @NotNull
    private Instruction getJumpTarget(@NotNull Label targetLabel) {
        return ((PseudocodeLabel)targetLabel).resolveToInstruction();
    }

    @NotNull
    private Instruction getNextPosition(int currentPosition) {
        int targetPosition = currentPosition + 1;
        assert targetPosition < mutableInstructionList.size() : currentPosition;
        return mutableInstructionList.get(targetPosition);
    }
}
