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
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.DiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiagnosticMessageTest extends JetLiteFixture {
    private static final String DIAGNOSTICS_NUMBER_DIRECTIVE = "DIAGNOSTICS_NUMBER";
    private static final String DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS";

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_ONLY);
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/diagnosticMessage/";
    }

    public void doTest(String name) throws Exception {
        String fileName = name + ".kt";
        JetFile psiFile = createPsiFile(null, fileName, loadFile(fileName));

        String fileData = JetTestUtils.doLoadFile(new File(getTestDataPath() + fileName));
        Map<String,String> directives = JetTestUtils.parseDirectives(fileData);
        int diagnosticNumber = computeDiagnosticNumber(directives);
        final Set<DiagnosticFactory> diagnosticFactories = computeDiagnosticFactories(directives);

        AnalyzeExhaust analyzeExhaust = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(psiFile, Collections.<AnalyzerScriptParameter>emptyList());
        BindingContext bindingContext = analyzeExhaust.getBindingContext();

        List<Diagnostic> diagnostics = ContainerUtil.filter(bindingContext.getDiagnostics().all(), new Condition<Diagnostic>() {
            @Override
            public boolean value(Diagnostic diagnostic) {
                return diagnosticFactories.contains(diagnostic.getFactory());
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

    private int computeDiagnosticNumber(Map<String, String> directives) {
        String diagnosticsNumber = directives.get(DIAGNOSTICS_NUMBER_DIRECTIVE);
        assert diagnosticsNumber != null : DIAGNOSTICS_NUMBER_DIRECTIVE + " should be present.";
        try {
            return Integer.parseInt(diagnosticsNumber);
        }
        catch (NumberFormatException e) {
            throw new AssertionError(DIAGNOSTICS_NUMBER_DIRECTIVE + " should contain number as its value.");
        }
    }

    private Set<DiagnosticFactory> computeDiagnosticFactories(Map<String, String> directives) {
        String diagnosticsData = directives.get(DIAGNOSTICS_DIRECTIVE);
        assert diagnosticsData != null : DIAGNOSTICS_DIRECTIVE + " should be present.";
        Set<DiagnosticFactory> diagnosticFactories = Sets.newHashSet();
        String[] diagnostics = diagnosticsData.split(" ");
        for (String diagnosticName : diagnostics) {
            String errorMessage = "Can't load diagnostic factory for " + diagnosticName;
            try {
                Field field = Errors.class.getField(diagnosticName);
                Object value = field.get(null);
                if (value instanceof DiagnosticFactory) {
                    diagnosticFactories.add((DiagnosticFactory)value);
                }
                else {
                    throw new AssertionError(errorMessage);
                }
            }
            catch (NoSuchFieldException e) {
                throw new AssertionError(errorMessage);
            }
            catch (IllegalAccessException e) {
                throw new AssertionError(errorMessage);
            }
        }
        return diagnosticFactories;
    }

    public void testConflictingSubstitutions() throws Exception {
        doTest("conflictingSubstitutions");
    }

    public void testFunctionPlaceholder() throws Exception {
        doTest("functionPlaceholder");
    }

    public void testRenderCollectionOfTypes() throws Exception {
        doTest("renderCollectionOfTypes");
    }

    public void testInaccessibleOuterClassExpression() throws Exception {
        doTest("inaccessibleOuterClassExpression");
    }

    public void testUpperBoundViolated() throws Exception {
        doTest("upperBoundViolated");
    }

    public void testTypeMismatchWithNothing() throws Exception {
        doTest("typeMismatchWithNothing");
    }

    public void testInvisibleMember() throws Exception {
        doTest("invisibleMember");
    }

    public void testNumberValueTypes() throws Exception {
        doTest("numberValueTypes");
    }

    public void testTypeInferenceExpectedTypeMismatch() throws Exception {
        doTest("typeInferenceExpectedTypeMismatch");
    }
}
