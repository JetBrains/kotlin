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

package org.jetbrains.jet;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.MutablePackageFragmentDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.diagnostics.Severity;
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;
import org.jetbrains.jet.util.slicedmap.ReadOnlySlice;
import org.jetbrains.jet.util.slicedmap.SlicedMap;
import org.jetbrains.jet.util.slicedmap.WritableSlice;
import org.jetbrains.jet.utils.PathUtil;
import org.junit.Assert;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.jet.ConfigurationKind.ALL;
import static org.jetbrains.jet.ConfigurationKind.JDK_AND_ANNOTATIONS;
import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.ANNOTATIONS_PATH_KEY;
import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.CLASSPATH_KEY;

public class JetTestUtils {
    private static final Pattern KT_FILES = Pattern.compile(".*?.kt");
    private static List<File> filesToDelete = new ArrayList<File>();

    public static final Pattern FILE_PATTERN = Pattern.compile("//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);
    public static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^//\\s*!(\\w+):\\s*(.*)$", Pattern.MULTILINE);

    public static final BindingTrace DUMMY_TRACE = new BindingTrace() {


        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {

                @NotNull
                @Override
                public Diagnostics getDiagnostics() {
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

                @NotNull
                @TestOnly
                @Override
                public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
                    return ImmutableMap.of();
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
            if (Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS.contains(diagnostic.getFactory())) {
                throw new IllegalStateException("Unresolved: " + diagnostic.getPsiElement().getText());
            }
        }
    };

    public static BindingTrace DUMMY_EXCEPTION_ON_ERROR_TRACE = new BindingTrace() {
        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {
                @NotNull
                @Override
                public Diagnostics getDiagnostics() {
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

                @NotNull
                @TestOnly
                @Override
                public <K, V> ImmutableMap<K, V> getSliceContents(@NotNull ReadOnlySlice<K, V> slice) {
                    return ImmutableMap.of();
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

    @SuppressWarnings("unchecked")
    private static final Class<? extends TestCase>[] NO_INNER_CLASSES = new Class[0];

    private JetTestUtils() {
    }

    public static AnalyzeExhaust analyzeFile(@NotNull JetFile namespace) {
        return AnalyzerFacadeForJVM.analyzeOneFileWithJavaIntegration(namespace, Collections.<AnalyzerScriptParameter>emptyList());
    }

    @NotNull
    public static JetCoreEnvironment createEnvironmentWithFullJdk(Disposable disposable) {
        return createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(disposable,
                                                                         ConfigurationKind.ALL, TestJdkKind.FULL_JDK);
    }

    @NotNull
    public static JetCoreEnvironment createEnvironmentWithMockJdkAndIdeaAnnotations(Disposable disposable) {
        return createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, ConfigurationKind.ALL);
    }

    @NotNull
    public static JetCoreEnvironment createEnvironmentWithMockJdkAndIdeaAnnotations(Disposable disposable, @NotNull ConfigurationKind configurationKind) {
        return createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(disposable, configurationKind, TestJdkKind.MOCK_JDK);
    }

    @NotNull
    public static JetCoreEnvironment createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind
    ) {
        return JetCoreEnvironment.createForTests(disposable, compilerConfigurationForTests(
                configurationKind, jdkKind, getAnnotationsJar()));
    }

    public static File findMockJdkRtJar() {
        return new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/rt.jar");
    }

    public static File getAnnotationsJar() {
        return new File(JetTestCaseBuilder.getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/annotations.jar");
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

    @NotNull
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

    public static void rmrf(File file) {
        if (file == null) {
            return;
        }
        if (!FileUtil.delete(file)) {
            throw new RuntimeException("failed to delete " + file);
        }
    }

    public static JetFile createFile(@NonNls String name, String text, @NotNull Project project) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return (JetFile) ((PsiFileFactoryImpl) PsiFileFactory.getInstance(project)).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
    }

    public static String doLoadFile(String myFullDataPath, String name) throws IOException {
        String fullName = myFullDataPath + File.separatorChar + name;
        return doLoadFile(new File(fullName));
    }

    public static String doLoadFile(@NotNull File file) throws IOException {
        return FileUtil.loadFile(file, CharsetToolkit.UTF8, true).trim();
    }

    public static String getFilePath(File file) {
        return FileUtil.toSystemIndependentName(file.getPath());
    }

    @NotNull
    public static CompilerConfiguration compilerConfigurationForTests(@NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind, File... extraClasspath) {
        return compilerConfigurationForTests(configurationKind, jdkKind, Arrays.asList(extraClasspath), Collections.<File>emptyList());
    }

    @NotNull
    public static CompilerConfiguration compilerConfigurationForTests(@NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind, @NotNull Collection<File> extraClasspath, @NotNull Collection<File> priorityClasspath) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addAll(CLASSPATH_KEY, priorityClasspath);
        configuration.add(CLASSPATH_KEY, jdkKind == TestJdkKind.MOCK_JDK ? findMockJdkRtJar() : PathUtil.findRtJar());
        if (configurationKind == ALL) {
            configuration.add(CLASSPATH_KEY, ForTestCompileRuntime.runtimeJarForTests());
        }
        configuration.addAll(CLASSPATH_KEY, extraClasspath);

        if (configurationKind == ALL || configurationKind == JDK_AND_ANNOTATIONS) {
            configuration.add(ANNOTATIONS_PATH_KEY, ForTestPackJdkAnnotations.jdkAnnotationsForTests());
        }

        return configuration;
    }

    public static void newTrace(@NotNull JetCoreEnvironment environment) {
        // let the next analysis use another trace
        CliLightClassGenerationSupport.getInstanceForCli(environment.getProject()).newBindingTrace();
    }

    public static void resolveAllKotlinFiles(JetCoreEnvironment environment) throws IOException {
        List<String> paths = environment.getConfiguration().get(CommonConfigurationKeys.SOURCE_ROOTS_KEY);
        assert paths != null;
        List<JetFile> jetFiles = Lists.newArrayList();
        for (String path : paths) {
            jetFiles.add(loadJetFile(environment.getProject(), new File(path)));
        }
        LazyResolveTestUtil.resolveEagerly(jetFiles, environment);
    }

    @NotNull
    public static List<File> collectKtFiles(@NotNull File root) {
        List<File> files = Lists.newArrayList();
        FileUtil.collectMatchedFiles(root, KT_FILES, files);
        return files;
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual) {
        try {
            if (!expectedFile.exists()) {
                FileUtil.writeToFile(expectedFile, actual);
                Assert.fail("Expected data file did not exist. Generating: " + expectedFile);
            }
            String expected = FileUtil.loadFile(expectedFile, CharsetToolkit.UTF8, true);

            // compare with hard copy: make sure nothing is lost in output
            String expectedText = StringUtil.convertLineSeparators(expected.trim());
            String actualText = StringUtil.convertLineSeparators(actual.trim());
            if (!Comparing.equal(expectedText, actualText)) {
                throw new FileComparisonFailure("Actual data differs from file content: " + expectedFile.getName(),
                                                expected, actual, expectedFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface TestFileFactory<F> {
        F create(String fileName, String text, Map<String, String> directives);
    }

    public static <F> List<F> createTestFiles(String testFileName, String expectedText, TestFileFactory<F> factory) {
        Map<String, String> directives = parseDirectives(expectedText);

        List<F> testFiles = Lists.newArrayList();
        Matcher matcher = FILE_PATTERN.matcher(expectedText);
        if (!matcher.find()) {
            // One file
            testFiles.add(factory.create(testFileName, expectedText, directives));
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

                testFiles.add(factory.create(fileName, fileText, directives));

                if (!nextFileExists) break;
            }
            assert processedChars == expectedText.length() : "Characters skipped from " +
                                                             processedChars +
                                                             " to " +
                                                             (expectedText.length() - 1);
        }
        return testFiles;
    }

    @NotNull
    public static Map<String, String> parseDirectives(String expectedText) {
        Map<String, String> directives = Maps.newHashMap();
        Matcher directiveMatcher = DIRECTIVE_PATTERN.matcher(expectedText);
        int start = 0;
        while (directiveMatcher.find()) {
            if (directiveMatcher.start() != start) {
                Assert.fail("Directives should only occur at the beginning of a file: " + directiveMatcher.group());
            }
            String name = directiveMatcher.group(1);
            String value = directiveMatcher.group(2);
            String oldValue = directives.put(name, value);
            Assert.assertNull("Directive overwritten: " + name + " old value: " + oldValue + " new value: " + value, oldValue);
            start = directiveMatcher.end() + 1;
        }
        return directives;
    }

    public static List<String> loadBeforeAfterText(String filePath) {
        String content;

        try {
            content = StringUtil.convertLineSeparators(FileUtil.loadFile(new File(filePath)));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> files = createTestFiles("", content, new TestFileFactory<String>() {
            @Override
            public String create(String fileName, String text, Map<String, String> directives) {
                int firstLineEnd = text.indexOf('\n');
                return StringUtil.trimTrailing(text.substring(firstLineEnd + 1));
            }
        });

        Assert.assertTrue("Exactly two files expected: ", files.size() == 2);

        return files;
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

    public static void compileJavaFiles(@NotNull Collection<File> files, List<String> options) throws IOException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(diagnosticCollector, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(files);

            JavaCompiler.CompilationTask task = javaCompiler.getTask(
                    new StringWriter(), // do not write to System.err
                    fileManager,
                    diagnosticCollector,
                    options,
                    null,
                    javaFileObjectsFromFiles);

            Boolean success = task.call(); // do NOT inline this variable, call() should complete before errorsToString()
            Assert.assertTrue(errorsToString(diagnosticCollector), success);
        } finally {
            fileManager.close();
        }
    }

    private static String errorsToString(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
        StringBuilder builder = new StringBuilder();
        for (javax.tools.Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            if (diagnostic.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
                builder.append(diagnostic).append("\n");
            }
        }
        return builder.toString();
    }

    public static void assertAllTestsPresentByMetadata(
            @NotNull Class<?> testCaseClass,
            @NotNull String generatorClassFqName,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            boolean recursive
    ) {
        TestMetadata testClassMetadata = testCaseClass.getAnnotation(TestMetadata.class);
        Assert.assertNotNull("No metadata for class: " + testCaseClass, testClassMetadata);
        String rootPath = testClassMetadata.value();
        File rootFile = new File(rootPath);

        Set<String> filePaths = collectPathsMetadata(testCaseClass);

        File[] files = testDataDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (recursive && containsTestData(file, filenamePattern)) {
                        assertTestClassPresentByMetadata(testCaseClass, generatorClassFqName, file);
                    }
                }
                else if (filenamePattern.matcher(file.getName()).matches()) {
                    assertFilePathPresent(file, rootFile, filePaths, generatorClassFqName);
                }
            }
        }
    }

    public static void assertAllTestsPresentInSingleGeneratedClass(
            @NotNull Class<?> testCaseClass,
            @NotNull final String generatorClassFqName,
            @NotNull File testDataDir,
            @NotNull final Pattern filenamePattern) {
        TestMetadata testClassMetadata = testCaseClass.getAnnotation(TestMetadata.class);
        Assert.assertNotNull("No metadata for class: " + testCaseClass, testClassMetadata);
        String rootPath = testClassMetadata.value();
        final File rootFile = new File(rootPath);

        final Set<String> filePaths = collectPathsMetadata(testCaseClass);

        FileUtil.processFilesRecursively(testDataDir, new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.isFile() && filenamePattern.matcher(file.getName()).matches()) {
                    assertFilePathPresent(file, rootFile, filePaths, generatorClassFqName);
                }

                return true;
            }
        });
    }

    private static void assertFilePathPresent(File file, File rootFile, Set<String> filePaths, String generatorClassFqName) {
        String path = FileUtil.getRelativePath(rootFile, file);
        if (path != null) {
            String relativePath = FileUtil.nameToCompare(path);
            if (!filePaths.contains(relativePath)) {
                Assert.fail("Test data file missing from the generated test class: " +
                            file +
                            pleaseReRunGenerator(generatorClassFqName));
            }
        }
    }

    private static Set<String> collectPathsMetadata(Class<?> testCaseClass) {
        return ContainerUtil.newHashSet(
                ContainerUtil.map(collectMethodsMetadata(testCaseClass), new Function<String, String>() {
                    @Override
                    public String fun(String pathData) {
                        return FileUtil.nameToCompare(pathData);
                    }
                }));
    }

    private static Set<String> collectMethodsMetadata(Class<?> testCaseClass) {
        Set<String> filePaths = Sets.newHashSet();
        for (Method method : testCaseClass.getDeclaredMethods()) {
            TestMetadata testMetadata = method.getAnnotation(TestMetadata.class);
            if (testMetadata != null) {
                filePaths.add(testMetadata.value());
            }
        }
        return filePaths;
    }

    private static boolean containsTestData(File dir, Pattern filenamePattern) {
        File[] files = dir.listFiles();
        assert files != null;
        for (File file : files) {
            if (file.isDirectory()) {
                if (containsTestData(file, filenamePattern)) {
                    return true;
                }
            }
            else {
                if (filenamePattern.matcher(file.getName()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void assertTestClassPresentByMetadata(
            @NotNull Class<?> outerClass,
            @NotNull String generatorClassFqName,
            @NotNull File testDataDir
    ) {
        InnerTestClasses innerClassesAnnotation = outerClass.getAnnotation(InnerTestClasses.class);
        Class<? extends TestCase>[] innerClasses = innerClassesAnnotation == null ? NO_INNER_CLASSES : innerClassesAnnotation.value();
        for (Class<?> innerClass : innerClasses) {
            TestMetadata testMetadata = innerClass.getAnnotation(TestMetadata.class);
            if (testMetadata != null && testMetadata.value().equals(getFilePath(testDataDir))) {
                return;
            }
        }
        Assert.fail("Test data directory missing from the generated test class: " +
                    testDataDir +
                    pleaseReRunGenerator(generatorClassFqName));
    }

    private static String pleaseReRunGenerator(String generatorClassFqName) {
        return "\nPlease re-run the generator: " + generatorClassFqName +
               getLocationFormattedForConsole(generatorClassFqName);
    }

    private static String getLocationFormattedForConsole(String generatorClassFqName) {
        return "(" + getSimpleName(generatorClassFqName) + ".java:1)";
    }

    private static String getSimpleName(String generatorClassFqName) {
        return generatorClassFqName.substring(generatorClassFqName.lastIndexOf(".") + 1);
    }

    public static JetFile loadJetFile(@NotNull Project project, @NotNull File ioFile) throws IOException {
        String text = FileUtil.loadFile(ioFile);
        return JetPsiFactory.createPhysicalFile(project, ioFile.getName(), text);
    }

    @NotNull
    public static List<JetFile> loadToJetFiles(
            @NotNull JetCoreEnvironment environment,
            @NotNull List<File> files
    ) throws IOException {
        List<JetFile> jetFiles = Lists.newArrayList();
        for (File file : files) {
            jetFiles.add(loadJetFile(environment.getProject(), file));
        }
        return jetFiles;
    }

    public static ModuleDescriptorImpl createEmptyModule() {
        return createEmptyModule("<empty-for-test>");
    }

    public static ModuleDescriptorImpl createEmptyModule(@NotNull String name) {
        return new ModuleDescriptorImpl(Name.special(name), Collections.<ImportPath>emptyList(), PlatformToKotlinClassMap.EMPTY);
    }

    @NotNull
    public static MutablePackageFragmentDescriptor createTestPackageFragment(@NotNull Name testPackageName) {
        return createTestPackageFragment(testPackageName, "<test module>");
    }

    @NotNull
    public static MutablePackageFragmentDescriptor createTestPackageFragment(@NotNull Name testPackageName, @NotNull String moduleName) {
        ModuleDescriptorImpl module = AnalyzerFacadeForJVM.createJavaModule(moduleName);
        MutablePackageFragmentProvider provider = new MutablePackageFragmentProvider(module);
        module.addFragmentProvider(provider);
        return provider.getOrCreateFragment(FqName.topLevel(testPackageName));
    }

    @NotNull
    public static File replaceExtension(@NotNull File file, @Nullable String newExtension) {
        return new File(file.getParentFile(), FileUtil.getNameWithoutExtension(file) + (newExtension == null ? "" : "." + newExtension));
    }
}
