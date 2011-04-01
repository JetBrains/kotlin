package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.cfg.Label;

import java.io.PrintStream;
import java.util.*;

/**
* @author abreslav
*/
public class Pseudocode {
    public class PseudocodeLabel implements Label {
        private final String name;

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

        @Nullable
        private List<Instruction> resolve() {
            Integer result = labels.get(this);
            assert result != null;
            return instructions.subList(result, instructions.size());
        }

    }

    private final List<Instruction> instructions = new ArrayList<Instruction>();
    private final Map<Label, Integer> labels = new LinkedHashMap<Label, Integer>();

//    @Nullable
//    private final Pseudocode parent;
//
//    public Pseudocode(Pseudocode parent) {
//        this.parent = parent;
//    }

    public PseudocodeLabel createLabel(String name) {
        return new PseudocodeLabel(name);
    }

    public void addInstruction(Instruction instruction) {
        instructions.add(instruction);
    }

    public void addLabel(Label label) {
        labels.put(label, instructions.size());
    }

    public void postProcess() {
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
                public void visitFunctionLiteralValue(FunctionLiteralValueInstruction instruction) {
                    instruction.getBody().postProcess();
                    super.visitFunctionLiteralValue(instruction);
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
        return getTargetInstruction(((PseudocodeLabel) targetLabel).resolve());
    }

    @NotNull
    private Instruction getTargetInstruction(@NotNull List<Instruction> instructions) {
        while (true) {
            assert instructions != null;
            Instruction targetInstruction = instructions.get(0);

            if (false == targetInstruction instanceof UnconditionalJumpInstruction) {
                return targetInstruction;
            }

            Label label = ((UnconditionalJumpInstruction) targetInstruction).getTargetLabel();
            instructions = ((PseudocodeLabel)label).resolve();
        }
    }

    @NotNull
    private Instruction getNextPosition(int currentPosition) {
        int targetPosition = currentPosition + 1;
        assert targetPosition < instructions.size() : currentPosition;
        return getTargetInstruction(instructions.subList(targetPosition, instructions.size()));
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
        String graphHeader = "digraph g";
        dumpSubgraph(out, graphHeader, new int[1], "");
    }

    private void dumpSubgraph(final PrintStream out, String graphHeader, final int[] count, String style) {
        out.println(graphHeader + " {");
        out.println(style);

        final Map<Instruction, String> nodeToName = new HashMap<Instruction, String>();
        for (Instruction node : instructions) {
            if (node instanceof UnconditionalJumpInstruction) {
                continue;
            }
            String name = "n" + count[0]++;
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
            else if (node instanceof UnsupportedElementInstruction) {
                shape = "box, fillcolor=red, style=filled";
            }
            else if (node instanceof FunctionLiteralValueInstruction) {
                shape = "Mcircle";
            }
            out.println(name + "[label=\"" + text + "\", shape=" + shape + "];");
        }

        for (final Instruction fromInst : instructions) {
            fromInst.accept(new InstructionVisitor() {
                @Override
                public void visitFunctionLiteralValue(FunctionLiteralValueInstruction instruction) {
                    int index = count[0];
                    instruction.getBody().dumpSubgraph(out, "subgraph cluster_" + index, count, "color=blue;\nlabel = \"f" + index + "\";");
                    printEdge(out, nodeToName.get(instruction), "n" + index, null);
                    visitInstructionWithNext(instruction);
                }

                @Override
                public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
                    // Nothing
                }

                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getResolvedTarget()), null);
                }

                @Override
                public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                    visitJump(instruction);
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNext()), null);
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
                    printEdge(out, from, nodeToName.get(instruction.getNextOnFalse()), "no");
                    printEdge(out, from, nodeToName.get(instruction.getNextOnTrue()), "yes");
                }

                @Override
                public void visitInstructionWithNext(InstructionWithNext instruction) {
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNext()), null);
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
    }

    private void printEdge(PrintStream out, String from, String to, String label) {
        if (label != null) {
            label = "[label=\"" + label + "\"]";
        }
        else {
            label = "";
        }
        out.println(from + " -> " + to + label + ";");
    }


}
