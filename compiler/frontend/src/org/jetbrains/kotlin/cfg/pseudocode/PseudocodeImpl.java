/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.cfg.pseudocode;

import com.google.common.collect.*;
import com.intellij.util.containers.BidirectionalMap;
import kotlin.Function0;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.Label;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.*;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.eval.MergeInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.AbstractJumpInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.ConditionalJumpInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.jumps.NondeterministicJumpInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineEnterInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineExitInstruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.SubroutineSinkInstruction;
import org.jetbrains.kotlin.cfg.pseudocodeTraverser.PseudocodeTraverserPackage;
import org.jetbrains.kotlin.psi.JetElement;

import java.util.*;

import static org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.BACKWARD;
import static org.jetbrains.kotlin.cfg.pseudocodeTraverser.TraversalOrder.FORWARD;

public class PseudocodeImpl implements Pseudocode {

    public class PseudocodeLabel implements Label {
        private final String name;
        private final String comment;
        private Integer targetInstructionIndex;


        private PseudocodeLabel(@NotNull String name, @Nullable String comment) {
            this.name = name;
            this.comment = comment;
        }

        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return comment == null ? name : (name + " [" + comment + "]");
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

        public PseudocodeLabel copy(int newLabelIndex) {
            return new PseudocodeLabel("L" + newLabelIndex, "copy of " + name + ", " + comment);
        }

        public PseudocodeImpl getPseudocode() {
            return PseudocodeImpl.this;
        }
    }

    private final List<Instruction> mutableInstructionList = new ArrayList<Instruction>();
    private final List<Instruction> instructions = new ArrayList<Instruction>();

    private final BidirectionalMap<JetElement, PseudoValue> elementsToValues = new BidirectionalMap<JetElement, PseudoValue>();

    private final Map<PseudoValue, List<Instruction>> valueUsages = Maps.newHashMap();
    private final Map<PseudoValue, Set<PseudoValue>> mergedValues = Maps.newHashMap();
    private final Set<Instruction> sideEffectFree = Sets.newHashSet();

    private Pseudocode parent = null;
    private Set<LocalFunctionDeclarationInstruction> localDeclarations = null;
    //todo getters
    private final Map<JetElement, Instruction> representativeInstructions = new HashMap<JetElement, Instruction>();

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

