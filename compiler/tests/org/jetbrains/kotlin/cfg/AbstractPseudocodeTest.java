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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.jvm.functions.Function3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cfg.pseudocode.Pseudocode;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeImpl;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeLabel;
import org.jetbrains.kotlin.cfg.pseudocode.PseudocodeUtil;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.Instruction;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.InstructionImpl;
import org.jetbrains.kotlin.cfg.pseudocode.instructions.special.LocalFunctionDeclarationInstruction;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class AbstractPseudocodeTest extends KotlinTestWithEnvironment {
    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    protected void doTest(String fileName) throws Exception {
        File file = new File(fileName);
        KtFile jetFile = KotlinTestUtils.loadJetFile(getProject(), file);

        SetMultimap<KtElement, Pseudocode> data = LinkedHashMultimap.create();
        AnalysisResult analysisResult = KotlinTestUtils.analyzeFile(jetFile, getEnvironment());
        List<KtDeclaration> declarations = jetFile.getDeclarations();
        BindingContext bindingContext = analysisResult.getBindingContext();
        for (KtDeclaration declaration : declarations) {
            addDeclaration(data, bindingContext, declaration);

            if (declaration instanceof KtDeclarationContainer) {
                for (KtDeclaration member : ((KtDeclarationContainer) declaration).getDeclarations()) {
                    // Properties and initializers are processed elsewhere
                    if (member instanceof KtNamedFunction || member instanceof KtSecondaryConstructor) {
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

    private static void addDeclaration(SetMultimap<KtElement, Pseudocode> data, BindingContext bindingContext, KtDeclaration declaration) {
        Pseudocode pseudocode = PseudocodeUtil.generatePseudocode(declaration, bindingContext);
        data.put(declaration, pseudocode);
        for (LocalFunctionDeclarationInstruction instruction : pseudocode.getLocalDeclarations()) {
            Pseudocode localPseudocode = instruction.getBody();
            data.put(localPseudocode.getCorrespondingElement(), localPseudocode);
        }
    }

    private void processCFData(File file, SetMultimap<KtElement, Pseudocode> data, BindingContext bindingContext) throws IOException {
        Collection<Pseudocode> pseudocodes = data.values();

        StringBuilder instructionDump = new StringBuilder();
        int i = 0;
        for (Pseudocode pseudocode : pseudocodes) {
            KtElement correspondingElement = pseudocode.getCorrespondingElement();
            String label;
            assert (correspondingElement instanceof KtNamedDeclaration || correspondingElement instanceof KtPropertyAccessor) :
                    "Unexpected element class is pseudocode: " + correspondingElement.getClass();
            boolean isAnonymousFunction =
                    correspondingElement instanceof KtFunctionLiteral
                    || (correspondingElement instanceof KtNamedFunction && correspondingElement.getName() == null);
            if (isAnonymousFunction) {
                label = "anonymous_" + i++;
            }
            else if (correspondingElement instanceof KtNamedDeclaration) {
                KtNamedDeclaration namedDeclaration = (KtNamedDeclaration) correspondingElement;
                label = namedDeclaration.getName();
            }
            else {
                String propertyName = ((KtProperty) correspondingElement.getParent()).getName();
                label = (((KtPropertyAccessor) correspondingElement).isGetter() ? "get" : "set") + "_" + propertyName;
            }

            instructionDump.append("== ").append(label).append(" ==\n");

            instructionDump.append(correspondingElement.getText());
            instructionDump.append("\n---------------------\n");
            dumpInstructions((PseudocodeImpl) pseudocode, instructionDump, bindingContext);
            instructionDump.append("=====================\n");
            checkPseudocode((PseudocodeImpl) pseudocode);
        }

        File expectedInstructionsFile = KotlinTestUtils.replaceExtension(file, getDataFileExtension());
        KotlinTestUtils.assertEqualsToFile(expectedInstructionsFile, instructionDump.toString());
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
        Integer prevDepth = previous != null ? previous.getBlockScope().getDepth() : null;
        int depth = instruction.getBlockScope().getDepth();
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
        List<PseudocodeLabel> labels = pseudocode.getLabels();
        int instructionColumnWidth = countInstructionColumnWidth(instructions);

        for (int i = 0; i < instructions.size(); i++) {
            Instruction instruction = instructions.get(i);
            for (PseudocodeLabel label: labels) {
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
