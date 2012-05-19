/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.UnresolvedReferenceDiagnosticFactory;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.SlicedMap;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class JetTestUtils {
    private static List<File> filesToDelete = new ArrayList<File>();

    public static final BindingTrace DUMMY_TRACE = new BindingTrace() {


        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {

                @Override
                public Collection<Diagnostic> getDiagnostics() {
                    throw new UnsupportedOperationException(); // TODO
                }

                @Override
                public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                    return DUMMY_TRACE.get(slice, key);
                }

                @NotNull
                @Override
                public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                    return DUMMY_TRACE.getKeys(slice);
                }
            };
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            if (slice == BindingContext.PROCESSED) return (V)Boolean.FALSE;
            return SlicedMap.DO_NOTHING.get(slice, key);
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            assert slice.isCollective();
            return Collections.emptySet();
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getFactory() instanceof UnresolvedReferenceDiagnosticFactory) {
                throw new IllegalStateException("Unresolved: " + diagnostic.getPsiElement().getText());
            }
        }
    };

    public static BindingTrace DUMMY_EXCEPTION_ON_ERROR_TRACE = new BindingTrace() {
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {
                @Override
                public Collection<Diagnostic> getDiagnostics() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.get(slice, key);
                }

                @NotNull
                @Override
                public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.getKeys(slice);
                }
            };
        }

        @Override
        public <K, V> void record(WritableSlice<K, V> slice, K key, V value) {
        }

        @Override
        public <K> void record(WritableSlice<K, Boolean> slice, K key) {
        }

        @Override
        public <K, V> V get(ReadOnlySlice<K, V> slice, K key) {
            return null;
        }

        @NotNull
        @Override
        public <K, V> Collection<K> getKeys(WritableSlice<K, V> slice) {
            assert slice.isCollective();
            return Collections.emptySet();
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                throw new IllegalStateException(DefaultErrorMessages.RENDERER.render(diagnostic));
            }
        }
    };

    private JetTestUtils() {
    }

    public static AnalyzeExhaust analyzeFile(@NotNull JetFile namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(namespace, flowDataTraceFactory,
                                                                      CompileCompilerDependenciesTest
                                                                              .compilerDependenciesForTests(CompilerSpecialMode.REGULAR,
                                                                                                            true));
    }

    public static JetCoreEnvironment createEnvironmentWithMockJdk(Disposable disposable) {
        return createEnvironmentWithMockJdk(disposable, CompilerSpecialMode.REGULAR);
    }

    public static JetCoreEnvironment createEnvironmentWithMockJdk(Disposable disposable, @NotNull CompilerSpecialMode compilerSpecialMode) {
        return JetCoreEnvironment.getCoreEnvironmentForJVM(disposable, CompileCompilerDependenciesTest
                .compilerDependenciesForTests(compilerSpecialMode, true));
    }

    public static File findMockJdkRtJar() {
        return new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
    }

    public static File getAnnotationsJar() {
        return new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar");
    }


    public static void mkdirs(File file) throws IOException {
        if (file.isDirectory()) {
            return;
        }
        if (!file.mkdirs()) {
            if (file.exists()) {
                throw new IOException("failed to create " + file + " file exists and not a directory");
            }
            throw new IOException();
        }
    }

    public static File tmpDirForTest(TestCase test) throws IOException {
        File answer = FileUtil.createTempDirectory(test.getClass().getSimpleName(), test.getName());
        deleteOnShutdown(answer);
        return answer;
    }

    public static File tmpDir(String name) throws IOException {
        // we should use this form. otherwise directory will be deleted on each test
        File answer = FileUtil.createTempDirectory(new File(System.getProperty("java.io.tmpdir")), name, "");
        deleteOnShutdown(answer);
        return answer;
    }

    public static void deleteOnShutdown(File file) {
        if (filesToDelete.isEmpty()) {
            ShutDownTracker.getInstance().registerShutdownTask(new Runnable() {
                @Override
                public void run() {
                    ShutDownTracker.invokeAndWait(true, true, new Runnable() {
                        @Override
                        public void run() {
                            for (File victim : filesToDelete) {
                                FileUtil.delete(victim);
                            }
                        }
                    });
                }
            });
        }

        filesToDelete.add(file);
    }

    public static final Pattern FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);

    public static JetCoreEnvironment createEnvironmentWithFullJdk(Disposable disposable) {
        return new JetCoreEnvironment(disposable,
                CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.REGULAR, false));
    }

    public interface TestFileFactory<F> {
        F create(String fileName, String text);
    }

    public static <F> List<F> createTestFiles(String testFileName, String expectedText, TestFileFactory<F> factory) {
        List<F> testFileFiles = Lists.newArrayList();
        Matcher matcher = FILE_PATTERN.matcher(expectedText);
        if (!matcher.find()) {
            // One file
            testFileFiles.add(factory.create(testFileName, expectedText));
        }
        else {
            int processedChars = 0;
            // Many files
            while (true) {
                String fileName = matcher.group(1);
                int start = matcher.start();
                assert start == processedChars : "Characters skipped from " + processedChars + " to " + matcher.start();

                boolean nextFileExists = matcher.find();
                int end;
                if (nextFileExists) {
                    end = matcher.start();
                }
                else {
                    end = expectedText.length();
                }
                String fileText = expectedText.substring(start, end);
                processedChars = end;

                testFileFiles.add(factory.create(fileName, fileText));

                if (!nextFileExists) break;
            }
            assert processedChars == expectedText.length() : "Characters skipped from " +
                                                             processedChars +
                                                             " to " +
                                                             (expectedText.length() - 1);
        }
        return testFileFiles;
    }

    public static String getLastCommentedLines(@NotNull Document document) {
        List<CharSequence> resultLines = new ArrayList<CharSequence>();
        for (int i = document.getLineCount() - 1; i >= 0; i--) {
            int lineStart = document.getLineStartOffset(i);
            int lineEnd = document.getLineEndOffset(i);
            if (document.getCharsSequence().subSequence(lineStart, lineEnd).toString().trim().isEmpty()) {
                continue;
            }

            if ("//".equals(document.getCharsSequence().subSequence(lineStart, lineStart + 2).toString())) {
                resultLines.add(document.getCharsSequence().subSequence(lineStart + 2, lineEnd));
            }
            else {
                break;
            }
        }
        Collections.reverse(resultLines);
        StringBuilder result = new StringBuilder();
        for (CharSequence line : resultLines) {
            result.append(line).append("\n");
        }
        result.delete(result.length() - 1, result.length());
        return result.toString();
    }
}
