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

/*
 * @author max
 */
package org.jetbrains.jet.cfg;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

public class JetControlFlowTest extends JetLiteFixture {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    private String myName;

    public JetControlFlowTest(String dataPath, String name) {
        super(dataPath);
        myName = name;
    }


    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(CompilerSpecialMode.STDLIB);
    }

    @Override
    public String getName() {
        return "test" + myName;
    }

    protected String getTestFilePath() {
        return myFullDataPath + "/" + myName;
    }
    
    @Override
    protected void runTest() throws Throwable {
        JetFile file = loadPsiFile(myName + ".jet");

        final Map<JetElement, Pseudocode> data = new LinkedHashMap<JetElement, Pseudocode>();
        AnalyzeExhaust analyzeExhaust = JetTestUtils.analyzeFile(file);
        List<JetDeclaration> declarations = file.getDeclarations();
        BindingContext bindingContext = analyzeExhaust.getBindingContext();
        for (JetDeclaration declaration : declarations) {
            Pseudocode pseudocode = PseudocodeUtil.generatePseudocode(declaration, bindingContext);
            data.put(declaration, pseudocode);
            for (LocalDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
                Pseudocode localPseudocode = instruction.getBody();
                data.put(localPseudocode.getCorrespondingElement(), localPseudocode);
            }
        }

        try {
            processCFData(myName, data);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if ("true".equals(System.getProperty("jet.control.flow.test.dump.graphs"))) {
                dumpDot(myName, data.values());
            }
        }
    }

    private void processCFData(String name, Map<JetElement, Pseudocode> data) throws IOException {
        Collection<Pseudocode> pseudocodes = data.values();

        StringBuilder instructionDump = new StringBuilder();
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {

            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            String label = "";
            assert (correspondingElement instanceof JetNamedDeclaration || correspondingElement instanceof JetSecondaryConstructor || correspondingElement instanceof JetPropertyAccessor) :
                    "Unexpected element class is pseudocode: " + correspondingElement.getClass();
            if (correspondingElement instanceof JetFunctionLiteral) {
                label = "anonymous_" + i++;
            }
            else if (correspondingElement instanceof JetNamedDeclaration) {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else if (correspondingElement instanceof JetPropertyAccessor) {
                String propertyName = ((JetProperty) correspondingElement.getParent()).getName();
                label = (((JetPropertyAccessor) correspondingElement).isGetter() ? "get" : "set") + "_" + propertyName;
            }
            else if (correspondingElement instanceof JetSecondaryConstructor) {
                label = "this";
            }

            instructionDump.append("== ").append(label).append(" ==\n");

            instructionDump.append(correspondingElement.getText());
            instructionDump.append("\n---------------------\n");
            dumpInstructions((PseudocodeImpl) pseudocode, instructionDump);
            instructionDump.append("=====================\n");
            
            //check edges directions
            Collection<Instruction> instructions = ((PseudocodeImpl)pseudocode).getAllInstructions();
            for (Instruction instruction : instructions) {
                if (!((InstructionImpl)instruction).isDead()) {
                    for (Instruction nextInstruction : instruction.getNextInstructions()) {
                        assertTrue("instruction '" + instruction + "' has '" + nextInstruction + "' among next instructions list, but not vice versa",
                                   nextInstruction.getPreviousInstructions().contains(instruction));
                    }
                    for (Instruction prevInstruction : instruction.getPreviousInstructions()) {
                        assertTrue("instruction '" + instruction + "' has '" + prevInstruction + "' among previous instructions list, but not vice versa",
                                   prevInstruction.getNextInstructions().contains(instruction));
                    }
                }
            }
        }

        String expectedInstructionsFileName = getTestFilePath() + ".instructions";
        File expectedInstructionsFile = new File(expectedInstructionsFileName);
        if (!expectedInstructionsFile.exists()) {
            FileUtil.writeToFile(expectedInstructionsFile, instructionDump.toString());
            fail("No expected instructions for " + name + " generated result is written into " + expectedInstructionsFileName);
        }
        String expectedInstructions = StringUtil.convertLineSeparators(FileUtil.loadFile(expectedInstructionsFile));

        assertEquals(expectedInstructions, instructionDump.toString());

//                        StringBuilder graphDump = new StringBuilder();
//                        for (Pseudocode pseudocode : pseudocodes) {
//                            topOrderDump(pseudocode.)
//                        }
    }

    public void dfsDump(PseudocodeImpl pseudocode, StringBuilder nodes, StringBuilder edges, Map<Instruction, String> nodeNames) {
        dfsDump(nodes, edges, pseudocode.getAllInstructions().get(0), nodeNames);
    }

    private void dfsDump(StringBuilder nodes, StringBuilder edges, Instruction instruction, Map<Instruction, String> nodeNames) {
        if (nodeNames.containsKey(instruction)) return;
        String name = "n" + nodeNames.size();
        nodeNames.put(instruction, name);
        nodes.append(name).append(" := ").append(renderName(instruction));

    }

    private String renderName(Instruction instruction) {
        throw new UnsupportedOperationException(); // TODO
    }

    private static String formatInstruction(Instruction instruction, int maxLength, Set<Instruction> remainedAfterPostProcessInstructions) {
        String[] parts = instruction.toString().split("\n");
        boolean isRemovedThroughPostProcess = !remainedAfterPostProcessInstructions.contains(instruction);
        String prefix = isRemovedThroughPostProcess ? "-   " : ((InstructionImpl)instruction).isDead() ? "*   " : "    ";
        if (parts.length == 1) {
            return prefix + String.format("%1$-" + maxLength + "s", instruction);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0, partsLength = parts.length; i < partsLength; i++) {
            String part = parts[i];
            sb.append(prefix).append(String.format("%1$-" + maxLength + "s", part));
            if (i < partsLength - 1) sb.append("\n");
        }
        return sb.toString();
    }

    private static String formatInstructionList(Collection<Instruction> instructions) {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (Iterator<Instruction> iterator = instructions.iterator(); iterator.hasNext(); ) {
            Instruction instruction = iterator.next();
            String instructionText = instruction.toString();
            String[] parts = instructionText.split("\n");
            if (parts.length > 1) {
                StringBuilder instructionSb = new StringBuilder();
                for (String part : parts) {
                    instructionSb.append(part.trim()).append(' ');
                }
                if (instructionSb.toString().length() > 30) {
                    sb.append(instructionSb.substring(0, 28)).append("..)");
                }
                else {
                    sb.append(instructionSb);
                }
            }
            else {
                sb.append(instruction);
            }
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(']');
        return sb.toString();
    }

    public void dumpInstructions(PseudocodeImpl pseudocode, @NotNull StringBuilder out) {
        List<Instruction> instructions = pseudocode.getAllInstructions();
        Set<Instruction> remainedAfterPostProcessInstructions = Sets.newHashSet(pseudocode.getInstructions());
        List<PseudocodeImpl.PseudocodeLabel> labels = pseudocode.getLabels();
        List<PseudocodeImpl> locals = new ArrayList<PseudocodeImpl>();
        int maxLength = 0;
        int maxNextLength = 0;
        for (Instruction instruction : instructions) {
            String instuctionText = instruction.toString();
            if (instuctionText.length() > maxLength) {
                String[] parts = instuctionText.split("\n");
                if (parts.length > 1) {
                    for (String part : parts) {
                        if (part.length() > maxLength) {
                            maxLength = part.length();
                        }
                    }
                }
                else {
                    if (instuctionText.length() > maxLength) {
                        maxLength = instuctionText.length();
                    }
                }
            }
            String instructionListText = formatInstructionList(instruction.getNextInstructions());
            if (instructionListText.length() > maxNextLength) {
                maxNextLength = instructionListText.length();
            }
        }
        for (int i = 0, instructionsSize = instructions.size(); i < instructionsSize; i++) {
            Instruction instruction = instructions.get(i);
            if (instruction instanceof LocalDeclarationInstruction) {
                LocalDeclarationInstruction localDeclarationInstruction = (LocalDeclarationInstruction) instruction;
                locals.add((PseudocodeImpl) localDeclarationInstruction.getBody());
            }
            for (PseudocodeImpl.PseudocodeLabel label: labels) {
                if (label.getTargetInstructionIndex() == i) {
                    out.append(label.getName()).append(":\n");
                }
            }

            out.append(formatInstruction(instruction, maxLength, remainedAfterPostProcessInstructions)).
                    append("    NEXT:").append(String.format("%1$-" + maxNextLength + "s", formatInstructionList(instruction.getNextInstructions()))).
                    append("    PREV:").append(formatInstructionList(instruction.getPreviousInstructions())).append("\n");
        }
        for (PseudocodeImpl local : locals) {
            dumpInstructions(local, out);
        }
    }

    public void dumpEdges(List<Instruction> instructions,  final PrintStream out, final int[] count, final Map<Instruction, String> nodeToName) {
        for (final Instruction fromInst : instructions) {
            fromInst.accept(new InstructionVisitor() {
                @Override
                public void visitLocalDeclarationInstruction(LocalDeclarationInstruction instruction) {
                    int index = count[0];
//                    instruction.getBody().dumpSubgraph(out, "subgraph cluster_" + index, count, "color=blue;\nlabel = \"f" + index + "\";", nodeToName);
                    printEdge(out, nodeToName.get(instruction), nodeToName.get(((PseudocodeImpl)instruction.getBody()).getAllInstructions().get(0)), null);
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

    public void dumpNodes(List<Instruction> instructions, PrintStream out, int[] count, Map<Instruction, String> nodeToName, Set<Instruction> remainedAfterPostProcessInstructions) {
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
            else if (node instanceof UnsupportedElementInstruction) {
                shape = "box, fillcolor=red, style=filled";
            }
            else if (node instanceof LocalDeclarationInstruction) {
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

    private void printEdge(PrintStream out, String from, String to, String label) {
        if (label != null) {
            label = "[label=\"" + label + "\"]";
        }
        else {
            label = "";
        }
        out.println(from + " -> " + to + label + ";");
    }

    private void dumpDot(String name, Collection<Pseudocode> pseudocodes) throws FileNotFoundException {
        String graphFileName = getTestFilePath() + ".dot";
        File target = new File(graphFileName);

        PrintStream out = new PrintStream(target);

        out.println("digraph " + name + " {");
        int[] count = new int[1];
        Map<Instruction, String> nodeToName = new HashMap<Instruction, String>();
        for (Pseudocode pseudocode : pseudocodes) {
            dumpNodes(((PseudocodeImpl)pseudocode).getAllInstructions(), out, count, nodeToName, Sets.newHashSet(pseudocode.getInstructions()));
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
            dumpEdges(((PseudocodeImpl)pseudocode).getAllInstructions(), out, count, nodeToName);
            out.println("}");
            i++;
        }
        out.println("}");
        out.close();
    }

    public static TestSuite suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(JetTestCaseBuilder.suiteForDirectory(JetTestCaseBuilder.getTestDataPathBase(), "/cfg/", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetControlFlowTest(dataPath, name);
            }
        }));
        return suite;
    }

}
