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

package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.cfg.LoopInfo;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.*;

public class PseudocodeImpl implements Pseudocode {

    public class PseudocodeLabel implements Label {
        private final String name;
        private Integer targetInstructionIndex;


        private PseudocodeLabel(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
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

        public Label copy() {
            return new PseudocodeLabel("copy " + name);
        }
    }

    private final List<Instruction> mutableInstructionList = new ArrayList<Instruction>();
    private final List<Instruction> instructions = new ArrayList<Instruction>();

    private Set<LocalFunctionDeclarationInstruction> localDeclarations = null;
    //todo getters
    private final Map<JetElement, Instruction> representativeInstructions = new HashMap<JetElement, Instruction>();
    private final Map<JetExpression, LoopInfo> loopInfo = Maps.newHashMap();
    
    private final List<PseudocodeLabel> labels = new ArrayList<PseudocodeLabel>();

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
    public Set<LocalFunctionDeclarationInstruction> getLocalDeclarations() {
        if (localDeclarations == null) {
            localDeclarations = getLocalDeclarations(this);
        }
        return localDeclarations;
    }

    @NotNull
    private static Set<LocalFunctionDeclarationInstruction> getLocalDeclarations(@NotNull Pseudocode pseudocode) {
        Set<LocalFunctionDeclarationInstruction> localDeclarations = Sets.newLinkedHashSet();
        for (Instruction instruction : pseudocode.getInstructions()) {
            if (instruction instanceof LocalFunctionDeclarationInstruction) {
                localDeclarations.add((LocalFunctionDeclarationInstruction) instruction);
                localDeclarations.addAll(getLocalDeclarations(((LocalFunctionDeclarationInstruction)instruction).getBody()));
            }
        }
        return localDeclarations;
    }

    /*package*/ PseudocodeLabel createLabel(String name) {
        PseudocodeLabel label = new PseudocodeLabel(name);
        labels.add(label);
        return label;
    }
    
    @Override
    @NotNull
    public List<Instruction> getInstructions() {
        return instructions;
    }

    @NotNull
    @Override
    public List<Instruction> getReversedInstructions() {
        LinkedHashSet<Instruction> traversedInstructions = Sets.newLinkedHashSet();
        traverseFollowingInstructions(sinkInstruction, traversedInstructions, false);
        if (traversedInstructions.size() < instructions.size()) {
            List<Instruction> simplyReversedInstructions = Lists.newArrayList(instructions);
            Collections.reverse(simplyReversedInstructions);
            for (Instruction instruction : simplyReversedInstructions) {
                if (!traversedInstructions.contains(instruction)) {
                    traverseFollowingInstructions(instruction, traversedInstructions, false);
                }
            }
        }
        return Lists.newArrayList(traversedInstructions);
    }

    //for tests only
    @NotNull
    public List<Instruction> getAllInstructions() {
        return mutableInstructionList;
    }

    @Override
    @NotNull
    public List<Instruction> getDeadInstructions() {
        List<Instruction> deadInstructions = Lists.newArrayList();
        for (Instruction instruction : mutableInstructionList) {
            if (isDead(instruction)) {
                deadInstructions.add(instruction);
            }
        }
        return deadInstructions;
    }

