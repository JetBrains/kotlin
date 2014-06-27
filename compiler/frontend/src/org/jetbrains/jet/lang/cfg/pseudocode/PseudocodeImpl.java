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
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.cfg.LoopInfo;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.*;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.AbstractJumpInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.NondeterministicJumpInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.SubroutineEnterInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.SubroutineExitInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.SubroutineSinkInstruction;
import org.jetbrains.jet.lang.cfg.pseudocodeTraverser.PseudocodeTraverserPackage;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

import java.util.*;

import static org.jetbrains.jet.lang.cfg.pseudocodeTraverser.TraversalOrder.BACKWARD;
import static org.jetbrains.jet.lang.cfg.pseudocodeTraverser.TraversalOrder.FORWARD;

public class PseudocodeImpl implements Pseudocode {

    public class PseudocodeLabel implements Label {
        private final String name;
        private Integer targetInstructionIndex;


        private PseudocodeLabel(String name) {
            this.name = name;
        }

        @NotNull
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

    private final Map<JetElement, PseudoValue> elementsToValues = new HashMap<JetElement, PseudoValue>();

    private final NotNullLazyValue<Map<PseudoValue, List<? extends Instruction>>> valueUsages =
            new NotNullLazyValue<Map<PseudoValue, List<? extends Instruction>>>() {
                @NotNull
                @Override
                protected Map<PseudoValue, List<? extends Instruction>> compute() {
                    return PseudocodePackage.collectValueUsages(PseudocodeImpl.this);
                }
            };

    private Pseudocode parent = null;
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
        for (Instruction instruction : ((PseudocodeImpl)pseudocode).mutableInstructionList) {
            if (instruction instanceof LocalFunctionDeclarationInstruction) {
                localDeclarations.add((LocalFunctionDeclarationInstruction) instruction);
                localDeclarations.addAll(getLocalDeclarations(((LocalFunctionDeclarationInstruction)instruction).getBody()));
            }
        }
        return localDeclarations;
    }

    @Override
    @Nullable
    public Pseudocode getParent() {
        return parent;
    }

    private void setParent(Pseudocode parent) {
        this.parent = parent;
    }

    @NotNull
    public Pseudocode getRootPseudocode() {
        Pseudocode parent = getParent();
        while (parent != null) {
            if (parent.getParent() == null) return parent;
            parent = parent.getParent();
        }
        return this;
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
        PseudocodeTraverserPackage.traverseFollowingInstructions(sinkInstruction, traversedInstructions, BACKWARD, null);
        if (traversedInstructions.size() < instructions.size()) {
            List<Instruction> simplyReversedInstructions = Lists.newArrayList(instructions);
            Collections.reverse(simplyReversedInstructions);
            for (Instruction instruction : simplyReversedInstructions) {
                if (!traversedInstructions.contains(instruction)) {
                    PseudocodeTraverserPackage.traverseFollowingInstructions(instruction, traversedInstructions, BACKWARD, null);
                }
            }
        }
        return Lists.newArrayList(traversedInstructions);
    }

    @Override
    @NotNull
    public List<Instruction> getInstructionsIncludingDeadCode() {
        return mutableInstructionList;
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

    @Nullable
    @Override
    public PseudoValue getElementValue(@Nullable JetElement element) {
        return elementsToValues.get(element);
    }

    @NotNull
    @Override
    public List<? extends Instruction> getUsages(@Nullable PseudoValue value) {
        List<? extends Instruction> result = valueUsages.getValue().get(value);
        return result != null ? result : Collections.<Instruction>emptyList();
    }

    /*package*/ void bindElementToValue(@NotNull JetElement element, @NotNull PseudoValue value) {
        elementsToValues.put(element, value);
    }

    /*package*/ void bindLabel(Label label) {
        ((PseudocodeLabel) label).setTargetInstructionIndex(mutableInstructionList.size());
    }

    public void postProcess() {
        if (postPrecessed) return;
        postPrecessed = true;
        errorInstruction.setSink(getSinkInstruction());
        exitInstruction.setSink(getSinkInstruction());
        int index = 0;
        for (Instruction instruction : mutableInstructionList) {
            //recursively invokes 'postProcess' for local declarations
            processInstruction(instruction, index);
            index++;
        }
        if (getParent() != null) return;

        // Collecting reachable instructions should be done after processing all instructions
        // (including instructions in local declarations) to avoid being in incomplete state.
        collectAndCacheReachableInstructions();
        for (LocalFunctionDeclarationInstruction localFunctionDeclarationInstruction : getLocalDeclarations()) {
            ((PseudocodeImpl) localFunctionDeclarationInstruction.getBody()).collectAndCacheReachableInstructions();
        }
    }

    private void collectAndCacheReachableInstructions() {
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
                if (instruction.getOnTrue()) {
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
                PseudocodeImpl body = (PseudocodeImpl) instruction.getBody();
                body.setParent(PseudocodeImpl.this);
                body.postProcess();
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
        PseudocodeTraverserPackage.traverseFollowingInstructions(getEnterInstruction(), visited, FORWARD, null);
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

    private void markDeadInstructions() {
        Set<Instruction> instructionSet = Sets.newHashSet(instructions);
        for (Instruction instruction : mutableInstructionList) {
            if (!instructionSet.contains(instruction)) {
                ((InstructionImpl)instruction).setMarkedAsDead(true);
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