    /*package*/ PseudocodeLabel createLabel(@NotNull String name, @Nullable String comment) {
        PseudocodeLabel label = new PseudocodeLabel(name, comment);
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

        if (instruction instanceof MergeInstruction) {
            addMergedValues((MergeInstruction) instruction);
        }

        for (PseudoValue inputValue : instruction.getInputValues()) {
            addValueUsage(inputValue, instruction);
            for (PseudoValue mergedValue : getMergedValues(inputValue)) {
                addValueUsage(mergedValue, instruction);
            }
        }
        if (PseudocodePackage.calcSideEffectFree(instruction)) {
            sideEffectFree.add(instruction);
        }
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
    public List<? extends JetElement> getValueElements(@Nullable PseudoValue value) {
        List<? extends JetElement> result = elementsToValues.getKeysByValue(value);
        return result != null ? result : Collections.<JetElement>emptyList();
    }

    @NotNull
    @Override
    public List<? extends Instruction> getUsages(@Nullable PseudoValue value) {
        List<? extends Instruction> result = valueUsages.get(value);
        return result != null ? result : Collections.<Instruction>emptyList();
    }

    @Override
    public boolean isSideEffectFree(@NotNull Instruction instruction) {
        return sideEffectFree.contains(instruction);
    }

    /*package*/ void bindElementToValue(@NotNull JetElement element, @NotNull PseudoValue value) {
        elementsToValues.put(element, value);
    }

    /*package*/ void bindLabel(Label label) {
        ((PseudocodeLabel) label).setTargetInstructionIndex(mutableInstructionList.size());
    }
    
    private Set<PseudoValue> getMergedValues(@NotNull PseudoValue value) {
        Set<PseudoValue> result = mergedValues.get(value);
        return result != null ? result : Collections.<PseudoValue>emptySet();
    }
    
    private void addMergedValues(@NotNull MergeInstruction instruction) {
        Set<PseudoValue> result = new LinkedHashSet<PseudoValue>();
        for (PseudoValue value : instruction.getInputValues()) {
            result.addAll(getMergedValues(value));
            result.add(value);
        }
        mergedValues.put(instruction.getOutputValue(), result);
    }

    private void addValueUsage(PseudoValue value, Instruction usage) {
        if (usage instanceof MergeInstruction) return;
        KotlinPackage.getOrPut(
                valueUsages,
                value,
                new Function0<List<Instruction>>() {
                    @Override
                    public List<Instruction> invoke() {
                        return Lists.newArrayList();
                    }
                }
        ).add(usage);
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
            public void visitInstructionWithNext(@NotNull InstructionWithNext instruction) {
                instruction.setNext(getNextPosition(currentPosition));
            }

            @Override
            public void visitJump(@NotNull AbstractJumpInstruction instruction) {
                instruction.setResolvedTarget(getJumpTarget(instruction.getTargetLabel()));
            }

            @Override
            public void visitNondeterministicJump(@NotNull NondeterministicJumpInstruction instruction) {
                instruction.setNext(getNextPosition(currentPosition));
                List<Label> targetLabels = instruction.getTargetLabels();
                for (Label targetLabel : targetLabels) {
                    instruction.setResolvedTarget(targetLabel, getJumpTarget(targetLabel));
                }
            }

            @Override
            public void visitConditionalJump(@NotNull ConditionalJumpInstruction instruction) {
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
            public void visitLocalFunctionDeclarationInstruction(@NotNull LocalFunctionDeclarationInstruction instruction) {
                PseudocodeImpl body = (PseudocodeImpl) instruction.getBody();
                body.setParent(PseudocodeImpl.this);
                body.postProcess();
                instruction.setNext(getSinkInstruction());
            }

            @Override
            public void visitSubroutineExit(@NotNull SubroutineExitInstruction instruction) {
                // Nothing
            }

            @Override
            public void visitSubroutineSink(@NotNull SubroutineSinkInstruction instruction) {
                // Nothing
            }

            @Override
            public void visitInstruction(@NotNull Instruction instruction) {
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

    public int repeatPart(@NotNull Label startLabel, @NotNull Label finishLabel, int labelCount) {
        PseudocodeImpl originalPseudocode = ((PseudocodeLabel) startLabel).getPseudocode();

        Integer startIndex = ((PseudocodeLabel) startLabel).getTargetInstructionIndex();
        assert startIndex != null;
        Integer finishIndex = ((PseudocodeLabel) finishLabel).getTargetInstructionIndex();
        assert finishIndex != null;

        Map<Label, Label> originalToCopy = Maps.newLinkedHashMap();
        Multimap<Instruction, Label> originalLabelsForInstruction = HashMultimap.create();
        for (PseudocodeLabel label : originalPseudocode.labels) {
            Integer index = label.getTargetInstructionIndex();
            if (index == null) continue; //label is not bounded yet
            if (label == startLabel || label == finishLabel) continue;

            if (startIndex <= index && index <= finishIndex) {
                originalToCopy.put(label, label.copy(labelCount++));
                originalLabelsForInstruction.put(getJumpTarget(label), label);
            }
        }
        for (Label label : originalToCopy.values()) {
            labels.add((PseudocodeLabel) label);
        }
        for (int index = startIndex; index < finishIndex; index++) {
            Instruction originalInstruction = originalPseudocode.mutableInstructionList.get(index);
            repeatLabelsBindingForInstruction(originalInstruction, originalToCopy, originalLabelsForInstruction);
            addInstruction(copyInstruction(originalInstruction, originalToCopy));
        }
        repeatLabelsBindingForInstruction(originalPseudocode.mutableInstructionList.get(finishIndex),
                                          originalToCopy,
                                          originalLabelsForInstruction);
        return labelCount;
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

    private static Instruction copyInstruction(@NotNull Instruction instruction, @NotNull Map<Label, Label> originalToCopy) {
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
    private static List<Label> copyLabels(Collection<Label> labels, Map<Label, Label> originalToCopy) {
        List<Label> newLabels = Lists.newArrayList();
        for (Label label : labels) {
            Label newLabel = originalToCopy.get(label);
            newLabels.add(newLabel != null ? newLabel : label);
        }
        return newLabels;
    }
}
