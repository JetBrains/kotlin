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

package org.jetbrains.kotlin.cfg;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public abstract class AbstractPseudocodeTest extends KotlinTestWithEnvironment {
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
        AnalysisResult analysisResult = JetTestUtils.analyzeFile(jetFile);
        List<JetDeclaration> declarations = jetFile.getDeclarations();
        BindingContext bindingContext = analysisResult.getBindingContext();
        for (JetDeclaration declaration : declarations) {
            addDeclaration(data, bindingContext, declaration);

            if (declaration instanceof JetDeclarationContainer) {
                for (JetDeclaration member : ((JetDeclarationContainer) declaration).getDeclarations()) {
                    // Properties and initializers are processed elsewhere
                    if (member instanceof JetNamedFunction || member instanceof JetSecondaryConstructor) {
                        addDeclaration(data, bindingContext, member);
                    }
                }
            }
        }

        try {
            processCFData(file, data, bindingContext);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        finally {
            if ("true".equals(System.getProperty("kotlin.control.flow.test.dump.graphs"))) {
                CFGraphToDotFilePrinter.dumpDot(file, data.values());
            }
        }
    }

    private static void addDeclaration(Map<JetElement, Pseudocode> data, BindingContext bindingContext, JetDeclaration declaration) {
        Pseudocode pseudocode = PseudocodeUtil.generatePseudocode(declaration, bindingContext);
        data.put(declaration, pseudocode);
        for (LocalFunctionDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = instruction.getBody();
            data.put(localPseudocode.getCorrespondingElement(), localPseudocode);
        }
    }

    private void processCFData(File file, Map<JetElement, Pseudocode> data, BindingContext bindingContext) throws IOException {
        Collection<Pseudocode> pseudocodes = data.values();

        StringBuilder instructionDump = new StringBuilder();
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            JetElement correspondingElement = pseudocode.getCorrespondingElement();
            String label;
            assert (correspondingElement instanceof JetNamedDeclaration || correspondingElement instanceof JetPropertyAccessor) :
                    "Unexpected element class is pseudocode: " + correspondingElement.getClass();
            if (correspondingElement instanceof JetFunctionLiteral) {
                label = "anonymous_" + i++;
            }
            else if (correspondingElement instanceof JetNamedDeclaration) {
                JetNamedDeclaration namedDeclaration = (JetNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else {
                String propertyName = ((JetProperty) correspondingElement.getParent()).getName();
                label = (((JetPropertyAccessor) correspondingElement).isGetter() ? "get" : "set") + "_" + propertyName;
            }

            instructionDump.append("== ").append(label).append(" ==\n");

            instructionDump.append(correspondingElement.getText());
            instructionDump.append("\n---------------------\n");
            dumpInstructions((PseudocodeImpl) pseudocode, instructionDump, bindingContext);
            instructionDump.append("=====================\n");
            checkPseudocode((PseudocodeImpl) pseudocode);
        }

        File expectedInstructionsFile = JetTestUtils.replaceExtension(file, getDataFileExtension());
        JetTestUtils.assertEqualsToFile(expectedInstructionsFile, instructionDump.toString());
    }

    protected String getDataFileExtension() {
        return "instructions";
    }

    protected void checkPseudocode(PseudocodeImpl pseudocode) {
    }

    private static String getIsDeadInstructionPrefix(
            @NotNull Instruction instruction,
            @NotNull Set<Instruction> remainedAfterPostProcessInstructions
    ) {
        boolean isRemovedThroughPostProcess = !remainedAfterPostProcessInstructions.contains(instruction);
        assert isRemovedThroughPostProcess == ((InstructionImpl)instruction).getMarkedAsDead();
        return isRemovedThroughPostProcess ? "-" : " ";
    }

    private static String getDepthInstructionPrefix(@NotNull Instruction instruction, @Nullable Instruction previous) {
        Integer prevDepth = previous != null ? previous.getLexicalScope().getDepth() : null;
        int depth = instruction.getLexicalScope().getDepth();
        if (prevDepth == null || depth != prevDepth) {
            return String.format("%2d ", depth);
        }
        return "   ";
    }

    private static String formatInstruction(Instruction instruction, int maxLength, String prefix) {
        String[] parts = instruction.toString().split("\n");

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

    protected abstract void dumpInstructions(
            @NotNull PseudocodeImpl pseudocode,
            @NotNull StringBuilder out,
            @NotNull BindingContext bindingContext
    );

    protected void dumpInstructions(
            @NotNull PseudocodeImpl pseudocode,
            @NotNull StringBuilder out,
            @NotNull Function3<Instruction, /*next*/Instruction, /*prev*/Instruction, String> getInstructionData
    ) {
        List<Instruction> instructions = pseudocode.getInstructionsIncludingDeadCode();
        Set<Instruction> remainedAfterPostProcessInstructions = Sets.newHashSet(pseudocode.getInstructions());
        List<PseudocodeImpl.PseudocodeLabel> labels = pseudocode.getLabels();
        int instructionColumnWidth = countInstructionColumnWidth(instructions);

        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            for (PseudocodeImpl.PseudocodeLabel label: labels) {
                if (label.getTargetInstructionIndex() == i) {
                    out.append(label).append(":\n");
                }
            }

            StringBuilder line = new StringBuilder();

            // Only print NEXT and PREV if the values are non-trivial
            Instruction next = i == instructions.size() - 1 ? null : instructions.get(i + 1);
            Instruction prev = i == 0 ? null : instructions.get(i - 1);

            String prefix = getIsDeadInstructionPrefix(instruction, remainedAfterPostProcessInstructions) +
                    getDepthInstructionPrefix(instruction, prev);
            line.append(formatInstruction(instruction, instructionColumnWidth, prefix));

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
}
