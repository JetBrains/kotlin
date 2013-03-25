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
import com.google.common.io.Files;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractJetDiagnosticsTest extends JetLiteFixture {

    @Override
    protected JetCoreEnvironment createEnvironment() {
        File javaFilesDir = new File(FileUtil.getTempDirectory(), "java-files");
        try {
            JetTestUtils.mkdirs(javaFilesDir);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
        return new JetCoreEnvironment(
                getTestRootDisposable(),
                JetTestUtils.compilerConfigurationForTests(
                        ConfigurationKind.JDK_AND_ANNOTATIONS,
                        TestJdkKind.MOCK_JDK,
                        Arrays.asList(JetTestUtils.getAnnotationsJar()),
                        Arrays.asList(javaFilesDir)
                )
        );
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
                    public TestFile create(String fileName, String text) {
                        if (fileName.endsWith(".java")) {
                            writeJavaFile(fileName, text, javaFilesDir);
                        }
                        return new TestFile(fileName, text);
                    }
                });

        analyzeAndCheck(file, expectedText, testFiles);
    }

    protected abstract void analyzeAndCheck(File testDataFile, String expectedText, List<TestFile> files);

    protected static List<JetFile> getJetFiles(List<TestFile> testFiles) {
        List<JetFile> jetFiles = Lists.newArrayList();
        for (TestFile testFile : testFiles) {
            if (testFile.getJetFile() != null) {
                jetFiles.add(testFile.getJetFile());
            }
        }
        return jetFiles;
    }

    protected class TestFile {
        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        private final String expectedText;
        private final String clearText;
        private final JetFile jetFile;

        public TestFile(String fileName, String textWithMarkers) {
            if (fileName.endsWith(".java")) {
                PsiFileFactory.getInstance(getProject()).createFileFromText(fileName, JavaLanguage.INSTANCE, textWithMarkers);
                // TODO: check there's not syntax errors
                this.jetFile = null;
                this.expectedText = this.clearText = textWithMarkers;
            }
            else {
                expectedText = textWithMarkers;
                clearText = CheckerTestUtil.parseDiagnosedRanges(expectedText, diagnosedRanges);
                this.jetFile = createCheckAndReturnPsiFile(null, fileName, clearText);
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
            List<Diagnostic> diagnostics = CheckerTestUtil.getDiagnosticsIncludingSyntaxErrors(bindingContext, jetFile);
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

            actualText.append(CheckerTestUtil.addDiagnosticMarkersToText(jetFile, diagnostics));
            return ok[0];
        }

    }

}
