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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Sets;
import com.intellij.openapi.util.Condition;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.AbstractDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiagnosticMessageTest extends JetLiteFixture {
    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/diagnosticMessage/";
    }

    public void doTest(String name, int diagnosticNumber, AbstractDiagnosticFactory... diagnosticFactories) throws Exception {
        String fileName = name + ".kt";
        JetFile psiFile = createPsiFile(null, fileName, loadFile(fileName));

        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(psiFile, Collections.<AnalyzerScriptParameter>emptyList());
        BindingContext bindingContext = analyzeExhaust.getBindingContext();

        final Set<AbstractDiagnosticFactory> factoriesSet = Sets.newHashSet(diagnosticFactories);
        List<Diagnostic> diagnostics = ContainerUtil.filter(bindingContext.getDiagnostics(), new Condition<Diagnostic>() {
            @Override
            public boolean value(Diagnostic diagnostic) {
                return factoriesSet.contains(diagnostic.getFactory());
            }
        });

        assertEquals("Expected diagnostics number mismatch:", diagnosticNumber, diagnostics.size());

        int index = 1;
        for (Diagnostic diagnostic : diagnostics) {
            String readableDiagnosticText;
            String extension;
            if (IdeErrorMessages.MAP.get(diagnostic.getFactory()) != null) {
                readableDiagnosticText = IdeErrorMessages.RENDERER.render(diagnostic).replaceAll(">", ">\n");
                extension = "html";
            }
            else {
                readableDiagnosticText = DefaultErrorMessages.RENDERER.render(diagnostic);
                extension = "txt";
            }
            String errorMessageFileName = name + index;
            String path = getTestDataPath() + "/" + errorMessageFileName + "." + extension;
            String actualText = "<!-- " + errorMessageFileName + " -->\n" + readableDiagnosticText;
            assertSameLinesWithFile(path, actualText);

            index++;
        }
    }

    public void testConflictingSubstitutions() throws Exception {
        doTest("conflictingSubstitutions", 2, Errors.TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS);
    }

    public void testFunctionPlaceholder() throws Exception {
        doTest("functionPlaceholder", 3, Errors.TYPE_INFERENCE_TYPE_CONSTRUCTOR_MISMATCH);
    }

    public void testRenderCollectionOfTypes() throws Exception {
        doTest("renderCollectionOfTypes", 1, Errors.EXPECTED_PARAMETERS_NUMBER_MISMATCH);
    }

    public void testInaccessibleOuterClassExpression() throws Exception {
        doTest("inaccessibleOuterClassExpression", 1, Errors.INACCESSIBLE_OUTER_CLASS_EXPRESSION);
    }

    public void testUpperBoundViolated() throws Exception {
        doTest("upperBoundViolated", 1, Errors.TYPE_INFERENCE_UPPER_BOUND_VIOLATED);
    }
}