    private static boolean isDead(@NotNull Instruction instruction) {
        if (!((InstructionImpl)instruction).isDead()) return false;
        for (Instruction copy : instruction.getCopies()) {
            if (!((InstructionImpl)copy).isDead()) return false;
        }
        return true;
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
            public void visitLocalFunctionDeclarationInstruction(LocalFunctionDeclarationInstruction instruction) {
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
        traverseFollowingInstructions(getEnterInstruction(), visited, true);
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
    
    private static void traverseFollowingInstructions(
            @NotNull Instruction rootInstruction,
            @NotNull Set<Instruction> visited,
            boolean directOrder
    ) {
        Deque<Instruction> stack = Queues.newArrayDeque();
        stack.push(rootInstruction);

        while (!stack.isEmpty()) {
            Instruction instruction = stack.pop();
            visited.add(instruction);

            Collection<Instruction> followingInstructions =
                    directOrder ? instruction.getNextInstructions() : instruction.getPreviousInstructions();

            for (Instruction followingInstruction : followingInstructions) {
                if (followingInstruction != null && !visited.contains(followingInstruction)) {
                    stack.push(followingInstruction);
                }
            }
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
    private Instruction getJumpTarget(@NotNull Label targetLabel) {
        return ((PseudocodeLabel)targetLabel).resolveToInstruction();
    }

    @NotNull
    private Instruction getNextPosition(int currentPosition) {
        int targetPosition = currentPosition + 1;
        assert targetPosition < mutableInstructionList.size() : currentPosition;
        return mutableInstructionList.get(targetPosition);
    }

    public void repeatPart(@NotNull Label startLabel, @NotNull Label finishLabel) {
        Integer startIndex = ((PseudocodeLabel) startLabel).getTargetInstructionIndex();
        assert startIndex != null;
        Integer finishIndex = ((PseudocodeLabel) finishLabel).getTargetInstructionIndex();
        assert finishIndex != null;

        Map<Label, Label> originalToCopy = Maps.newHashMap();
        Multimap<Instruction, Label> originalLabelsForInstruction = HashMultimap.create();
        for (PseudocodeLabel label : labels) {
            Integer index = label.getTargetInstructionIndex();
            if (index == null) continue; //label is not bounded yet
            if (label == startLabel || label == finishLabel) continue;

            if (startIndex <= index && index <= finishIndex) {
                originalToCopy.put(label, label.copy());
                originalLabelsForInstruction.put(getJumpTarget(label), label);
            }
        }
        for (int index = startIndex; index < finishIndex; index++) {
            Instruction originalInstruction = mutableInstructionList.get(index);
            repeatLabelsBindingForInstruction(originalInstruction, originalToCopy, originalLabelsForInstruction);
            addInstruction(copyInstruction(originalInstruction, originalToCopy));
        }
        repeatLabelsBindingForInstruction(mutableInstructionList.get(finishIndex), originalToCopy, originalLabelsForInstruction);
    }

    private void repeatLabelsBindingForInstruction(
            @NotNull Instruction originalInstruction,
            @NotNull Map<Label, Label> originalToCopy,
            @NotNull Multimap<Instruction, Label> originalLabelsForInstruction
    ) {
        for (Label originalLabel : originalLabelsForInstruction.get(originalInstruction)) {
            bindLabel(originalToCopy.get(originalLabel));
        }
    }

    private Instruction copyInstruction(@NotNull Instruction instruction, @NotNull Map<Label, Label> originalToCopy) {
        if (instruction instanceof AbstractJumpInstruction) {
            Label originalTarget = ((AbstractJumpInstruction) instruction).getTargetLabel();
            if (originalToCopy.containsKey(originalTarget)) {
                return ((AbstractJumpInstruction)instruction).copy(originalToCopy.get(originalTarget));
            }
        }
        if (instruction instanceof NondeterministicJumpInstruction) {
            List<Label> originalTargets = ((NondeterministicJumpInstruction) instruction).getTargetLabels();
            List<Label> copyTargets = copyLabels(originalTargets, originalToCopy);
            return ((NondeterministicJumpInstruction) instruction).copy(copyTargets);
        }
        return ((InstructionImpl)instruction).copy();
    }

    @NotNull
    private List<Label> copyLabels(Collection<Label> labels, Map<Label, Label> originalToCopy) {
        List<Label> newLabels = Lists.newArrayList();
        for (Label label : labels) {
            Label newLabel = originalToCopy.get(label);
            newLabels.add(newLabel != null ? newLabel : label);
        }
        return newLabels;
    }
}
