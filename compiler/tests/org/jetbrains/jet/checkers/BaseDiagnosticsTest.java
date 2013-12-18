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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.*;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaseDiagnosticsTest extends JetLiteFixture {

    public static final String DIAGNOSTICS_DIRECTIVE = "DIAGNOSTICS";
    public static final Pattern DIAGNOSTICS_PATTERN = Pattern.compile("([\\+\\-!])(\\w+)\\s*");
    public static final ImmutableSet<DiagnosticFactory> DIAGNOSTICS_TO_INCLUDE_ANYWAY =
            ImmutableSet.of(
                    Errors.UNRESOLVED_REFERENCE,
                    Errors.UNRESOLVED_REFERENCE_WRONG_RECEIVER,
                    CheckerTestUtil.SyntaxErrorDiagnosticFactory.INSTANCE,
                    CheckerTestUtil.DebugInfoDiagnosticFactory.ELEMENT_WITH_ERROR_TYPE,
                    CheckerTestUtil.DebugInfoDiagnosticFactory.MISSING_UNRESOLVED,
                    CheckerTestUtil.DebugInfoDiagnosticFactory.UNRESOLVED_WITH_TARGET
            );
    public static final String CHECK_TYPE_DIRECTIVE = "CHECK_TYPE";
    private static final String CHECK_TYPE_DECLARATIONS = "\nclass _<T>" +
                                                          "\nfun <T> T.checkType(f: (_<T>) -> Unit) = f";

    @Override
    protected JetCoreEnvironment createEnvironment() {
        File javaFilesDir = new File(FileUtil.getTempDirectory(), "java-files");
        try {
            JetTestUtils.mkdirs(javaFilesDir);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), JetTestUtils.compilerConfigurationForTests(
                        ConfigurationKind.JDK_AND_ANNOTATIONS,
                        TestJdkKind.MOCK_JDK,
                        Arrays.asList(JetTestUtils.getAnnotationsJar()),
                        Arrays.asList(javaFilesDir)
                ));
    }

    private static boolean writeJavaFile(@NotNull String fileName, @NotNull String content, @NotNull File javaFilesDir) {
        try {
            File javaFile = new File(javaFilesDir, fileName);
            JetTestUtils.mkdirs(javaFile.getParentFile());
            Files.write(content, javaFile, Charset.forName("utf-8"));
            return true;
        } catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    protected void doTest(String filePath) throws IOException {
        File file = new File(filePath);
        final File javaFilesDir = new File(FileUtil.getTempDirectory(), "java-files");

        String expectedText = JetTestUtils.doLoadFile(file);

        List<TestFile> testFiles =
                JetTestUtils.createTestFiles(file.getName(), expectedText, new JetTestUtils.TestFileFactory<TestFile>() {
                    @Override
                    public TestFile create(String fileName, String text, Map<String, String> directives) {
                        if (fileName.endsWith(".java")) {
                            writeJavaFile(fileName, text, javaFilesDir);
                        }
                        return new TestFile(fileName, text, directives);
                    }
                });

        analyzeAndCheck(file, testFiles);
    }

    protected abstract void analyzeAndCheck(
            File testDataFile,
            List<TestFile> files
    );

    protected static List<JetFile> getJetFiles(List<TestFile> testFiles) {
        List<JetFile> jetFiles = Lists.newArrayList();
        for (TestFile testFile : testFiles) {
            if (testFile.getJetFile() != null) {
                jetFiles.add(testFile.getJetFile());
            }
        }
        return jetFiles;
    }

    public static Condition<Diagnostic> parseDiagnosticFilterDirective(Map<String, String> directiveMap) {
        String directives = directiveMap.get(DIAGNOSTICS_DIRECTIVE);
        if (directives == null) {
            return Conditions.alwaysTrue();
        }
        Condition<Diagnostic> condition = Conditions.alwaysTrue();
        Matcher matcher = DIAGNOSTICS_PATTERN.matcher(directives);
        if (!matcher.find()) {
            Assert.fail("Wrong syntax in the '// DIAGNOSTICS: ...' directive:\n" +
                        "found: '" + directives + "'\n" +
                        "Must be '([+-!]DIAGNOSTIC_FACTORY_NAME|ERROR|WARNING|INFO)+'\n" +
                        "where '+' means 'include'\n" +
                        "      '-' means 'exclude'\n" +
                        "      '!' means 'exclude everything but this'\n" +
                        "directives are applied in the order of appearance, i.e. !FOO +BAR means inluce only FOO and BAR");
        }
        boolean first = true;
        do {
            String operation = matcher.group(1);
            final String name = matcher.group(2);

            Condition<Diagnostic> newCondition;
            if (ImmutableSet.of("ERROR", "WARNING", "INFO").contains(name)) {
                final Severity severity = Severity.valueOf(name);
                newCondition = new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return diagnostic.getSeverity() == severity;
                    }
                };
            }
            else {
                newCondition = new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return name.equals(diagnostic.getFactory().getName());
                    }
                };
            }
            if ("!".equals(operation)) {
                if (!first) {
                    Assert.fail("'" + operation + name + "' appears in a position rather than the first one, " +
                                "which effectively cancels all the previous filters in this directive");
                }
                condition = newCondition;
            }
            else if ("+".equals(operation)) {
                condition = Conditions.or(condition, newCondition);
            }
            else if ("-".equals(operation)) {
                condition = Conditions.and(condition, Conditions.not(newCondition));
            }
            first = false;
        }
        while (matcher.find());
        // We always include UNRESOLVED_REFERENCE and SYNTAX_ERROR because they are too likely to indicate erroneous test data
        return Conditions.or(
                condition,
                new Condition<Diagnostic>() {
                    @Override
                    public boolean value(Diagnostic diagnostic) {
                        return DIAGNOSTICS_TO_INCLUDE_ANYWAY.contains(diagnostic.getFactory());
                    }
                });
    }

    protected class TestFile {
        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        private final String expectedText;
        private final String clearText;
        private final JetFile jetFile;
        private final Condition<Diagnostic> whatDiagnosticsToConsider;
        private final boolean declareCheckType;

        public TestFile(
                String fileName,
                String textWithMarkers,
                Map<String, String> directives
        ) {
            this.whatDiagnosticsToConsider = parseDiagnosticFilterDirective(directives);
            this.declareCheckType = directives.containsKey(CHECK_TYPE_DIRECTIVE);
            if (fileName.endsWith(".java")) {
                PsiFileFactory.getInstance(getProject()).createFileFromText(fileName, JavaLanguage.INSTANCE, textWithMarkers);
                // TODO: check there's not syntax errors
                this.jetFile = null;
                this.expectedText = this.clearText = textWithMarkers;
            }
            else {
                expectedText = textWithMarkers;
                clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
                this.jetFile = createCheckAndReturnPsiFile(
                        null, fileName, declareCheckType ? clearText + CHECK_TYPE_DECLARATIONS : clearText);
                for (CheckerTestUtil.DiagnosedRange diagnosedRange : diagnosedRanges) {
                    diagnosedRange.setFile(jetFile);
                }
            }
        }


        @Nullable
        public JetFile getJetFile() {
            return jetFile;
        }

        public boolean getActualText(BindingContext bindingContext, StringBuilder actualText) {
            if (this.jetFile == null) {
                // TODO: check java files too
                actualText.append(this.clearText);
                return true;
            }

            final boolean[] ok = { true };
            List<Diagnostic> diagnostics = ContainerUtil.filter(
                    CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile),
                    whatDiagnosticsToConsider
            );
            CheckerTestUtil.diagnosticsDiff(diagnosedRanges, diagnostics, new CheckerTestUtil.DiagnosticDiffCallbacks() {

                @Override
                public void missingDiagnostic(String type, int expectedStart, int expectedEnd) {
                    String message = "Missing " + type + DiagnosticUtils.atLocation(jetFile, new TextRange(expectedStart, expectedEnd));
                    System.err.println(message);
                    ok[0] = false;
                }

                @Override
                public void unexpectedDiagnostic(String type, int actualStart, int actualEnd) {
                    String message = "Unexpected " + type + DiagnosticUtils.atLocation(jetFile, new TextRange(actualStart, actualEnd));
                    System.err.println(message);
                    ok[0] = false;
                }
            });

            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, diagnostics, new Function<PsiFile, String>() {
                @Override
                public String fun(PsiFile file) {
                    String text = file.getText();
                    return declareCheckType ? StringUtil.trimEnd(text, CHECK_TYPE_DECLARATIONS) : text;
                }
            }));
            return ok[0];
        }
    }
}
