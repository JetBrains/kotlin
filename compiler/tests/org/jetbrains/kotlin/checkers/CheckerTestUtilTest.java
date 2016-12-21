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

package org.jetbrains.kotlin.checkers;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.checkers.CheckerTestUtil.ActualDiagnostic;
import org.jetbrains.kotlin.checkers.CheckerTestUtil.DiagnosedRange;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class CheckerTestUtilTest extends KotlinTestWithEnvironment {
    @NotNull
    private static String getTestDataPath() {
        return KotlinTestUtils.getTestDataPathBase() + "/diagnostics/checkerTestUtil";
    }

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    protected void doTest(TheTest theTest) throws Exception {
        String text = KotlinTestUtils.doLoadFile(getTestDataPath(), "test.kt");
        theTest.test(TestCheckerUtil.createCheckAndReturnPsiFile("test.kt", text, getProject()), getEnvironment());
    }

    public void testEquals() throws Exception {
        doTest(new TheTest() {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
            }
        });
    }

    public void testMissing() throws Exception {
        final DiagnosticData typeMismatch1 = diagnostics.get(1);
        doTest(new TheTest(missing(typeMismatch1)) {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
                diagnostics.remove(typeMismatch1.index);
            }
        });
    }

    public void testUnexpected() throws Exception {
        final DiagnosticData typeMismatch1 = diagnostics.get(1);
        doTest(new TheTest(unexpected(typeMismatch1)) {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(typeMismatch1.index);
            }
        });
    }

    public void testBoth() throws Exception {
        final DiagnosticData typeMismatch1 = diagnostics.get(1);
        final DiagnosticData unresolvedReference = diagnostics.get(6);
        doTest(new TheTest(unexpected(typeMismatch1), missing(unresolvedReference)) {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(typeMismatch1.rangeIndex);
                diagnostics.remove(unresolvedReference.index);
            }
        });
    }

    public void testMissingInTheMiddle() throws Exception {
        final DiagnosticData noneApplicable = diagnostics.get(4);
        final DiagnosticData typeMismatch3 = diagnostics.get(5);
        doTest(new TheTest(unexpected(noneApplicable), missing(typeMismatch3)) {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.remove(noneApplicable.rangeIndex);
                diagnostics.remove(typeMismatch3.index);
            }
        });
    }

    public void testWrongParameters() throws Exception {
        final DiagnosticData unused = diagnostics.get(2);
        String unusedDiagnostic = asTextDiagnostic(unused, "i");
        final DiagnosedRange range = asDiagnosticRange(unused, unusedDiagnostic);
        doTest(new TheTest(wrongParameters(unusedDiagnostic, "UNUSED_VARIABLE(a)", unused.startOffset, unused.endOffset)) {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.set(unused.rangeIndex, range);
            }
        });
    }

    public void testWrongParameterInMultiRange() throws Exception {
        final DiagnosticData unresolvedReference = diagnostics.get(6);
        String unusedDiagnostic = asTextDiagnostic(unresolvedReference, "i");
        String toManyArguments = asTextDiagnostic(diagnostics.get(7));
        final DiagnosedRange range = asDiagnosticRange(unresolvedReference, unusedDiagnostic, toManyArguments);
        doTest(new TheTest(wrongParameters(unusedDiagnostic, "UNRESOLVED_REFERENCE(xx)", unresolvedReference.startOffset, unresolvedReference.endOffset)) {
            @Override
            protected void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges) {
                diagnosedRanges.set(unresolvedReference.rangeIndex, range);
            }
        });
    }

    public void testAbstractJetDiagnosticsTest() throws Exception {
        AbstractDiagnosticsTest test = new AbstractDiagnosticsTest() {
            {setUp();}
        };
        test.doTest(getTestDataPath() + File.separatorChar + "test_with_diagnostic.kt");
    }

    private static abstract class TheTest {
        private final String[] expected;

        protected TheTest(String... expectedMessages) {
            this.expected = expectedMessages;
        }

        public void test(@NotNull PsiFile psiFile, @NotNull KotlinCoreEnvironment environment) {
            BindingContext bindingContext =
                    JvmResolveUtil.analyze((KtFile) psiFile, environment).getBindingContext();

            String expectedText = CheckerTestUtil.addDiagnosticMarkersToText(
                    psiFile,
                    CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile, false, null, null)
            ).toString();

            List<DiagnosedRange> diagnosedRanges = Lists.newArrayList();
            CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);

            List<ActualDiagnostic> actualDiagnostics =
                    CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, psiFile, false, null, null);
            Collections.sort(actualDiagnostics, CheckerTestUtil.DIAGNOSTIC_COMPARATOR);

            makeTestData(actualDiagnostics, diagnosedRanges);

            List<String> expectedMessages = Lists.newArrayList(expected);
            final List<String> actualMessages = Lists.newArrayList();

            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, actualDiagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {
                @Override
                public void missingDiagnostic(CheckerTestUtil.TextDiagnostic diagnostic, int expectedStart, int expectedEnd) {
                    actualMessages.add(missing(diagnostic.getDescription(), expectedStart, expectedEnd));
                }

                @Override
                public void wrongParametersDiagnostic(
                        CheckerTestUtil.TextDiagnostic expectedDiagnostic,
                        CheckerTestUtil.TextDiagnostic actualDiagnostic,
                        int start,
                        int end
                ) {
                    actualMessages.add(wrongParameters(expectedDiagnostic.asString(), actualDiagnostic.asString(), start, end));
                }

                @Override
                public void unexpectedDiagnostic(CheckerTestUtil.TextDiagnostic diagnostic, int actualStart, int actualEnd) {
                    actualMessages.add(unexpected(diagnostic.getDescription(), actualStart, actualEnd));
                }
            });

            assertEquals(listToString(expectedMessages), listToString(actualMessages));
        }

        private static String listToString(List<String> expectedMessages) {
            StringBuilder stringBuilder = new StringBuilder();
            for (String expectedMessage : expectedMessages) {
                stringBuilder.append(expectedMessage).append("\n");
            }
            return stringBuilder.toString();
        }

        protected abstract void makeTestData(List<ActualDiagnostic> diagnostics, List<DiagnosedRange> diagnosedRanges);
    }

    private static String wrongParameters(String expected, String actual, int start, int end) {
        return "Wrong parameters " + expected + " != " + actual +" at " + start + " to " + end;
    }

    private static String unexpected(String type, int actualStart, int actualEnd) {
        return "Unexpected " + type + " at " + actualStart + " to " + actualEnd;
    }

    private static String missing(String type, int expectedStart, int expectedEnd) {
        return "Missing " + type + " at " + expectedStart + " to " + expectedEnd;
    }

    private static String unexpected(DiagnosticData data) {
        return unexpected(data.name, data.startOffset, data.endOffset);
    }

    private static String missing(DiagnosticData data) {
        return missing(data.name, data.startOffset, data.endOffset);
    }

    private static String asTextDiagnostic(DiagnosticData diagnosticData, String... params) {
        return diagnosticData.name + "(" + StringUtil.join(params, "; ") + ")";
    }

    private static DiagnosedRange asDiagnosticRange(DiagnosticData diagnosticData, String... textDiagnostics) {
        DiagnosedRange range = new DiagnosedRange(diagnosticData.startOffset);
        range.setEnd(diagnosticData.endOffset);
        for (String textDiagnostic : textDiagnostics)
            range.addDiagnostic(textDiagnostic);
        return range;
    }

    private static class DiagnosticData {
        public int index;
        public int rangeIndex;
        public String name;
        public int startOffset;
        public int endOffset;

        private DiagnosticData(int index, int rangeIndex, String name, int startOffset, int endOffset) {
            this.index = index;
            this.rangeIndex = rangeIndex;
            this.name = name;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
        }
    }

    private final List<DiagnosticData> diagnostics = Lists.newArrayList(
            new DiagnosticData(0, 0, "UNUSED_PARAMETER", 8, 9),
            new DiagnosticData(1, 1, "CONSTANT_EXPECTED_TYPE_MISMATCH", 56, 57),
            new DiagnosticData(2, 2, "UNUSED_VARIABLE", 67, 68),
            new DiagnosticData(3, 3, "TYPE_MISMATCH", 98, 99),
            new DiagnosticData(4, 4, "NONE_APPLICABLE", 120, 121),
            new DiagnosticData(5, 5, "TYPE_MISMATCH", 159, 167),
            new DiagnosticData(6, 6, "UNRESOLVED_REFERENCE", 164, 166),
            new DiagnosticData(7, 6, "TOO_MANY_ARGUMENTS", 164, 166)
    );
}
