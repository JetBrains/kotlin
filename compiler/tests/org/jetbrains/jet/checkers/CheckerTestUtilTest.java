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

package org.jetbrains.jet.checkers;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;

import java.util.Collections;
import java.util.List;

public class CheckerTestUtilTest extends JetLiteFixture {

    public CheckerTestUtilTest() {
        super("diagnostics/checkerTestUtil");
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }


    protected void doTest(TheTest theTest) throws Exception {
        prepareForTest("test");
        theTest.test(getFile());
    }

    public void testEquals() throws Exception {
        doTest(new TheTest() {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
            }
        });
    }

    public void testMissing() throws Exception {
        doTest(new TheTest("Missing TYPE_MISMATCH at 56 to 57") {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnostics.remove(1);
            }
        });
    }

    public void testUnexpected() throws Exception {
        doTest(new TheTest("Unexpected TYPE_MISMATCH at 56 to 57") {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(1);
            }
        });
    }

    public void testBoth() throws Exception {
        doTest(new TheTest("Unexpected TYPE_MISMATCH at 56 to 57", "Missing UNRESOLVED_REFERENCE at 164 to 166") {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(1);
                diagnostics.remove(diagnostics.size() - 1);
            }
        });
    }

    public void testMissingInTheMiddle() throws Exception {
        doTest(new TheTest("Unexpected NONE_APPLICABLE at 120 to 121", "Missing TYPE_MISMATCH at 159 to 167") {
            @Override
            protected void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(4);
                diagnostics.remove(diagnostics.size() - 3);
            }
        });
    }

    private static abstract class TheTest {
        private final String[] expected;

        protected TheTest(String... expectedMessages) {
            this.expected = expectedMessages;
        }

        public void test(final @NotNull PsiFile psiFile) {
            BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(
                    (JetFile) psiFile,Collections.<AnalyzerScriptParameter>emptyList())
                    .getBindingContext();

            String expectedText = CheckerTestUtil.addDiagnosticMarkersToText(psiFile, CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile)).toString();

            List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
            CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
            for (CheckerTestUtil.DiagnosedRange diagnosedRange : diagnosedRanges) {
                diagnosedRange.setFile(psiFile);
            }

            List<Diagnostic> diagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile);
            Collections.sort(diagnostics, CheckerTestUtil.DIAGNOSTIC_COMPARATOR);

            makeTestData(diagnostics, diagnosedRanges);

            List<String> expectedMessages = Lists.newArrayList(expected);
            final List<String> actualMessages = Lists.newArrayList();

            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, diagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {
                @NotNull
                @Override
                public PsiFile getFile() {
                    return psiFile;
                }

                @Override
                public void missingDiagnostic(String type, int expectedStart, int expectedEnd) {
                    String message = "Missing " + type + " at " + expectedStart + " to " + expectedEnd;
                    actualMessages.add(message);
                }

                @Override
                public void unexpectedDiagnostic(String type, int actualStart, int actualEnd) {
                    String message = "Unexpected " + type + " at " + actualStart + " to " + actualEnd;
                    actualMessages.add(message);
                }
            });

            assertEquals(listToString(expectedMessages), listToString(actualMessages));
        }

        private String listToString(List<String> expectedMessages) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String expectedMessage : expectedMessages) {
                stringBuilder.append(expectedMessage).append("\n");
            }
            return stringBuilder.toString();
        }

        protected abstract void makeTestData(List<Diagnostic> diagnostics, List<CheckerTestUtil.DiagnosedRange> diagnosedRanges);
    }

}
