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

package org.jetbrains.jet.cfg;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class AbstractControlFlowTest extends KotlinTestWithEnvironment {
    static {
        System.setProperty("idea.platform.prefix", "Idea");
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    protected void doTest(String fileName) throws Exception {
        File file = new File(fileName);
        JetFile jetFile = JetTestUtils.loadJetFile(getProject(), file);

        Map<JetElement, Pseudocode> data = new LinkedHashMap<JetElement, Pseudocode>();
        AnalyzeExhaust analyzeExhaust = JetTestUtils.analyzeFile(jetFile);
        List<JetDeclaration> declarations = jetFile.getDeclarations();
        BindingContext bindingContext = analyzeExhaust.getBindingContext();
        for (JetDeclaration declaration : declarations) {
            addDeclaration(data, bindingContext, declaration);

            if (declaration instanceof JetDeclarationContainer) {
                for (JetDeclaration member : ((JetDeclarationContainer) declaration).getDeclarations()) {
                    // Properties and initializers are processed elsewhere
                    if (member instanceof JetNamedFunction) {
                        addDeclaration(data, bindingContext, member);
                    }
                }
            }
        }

        try {
            processCFData(file, data);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if ("true".equals(System.getProperty("jet.control.flow.test.dump.graphs"))) {
                CFGraphToDotFilePrinter.dumpDot(file, data.values());
            }
        }
    }

    private void addDeclaration(Map<JetElement, Pseudocode> data, BindingContext bindingContext, JetDeclaration declaration) {
        Pseudocode pseudocode = PseudocodeUtil.generatePseudocode(declaration, bindingContext);
        data.put(declaration, pseudocode);
        for (LocalFunctionDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = instruction.getBody();
            data.put(localPseudocode.getCorrespondingElement(), localPseudocode);
        }
    }

    private void processCFData(File file, Map<JetElement, Pseudocode> data) throws IOException {
        Collection<Pseudocode> pseudocodes = data.values();

        StringBuilder instructionDump = new StringBuilder();
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {

            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            String label = "";
            assert (correspondingElement instanceof JetNamedDeclaration || correspondingElement instanceof JetPropertyAccessor) :
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

            instructionDump.append("== ").append(label).append(" ==\n");

            instructionDump.append(correspondingElement.getText());
            instructionDump.append("\n---------------------\n");
            dumpInstructions((PseudocodeImpl) pseudocode, instructionDump);
            instructionDump.append("=====================\n");
            checkPseudocode((PseudocodeImpl) pseudocode);
        }

        File expectedInstructionsFile = JetTestUtils.replaceExtension(file, "instructions");
        JetTestUtils.assertEqualsToFile(expectedInstructionsFile, instructionDump.toString());
    }

    private void checkPseudocode(PseudocodeImpl pseudocode) {
        //check edges directions
        Collection<Instruction> instructions = pseudocode.getAllInstructions();
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

    private static String formatInstruction(Instruction instruction, int maxLength, Set<Instruction> remainedAfterPostProcessInstructions) {
        String[] parts = instruction.toString().split("\n");
        boolean isRemovedThroughPostProcess = !remainedAfterPostProcessInstructions.contains(instruction);
        assert isRemovedThroughPostProcess == ((InstructionImpl)instruction).isDead();
        String prefix = isRemovedThroughPostProcess ? "-   " : "    ";
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

    private void dumpInstructions(PseudocodeImpl pseudocode, @NotNull StringBuilder out) {
        final int nextInstructionsColumnWidth = countNextInstructionsColumnWidth(pseudocode.getAllInstructions());

        dumpInstructions(pseudocode, out, new Function3<Instruction, Instruction, Instruction, String>() {
            @Override
            public String invoke(Instruction instruction, Instruction next, Instruction prev) {
                StringBuilder result = new StringBuilder();
                Collection<Instruction> nextInstructions = instruction.getNextInstructions();

                if (!sameContents(next, nextInstructions)) {
                    result.append("    NEXT:").append(
                            String.format("%1$-" + nextInstructionsColumnWidth + "s", formatInstructionList(nextInstructions)));
                }
                Collection<Instruction> previousInstructions = instruction.getPreviousInstructions();
                if (!sameContents(prev, previousInstructions)) {
                    result.append("    PREV:").append(formatInstructionList(previousInstructions));
                }
                return result.toString();
            }
        });
    }

    private void dumpInstructions(
            @NotNull PseudocodeImpl pseudocode,
            @NotNull StringBuilder out,
            @NotNull Function3<Instruction, /*next*/Instruction, /*prev*/Instruction, String> getInstructionData
    ) {
        List<Instruction> instructions = pseudocode.getAllInstructions();
        Set<Instruction> remainedAfterPostProcessInstructions = Sets.newHashSet(pseudocode.getInstructions());
        List<PseudocodeImpl.PseudocodeLabel> labels = pseudocode.getLabels();
        int instructionColumnWidth = countInstructionColumnWidth(instructions);

        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            for (PseudocodeImpl.PseudocodeLabel label: labels) {
                if (label.getTargetInstructionIndex() == i) {
                    out.append(label.getName()).append(":\n");
                }
            }

            StringBuilder line = new StringBuilder();

            line.append(formatInstruction(instruction, instructionColumnWidth, remainedAfterPostProcessInstructions));

            // Only print NEXT and PREV if the values are non-trivial
            Instruction next = i == instructions.size() - 1 ? null : instructions.get(i + 1);
            Instruction prev = i == 0 ? null : instructions.get(i - 1);
            line.append(getInstructionData.invoke(instruction, next, prev));

            out.append(StringUtil.trimTrailing(line.toString()));
            out.append("\n");
        }
    }

    private static int countInstructionColumnWidth(List<Instruction> instructions) {
        int maxWidth = 0;
        for (Instruction instruction : instructions) {
            String instuctionText = instruction.toString();
            if (instuctionText.length() > maxWidth) {
                String[] parts = instuctionText.split("\n");
                if (parts.length > 1) {
                    for (String part : parts) {
                        if (part.length() > maxWidth) {
                            maxWidth = part.length();
                        }
                    }
                }
                else {
                    if (instuctionText.length() > maxWidth) {
                        maxWidth = instuctionText.length();
                    }
                }
            }
        }
        return maxWidth;
    }

    private static int countNextInstructionsColumnWidth(List<Instruction> instructions) {
        int maxWidth = 0;
        for (Instruction instruction : instructions) {
            String instructionListText = formatInstructionList(instruction.getNextInstructions());
            if (instructionListText.length() > maxWidth) {
                maxWidth = instructionListText.length();
            }
        }
        return maxWidth;
    }

    private static boolean sameContents(@Nullable Instruction natural, Collection<Instruction> actual) {
        if (natural == null) {
            return actual.isEmpty();
        }
        return Collections.singleton(natural).equals(new HashSet<Instruction>(actual));
    }
}
