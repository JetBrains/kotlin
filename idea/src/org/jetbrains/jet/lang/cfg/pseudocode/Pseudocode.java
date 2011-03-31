package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.Label;

import java.io.PrintStream;
import java.util.*;

/**
* @author abreslav
*/
public class Pseudocode {
    private final List<Instruction> instructions = new ArrayList<Instruction>();
    private final Map<Label, Integer> labels = new LinkedHashMap<Label, Integer>();

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public Map<Label, Integer> getLabels() {
        return labels;
    }

    public void postProcess() {
        for (int i = 0, instructionsSize = instructions.size(); i < instructionsSize; i++) {
            Instruction instruction = instructions.get(i);
            final int currentPosition = i;
            instruction.accept(new InstructionVisitor() {
                @Override
                public void visitRead(ValueInstruction instruction) {
                    instruction.setNext(getNextPosition(currentPosition));
                }

                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    instruction.setResolvedTarget(getJumpTarget(instruction.getTargetLabel()));
                }

                @Override
                public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                    instruction.setNext(getNextPosition(currentPosition));
                    visitJump(instruction);
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
                public void visitSubroutineExit(SubroutineExitInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitInstruction(Instruction instruction) {
                    throw new UnsupportedOperationException(instruction.toString());
                }
            });
        }
    }

    @NotNull
    private Instruction getJumpTarget(@NotNull Label targetLabel) {
        Integer targetPosition = labels.get(targetLabel);
        return getTargetInstruction(targetPosition);
    }

    @NotNull
    private Instruction getTargetInstruction(@NotNull Integer targetPosition) {
        while (true) {
            assert targetPosition != null;
            Instruction targetInstruction = instructions.get(targetPosition);

            if (false == targetInstruction instanceof UnconditionalJumpInstruction) {
                return targetInstruction;
            }

            Label label = ((UnconditionalJumpInstruction) targetInstruction).getTargetLabel();
            targetPosition = labels.get(label);
        }
    }

    @NotNull
    private Instruction getNextPosition(int currentPosition) {
        int targetPosition = currentPosition + 1;
        assert targetPosition < instructions.size() : currentPosition;
        return getTargetInstruction(targetPosition);
    }

    public void dumpInstructions(@NotNull PrintStream out) {
        for (int i = 0, instructionsSize = instructions.size(); i < instructionsSize; i++) {
            Instruction instruction = instructions.get(i);
            for (Map.Entry<Label, Integer> entry : labels.entrySet()) {
                if (entry.getValue() == i) {
                    out.println(entry.getKey() + ":");
                }
            }
            out.println("    " + instruction);
        }
    }

    public void dumpGraph(@NotNull final PrintStream out) {
        out.println("digraph g {");

        final Map<Instruction, String> nodeToName = new HashMap<Instruction, String>();
        int count = 0;
        for (Instruction node : instructions) {
            if (node instanceof UnconditionalJumpInstruction) {
                continue;
            }
            String name = "n" + count++;
            nodeToName.put(node, name);
            String text = node.toString();
            int newline = text.indexOf("\n");
            if (newline >= 0) {
                text = text.substring(0, newline);
            }
            String shape = "box";
            if (node instanceof ConditionalJumpInstruction) {
                shape = "diamond";
            }
            else if (node instanceof NondeterministicJumpInstruction) {
                shape = "Mdiamond";
            }
            out.println(name + "[label=\"" + text + "\", shape=" + shape + "];");
        }

        for (final Instruction fromInst : instructions) {
            fromInst.accept(new InstructionVisitor() {
                @Override
                public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    writeEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getResolvedTarget()), null);
                }

                @Override
                public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                    visitJump(instruction);
                    writeEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNext()), null);
                }

                @Override
                public void visitReturnValue(ReturnValueInstruction instruction) {
                    super.visitReturnValue(instruction);
                }

                @Override
                public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
                    super.visitReturnNoValue(instruction);
                }

                @Override
                public void visitConditionalJump(ConditionalJumpInstruction instruction) {
                    String from = nodeToName.get(instruction);
                    writeEdge(out, from, nodeToName.get(instruction.getNextOnFalse()), "no");
                    writeEdge(out, from, nodeToName.get(instruction.getNextOnTrue()), "yes");
                }

                @Override
                public void visitRead(ValueInstruction instruction) {
                    writeEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNext()), null);
                }

                @Override
                public void visitSubroutineExit(SubroutineExitInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitInstruction(Instruction instruction) {
                    throw new UnsupportedOperationException(instruction.toString());
                }
            });
        }
        out.println("}");
        out.close();
    }

    private void writeEdge(PrintStream out, String from, String to, String label) {
        if (label != null) {
            label = "[label=\"" + label + "\"]";
        }
        else {
            label = "";
        }
        out.println(from + " -> " + to + label + ";");
    }


}
