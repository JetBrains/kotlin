package org.jetbrains.jet.lang.cfg.pseudocode;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.Label;
import org.jetbrains.jet.lang.psi.JetElement;

import java.util.*;

/**
* @author abreslav
*/
public class Pseudocode {

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

        public int getTargetInstructionIndex() {
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
    private final List<PseudocodeLabel> labels = new ArrayList<PseudocodeLabel>();
    private final List<PseudocodeLabel> allowedDeadLabels = new ArrayList<PseudocodeLabel>();

    private final JetElement correspondingElement;
    private SubroutineExitInstruction exitInstruction;
    private SubroutineSinkInstruction sinkInstruction;
    private boolean postPrecessed = false;

    public Pseudocode(JetElement correspondingElement) {
        this.correspondingElement = correspondingElement;
    }

    public JetElement getCorrespondingElement() {
        return correspondingElement;
    }

    public PseudocodeLabel createLabel(String name) {
        PseudocodeLabel label = new PseudocodeLabel(name);
        labels.add(label);
        return label;
    }
    
    public void allowDead(Label label) {
        allowedDeadLabels.add((PseudocodeLabel) label);
    }

    @NotNull
    public List<Instruction> getInstructions() {
        return instructions;
    }

    @Deprecated //for tests only
    @NotNull
    public List<Instruction> getMutableInstructionList() {
        return mutableInstructionList;
    }
    
    @NotNull
    public List<Instruction> getDeadInstructions() {
        List<Instruction> deadInstructions = Lists.newArrayList();
        for (Instruction instruction : instructions) {
            if (instruction.isDead()) {
                deadInstructions.add(instruction);
            }
        }
        return deadInstructions;
    }

    @Deprecated //for tests only
    @NotNull
    public List<PseudocodeLabel> getLabels() {
        return labels;
    }

    public void addExitInstruction(SubroutineExitInstruction exitInstruction) {
        addInstruction(exitInstruction);
        assert this.exitInstruction == null;
        this.exitInstruction = exitInstruction;
    }
    
    public void addSinkInstruction(SubroutineSinkInstruction sinkInstruction) {
        addInstruction(sinkInstruction);
        assert this.sinkInstruction == null;
        this.sinkInstruction = sinkInstruction;
    }

    public void addInstruction(Instruction instruction) {
        mutableInstructionList.add(instruction);
        instruction.setOwner(this);
    }

    @NotNull
    public SubroutineExitInstruction getExitInstruction() {
        return exitInstruction;
    }

    @NotNull
    public SubroutineSinkInstruction getSinkInstruction() {
        return sinkInstruction;
    }


    @NotNull
    public SubroutineEnterInstruction getEnterInstruction() {
        return (SubroutineEnterInstruction) mutableInstructionList.get(0);
    }

    public void bindLabel(Label label) {
        ((PseudocodeLabel) label).setTargetInstructionIndex(mutableInstructionList.size());
    }

    public void postProcess() {
        if (postPrecessed) return;
        postPrecessed = true;
        for (int i = 0, instructionsSize = mutableInstructionList.size(); i < instructionsSize; i++) {
            Instruction instruction = mutableInstructionList.get(i);
            final int currentPosition = i;
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
                    instruction.getBody().postProcess();
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
        getExitInstruction().setSink(getSinkInstruction());
        Set<Instruction> allowedDeadStartInstructions = prepareAllowedDeadInstructions();
        markDeadInstructions();
        Collection<Instruction> allowedDeadInstructions = collectAllowedDeadInstructions(allowedDeadStartInstructions);
        instructions.addAll(mutableInstructionList);
        instructions.removeAll(allowedDeadInstructions);

    }

    private void markDeadInstructions() {
        boolean hasRemovedInstruction = true;
        Collection<Instruction> processedInstructions = Sets.newHashSet();
        while (hasRemovedInstruction) {
            hasRemovedInstruction = false;
            for (Instruction instruction : mutableInstructionList) {
                if (!(instruction instanceof SubroutineEnterInstruction || instruction instanceof SubroutineExitInstruction || instruction instanceof SubroutineSinkInstruction) &&
                    instruction.getPreviousInstructions().isEmpty() && !processedInstructions.contains(instruction)) {
                    hasRemovedInstruction = true;
                    for (Instruction nextInstruction : instruction.getNextInstructions()) {
                        nextInstruction.getPreviousInstructions().remove(instruction);
                    }
                    ((InstructionImpl)instruction).die();
                    processedInstructions.add(instruction);
                }
            }
        }
    }

    @NotNull
    private Set<Instruction> prepareAllowedDeadInstructions() {
        Set<Instruction> allowedDeadStartInstructions = Sets.newHashSet();
        for (PseudocodeLabel allowedDeadLabel : allowedDeadLabels) {
            Instruction allowedDeadInstruction = getJumpTarget(allowedDeadLabel);
            if (allowedDeadInstruction.getPreviousInstructions().isEmpty()) {
                allowedDeadStartInstructions.add(allowedDeadInstruction);
            }
        }
        return allowedDeadStartInstructions;
    }

    @NotNull
    private Collection<Instruction> collectAllowedDeadInstructions(@NotNull Set<Instruction> allowedDeadStartInstructions) {
        Set<Instruction> allowedDeadInstructions = Sets.newHashSet();
        for (Instruction allowedDeadStartInstruction : allowedDeadStartInstructions) {
            collectAllowedDeadInstructions(allowedDeadStartInstruction, allowedDeadInstructions);
        }
        return allowedDeadInstructions;
    }
    
    private void collectAllowedDeadInstructions(Instruction allowedDeadInstruction, Set<Instruction> instructionSet) {
        if (allowedDeadInstruction.isDead()) {
            instructionSet.add(allowedDeadInstruction);
            for (Instruction instruction : allowedDeadInstruction.getNextInstructions()) {
                collectAllowedDeadInstructions(instruction, instructionSet);
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
