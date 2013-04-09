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
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.NullableFunction;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.*;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticUtils;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;

public abstract class AbstractJetDiagnosticsTest extends KotlinTestWithEnvironmentManagement {

    public static final Set<String> KOTLIN_EXTENSIONS = ImmutableSet.of("kt", "ktscript");

    private TestCoreEnvironment createEnvironment(File javaFilesDir, Collection<File> kotlinSourceFiles) {
        try {
            JetTestUtils.mkdirs(javaFilesDir);
        }
        catch (IOException e) {
            throw ExceptionUtils.rethrow(e);
        }
        return new TestCoreEnvironment(
                getTestRootDisposable(),
                JetTestUtils.compilerConfigurationForTests(
                        ConfigurationKind.JDK_AND_ANNOTATIONS,
                        TestJdkKind.MOCK_JDK,
                        kotlinSourceFiles,
                        Arrays.asList(JetTestUtils.getAnnotationsJar()),
                        Arrays.asList(javaFilesDir)
                )
        );
    }

    private static File writeFile(@NotNull String fileName, @NotNull String content, @NotNull File dir) {
        try {
            File file = new File(dir, fileName);
            JetTestUtils.mkdirs(file.getParentFile());
            Files.write(content, file, Charset.forName("utf-8"));
            return file;
        }
        catch (Exception e) {
            throw ExceptionUtils.rethrow(e);
        }
    }

    protected void doTest(String filePath) throws IOException {
        File testDataFile = new File(filePath);

        String expectedText = JetTestUtils.doLoadFile(testDataFile);


        final TestEnvironment testEnvironment = new TestEnvironment(JetTestUtils.tmpDirForTest(this));
        JetTestUtils.createTestFiles(testDataFile.getName(), expectedText, new JetTestUtils.TestFileFactory<TestFile>() {
            @Override
            public TestFile create(String fileName, String text) {
                return testEnvironment.createTestFile(fileName, text);
            }
        });

        analyzeAndCheck(testDataFile, expectedText, testEnvironment);
    }

    protected abstract void analyzeAndCheck(File testDataFile, String expectedText, TestEnvironment testEnvironment);

    private static boolean isKotlinFile(String fileName) {
        return KOTLIN_EXTENSIONS.contains(FileUtil.getExtension(fileName));
    }

    protected class TestEnvironment {
        private TestCoreEnvironment coreEnvironment;
        private final File rootDir;
        private final Map<String, TestFile> testFiles = Maps.newLinkedHashMap();
        private int nameCount = 0;

        public TestEnvironment(@NotNull File rootDir) {
            this.rootDir = rootDir;
        }

        @NotNull
        public TestCoreEnvironment getCoreEnvironment() {
            if (coreEnvironment == null) {
                coreEnvironment = createEnvironment(rootDir, kotlinFiles(testFiles.values()));
            }
            return coreEnvironment;
        }

        private Collection<File> kotlinFiles(Collection<TestFile> testFiles) {
            return ContainerUtil.mapNotNull(testFiles, new NullableFunction<TestFile, File>() {
                @Nullable
                @Override
                public File fun(TestFile testFile) {
                    if (isKotlinFile(testFile.getFileName())) return testFile.getIoFile();
                    return null;
                }
            });
        }

        @NotNull
        public Project getProject() {
            return getCoreEnvironment().getProject();
        }

        @NotNull
        public Collection<TestFile> getTestFiles() {
            return testFiles.values();
        }

        public TestFile createTestFile(String name, String textWithMarkers) {
            File file = new File(name);
            if ("_".equals(FileUtil.getNameWithoutExtension(file))) {
                name = freshFileName(file);
            }
            assert !testFiles.containsKey(name) : "Duplicate file name: " + name + "Use '_.ext' to get a freshly generated name automatically";
            TestFile testFile = new TestFile(this, name, textWithMarkers);
            testFiles.put(name, testFile);
            writeFile(testFile.getFileName(), testFile.getTextWithoutMarkers(), rootDir);
            return testFile;
        }

        @NotNull
        private String freshFileName(@NotNull File file) {
            String freshName = (nameCount++) + "." + FileUtil.getExtension(file.getName());
            return new File(file.getParentFile(), freshName).getPath();
        }
    }

    protected class TestFile {
        private final List<CheckerTestUtil.DiagnosedRange> diagnosedRanges = Lists.newArrayList();
        private final String fileName;
        private final String expectedText;
        private final String textWithoutMarkers;
        private TestEnvironment testEnvironment;

        public TestFile(TestEnvironment testEnvironment, String fileName, String textWithMarkers) {
            this.testEnvironment = testEnvironment;
            this.fileName = fileName;
            this.expectedText = textWithMarkers;
            this.textWithoutMarkers = CheckerTestUtil.parseDiagnosedRanges(textWithMarkers, diagnosedRanges);
        }

        public String getFileName() {
            return fileName;
        }

        public String getTextWithoutMarkers() {
            return textWithoutMarkers;
        }

        public String getExpectedText() {
            return expectedText;
        }

        public boolean getActualText(final JetFile jetFile, List<Diagnostic> diagnostics, StringBuilder actualText) {
            final boolean[] ok = { true };
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

        @NotNull
        public PsiFile getPsiFile() {
            File ioFile = getIoFile();
            VirtualFile virtualFile = testEnvironment.getCoreEnvironment().getVirtualFileSystem().findFileByIoFile(ioFile);
            assert virtualFile != null : "Virtual file not found: " + ioFile;

            return PsiManager.getInstance(testEnvironment.getProject()).findFile(virtualFile);
        }

        @NotNull
        public File getIoFile() {
            return new File(testEnvironment.rootDir, getFileName());
        }
    }
}
