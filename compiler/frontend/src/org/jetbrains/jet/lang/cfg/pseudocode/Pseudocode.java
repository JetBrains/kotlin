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
            return instructions.subList(getTargetInstructionIndex(), instructions.size());
        }

        public Instruction resolveToInstruction() {
            assert targetInstructionIndex != null;
            return instructions.get(targetInstructionIndex);
        }

    }

    private final List<Instruction> instructions = new ArrayList<Instruction>();
    private final List<PseudocodeLabel> labels = new ArrayList<PseudocodeLabel>();

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

    @NotNull
    public List<Instruction> getInstructions() {
        return instructions;
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
        instructions.add(instruction);
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
        return (SubroutineEnterInstruction) instructions.get(0);
    }

    public void bindLabel(Label label) {
        ((PseudocodeLabel) label).setTargetInstructionIndex(instructions.size());
    }

    public void postProcess() {
        if (postPrecessed) return;
        postPrecessed = true;
        for (int i = 0, instructionsSize = instructions.size(); i < instructionsSize; i++) {
            Instruction instruction = instructions.get(i);
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
                    instruction.setSink(getSinkInstruction());
                    super.visitLocalDeclarationInstruction(instruction);
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
        removeDeadInstructions();
    }

    private void removeDeadInstructions() {
        boolean hasRemovedInstruction = true;
        Collection<Instruction> processedInstructions = Sets.newHashSet();
        while (hasRemovedInstruction) {
            hasRemovedInstruction = false;
            for (Instruction instruction : instructions) {
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
    private Instruction getJumpTarget(@NotNull Label targetLabel) {
        return ((PseudocodeLabel)targetLabel).resolveToInstruction();
        //return getTargetInstruction(((PseudocodeLabel) targetLabel).resolve());
    }

//    @NotNull
//    private Instruction getTargetInstruction(@NotNull List<Instruction> instructions) {
//        while (true) {
//            assert instructions != null;
//            Instruction targetInstruction = instructions.get(0);
//
//            //if (false == targetInstruction instanceof UnconditionalJumpInstruction) {
//                return targetInstruction;
//            //}
//
////            Label label = ((UnconditionalJumpInstruction) targetInstruction).getTargetLabel();
////            instructions = ((PseudocodeLabel)label).resolve();
//        }
//    }

    @NotNull
    private Instruction getNextPosition(int currentPosition) {
        int targetPosition = currentPosition + 1;
        assert targetPosition < instructions.size() : currentPosition;
        return instructions.get(targetPosition);
        //return getTargetInstruction(instructions.subList(targetPosition, instructions.size()));
    }
}
