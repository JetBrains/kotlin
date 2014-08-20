/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.cfg;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.cfg.pseudocode.Pseudocode;
import org.jetbrains.jet.lang.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionVisitor;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionWithNext;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.MagicInstruction;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval.MagicKind;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.jumps.*;
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.special.*;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetNamedDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.*;

public class CFGraphToDotFilePrinter {
    public static void dumpDot(File file, Collection<Pseudocode> pseudocodes) throws FileNotFoundException {
        File target = JetTestUtils.replaceExtension(file, "dot");

        PrintStream out = new PrintStream(target);

        out.println("digraph " + FileUtil.getNameWithoutExtension(file) + " {");
        int[] count = new int[1];
        Map<Instruction, String> nodeToName = new HashMap<Instruction, String>();
        for (Pseudocode pseudocode : pseudocodes) {
            dumpNodes(pseudocode.getInstructionsIncludingDeadCode(), out, count, nodeToName, Sets
                    .newHashSet(pseudocode.getInstructions()));
        }
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            String label;
            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            if (correspondingElement instanceof JetNamedDeclaration) {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else {
                label = "anonymous_" + i;
            }
            out.println("subgraph cluster_" + i + " {\n" +
                        "label=\"" + label + "\";\n" +
                        "color=blue;\n");
            dumpEdges(pseudocode.getInstructionsIncludingDeadCode(), out, count, nodeToName);
            out.println("}");
            i++;
        }
        out.println("}");
        out.close();
    }

    private static void dumpEdges(List<Instruction> instructions,  final PrintStream out, final int[] count, final Map<Instruction, String> nodeToName) {
        for (Instruction fromInst : instructions) {
            fromInst.accept(new InstructionVisitor() {
                @Override
                public void visitLocalFunctionDeclarationInstruction(LocalFunctionDeclarationInstruction instruction) {
                    int index = count[0];
//                    instruction.getBody().dumpSubgraph(out, "subgraph cluster_" + index, count, "color=blue;\nlabel = \"f" + index + "\";", nodeToName);
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getBody().getInstructionsIncludingDeadCode().get(0)), null);
                    visitInstructionWithNext(instruction);
                }

                @Override
                public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getResolvedTarget()), null);
                }

                @Override
                public void visitJump(AbstractJumpInstruction instruction) {
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getResolvedTarget()), null);
                }

                @Override
                public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
                    for (Instruction nextInstruction : instruction.getNextInstructions()) {
                        printEdge(out, nodeToName.get(instruction), nodeToName.get(nextInstruction), null);
                    }
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
                    if (!instruction.getNextInstructions().isEmpty()) {
                        printEdge(out, nodeToName.get(instruction), nodeToName.get(instruction.getNextInstructions().iterator().next()), null);
                    }
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
    }

    private static void dumpNodes(List<Instruction> instructions, PrintStream out, int[] count, Map<Instruction, String> nodeToName, Set<Instruction> remainedAfterPostProcessInstructions) {
        for (Instruction node : instructions) {
            String name = "n" + count[0]++;
            nodeToName.put(node, name);
            String text = node.toString();
            int newline = text.indexOf("\n");
            if (newline >= 0) {
                text = text.substring(0, newline);
            }
            String shape = "box";
            if (node instanceof ConditionalJumpInstruction || node instanceof UnconditionalJumpInstruction) {
                shape = "diamond";
            }
            else if (node instanceof NondeterministicJumpInstruction) {
                shape = "Mdiamond";
            }
            else if (node instanceof MagicInstruction && ((MagicInstruction) node).getKind() == MagicKind.UNSUPPORTED_ELEMENT) {
                shape = "box, fillcolor=red, style=filled";
            }
            else if (node instanceof LocalFunctionDeclarationInstruction) {
                shape = "Mcircle";
            }
            else if (node instanceof SubroutineEnterInstruction || node instanceof SubroutineExitInstruction) {
                shape = "roundrect, style=rounded";
            }
            if (!remainedAfterPostProcessInstructions.contains(node)) {
                shape += "box, fillcolor=grey, style=filled";
            }
            out.println(name + "[label=\"" + text + "\", shape=" + shape + "];");
        }
    }

    private static void printEdge(PrintStream out, String from, String to, String label) {
        if (label != null) {
            label = "[label=\"" + label + "\"]";
        }
        else {
            label = "";
        }
        out.println(from + " -> " + to + label + ";");
    }
}
