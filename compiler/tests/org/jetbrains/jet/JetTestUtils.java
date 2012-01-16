package org.jetbrains.jet;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.JetCoreEnvironment;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.UnresolvedReferenceDiagnostic;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacade;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.SlicedMap;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author abreslav
 */
public class JetTestUtils {
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
            if (slice == BindingContext.PROCESSED) return (V) Boolean.FALSE;
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
            if (diagnostic instanceof UnresolvedReferenceDiagnostic) {
                UnresolvedReferenceDiagnostic unresolvedReferenceDiagnostic = (UnresolvedReferenceDiagnostic) diagnostic;
                throw new IllegalStateException("Unresolved: " + unresolvedReferenceDiagnostic.getPsiElement().getText());
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
                        throw new IllegalStateException(diagnostic.getMessage());
                    }
                }
            };

    public static BindingContext analyzeFile(@NotNull JetFile namespace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        return AnalyzerFacade.analyzeOneFileWithJavaIntegration(namespace, flowDataTraceFactory);
    }


    public static JetCoreEnvironment createEnvironmentWithMockJdk(Disposable disposable) {
        JetCoreEnvironment environment = new JetCoreEnvironment(disposable);
        final File rtJar = new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
        environment.addToClasspath(rtJar);
        environment.addToClasspath(new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
        return environment;
    }


    public static void mkdirs(File file) throws IOException {
        if (file.isDirectory()) {
            return;
        }
        if (!file.mkdirs()) {
            throw new IOException();
        }
    }

    public static void rmrf(File file) {
        if (file != null) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    rmrf(child);
                }
            }
            file.delete();
        }
    }
    
    public static File tmpDirForTest(TestCase test) {
        return new File("tmp/" + test.getClass().getSimpleName() + "/" + test.getName());
    }

    public static void recreateDirectory(File file) throws IOException {
        rmrf(file);
        mkdirs(file);
    }

    public static final Pattern FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);
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
            assert processedChars == expectedText.length() : "Characters skipped from " + processedChars + " to " + (expectedText.length() - 1);
        }
        return testFileFiles;
    }
}
