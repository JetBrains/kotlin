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

package org.jetbrains.kotlin.test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.ShutDownTracker;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.util.Function;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRoot;
import org.jetbrains.kotlin.config.KotlinSourceRoot;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.diagnostics.Diagnostic;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.diagnostics.Severity;
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.TargetPlatform;
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.test.util.JetTestUtilsKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo;
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice;
import org.jetbrains.kotlin.util.slicedMap.SlicedMap;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.kotlin.utils.PathUtil;
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

import static org.jetbrains.kotlin.config.JVMConfigurationKeys.MODULE_NAME;
import static org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.compileKotlinToDirAndGetAnalysisResult;
import static org.jetbrains.kotlin.test.ConfigurationKind.ALL;

public class KotlinTestUtils {
    public static final String TEST_GENERATOR_NAME = "org.jetbrains.kotlin.generators.tests.TestsPackage";
    public static final String PLEASE_REGENERATE_TESTS = "Please regenerate tests (GenerateTests.kt)";

    private static final List<File> filesToDelete = new ArrayList<File>();

    /**
     * Syntax:
     *
     * // MODULE: name(dependency1, dependency2, ...)
     *
     * // FILE: name
     *
     * Several files may follow one module
     */
    public static final Pattern FILE_OR_MODULE_PATTERN = Pattern.compile("(?://\\s*MODULE:\\s*(\\w+)(\\(\\w+(?:, \\w+)*\\))?\\s*)?" +
                                                                         "//\\s*FILE:\\s*(.*)$", Pattern.MULTILINE);
    public static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^//\\s*!(\\w+)(:\\s*(.*)$)?", Pattern.MULTILINE);

    public static final BindingTrace DUMMY_TRACE = new BindingTrace() {
        @NotNull
        @Override
        public BindingContext getBindingContext() {
            return new BindingContext() {

                @NotNull
                @Override
                public Diagnostics getDiagnostics() {
                    return Diagnostics.Companion.getEMPTY();
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

                @Nullable
                @Override
                public KotlinType getType(@NotNull KtExpression expression) {
                    return DUMMY_TRACE.getType(expression);
                }

                @Override
                public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
                    // do nothing
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
        @SuppressWarnings("unchecked")
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

        @Nullable
        @Override
        public KotlinType getType(@NotNull KtExpression expression) {
            KotlinTypeInfo typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression);
            return typeInfo != null ? typeInfo.getType() : null;
        }

        @Override
        public void recordType(@NotNull KtExpression expression, @Nullable KotlinType type) {
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

                @Nullable
                @Override
                public KotlinType getType(@NotNull KtExpression expression) {
                    return DUMMY_EXCEPTION_ON_ERROR_TRACE.getType(expression);
                }

                @Override
                public void addOwnDataTo(@NotNull BindingTrace trace, boolean commitDiagnostics) {
                    // do nothing
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

        @Nullable
        @Override
        public KotlinType getType(@NotNull KtExpression expression) {
            return null;
        }

        @Override
        public void recordType(@NotNull KtExpression expression, @Nullable KotlinType type) {
        }

        @Override
        public void report(@NotNull Diagnostic diagnostic) {
            if (diagnostic.getSeverity() == Severity.ERROR) {
                throw new IllegalStateException(DefaultErrorMessages.render(diagnostic));
            }
        }
    };

    // We suspect sequences of eight consecutive hexadecimal digits to be a package part hash code
    private static final Pattern STRIP_PACKAGE_PART_HASH_PATTERN = Pattern.compile("\\$([0-9a-f]{8})");

    private KotlinTestUtils() {
    }

    @NotNull
    public static AnalysisResult analyzeFile(@NotNull KtFile file, @NotNull KotlinCoreEnvironment environment) {
        return JvmResolveUtil.analyzeOneFileWithJavaIntegration(file, environment);
    }

    @NotNull
    public static KotlinCoreEnvironment createEnvironmentWithMockJdkAndIdeaAnnotations(Disposable disposable) {
        return createEnvironmentWithMockJdkAndIdeaAnnotations(disposable, ConfigurationKind.ALL);
    }

    @NotNull
    public static KotlinCoreEnvironment createEnvironmentWithMockJdkAndIdeaAnnotations(Disposable disposable, @NotNull ConfigurationKind configurationKind) {
        return createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(disposable, configurationKind, TestJdkKind.MOCK_JDK);
    }

    @NotNull
    public static KotlinCoreEnvironment createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
            @NotNull Disposable disposable,
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind
    ) {
        return KotlinCoreEnvironment.createForTests(
                disposable,
                compilerConfigurationForTests(configurationKind, jdkKind, getAnnotationsJar()),
                EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    @NotNull
    public static String getTestDataPathBase() {
        return getHomeDirectory() + "/compiler/testData";
    }

    @NotNull
    public static String getHomeDirectory() {
        File resourceRoot = PathUtil.getResourcePathForClass(KotlinTestUtils.class);
        return FileUtil.toSystemIndependentName(resourceRoot.getParentFile().getParentFile().getParent());
    }

    public static File findMockJdkRtJar() {
        return new File(getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/rt.jar");
    }

    // Differs from common mock JDK only by one additional 'nonExistingMethod' in Collection and constructor from Double in Throwable
    // It's needed to test the way we load additional built-ins members that neither in black nor white lists
    public static File findMockJdkRtModified() {
        return new File(getHomeDirectory(), "compiler/testData/mockJDKModified/rt.jar");
    }

    public static File findAndroidApiJar() {
        return new File(getHomeDirectory(), "dependencies/android.jar");
    }

    public static File getAnnotationsJar() {
        return new File(getHomeDirectory(), "compiler/testData/mockJDK/jre/lib/annotations.jar");
    }

    public static void mkdirs(@NotNull File file) {
        if (file.isDirectory()) {
            return;
        }
        if (!file.mkdirs()) {
            if (file.exists()) {
                throw new IllegalStateException("Failed to create " + file + ": file exists and not a directory");
            }
            throw new IllegalStateException("Failed to create " + file);
        }
    }

    @NotNull
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

    @NotNull
    public static KtFile createFile(@NotNull @NonNls final String name, @NotNull String text, @NotNull Project project) {
        String shortName = name.substring(name.lastIndexOf('/') + 1);
        shortName = shortName.substring(shortName.lastIndexOf('\\') + 1);
        LightVirtualFile virtualFile = new LightVirtualFile(shortName, KotlinLanguage.INSTANCE, text) {
            @NotNull
            @Override
            public String getPath() {
                //TODO: patch LightVirtualFile
                return "/" + name;
            }
        };

        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        PsiFileFactoryImpl factory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(project);
        //noinspection ConstantConditions
        return (KtFile) factory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, true, false);
    }

    public static String doLoadFile(String myFullDataPath, String name) throws IOException {
        String fullName = myFullDataPath + File.separatorChar + name;
        return doLoadFile(new File(fullName));
    }

    public static String doLoadFile(@NotNull File file) throws IOException {
        return FileUtil.loadFile(file, CharsetToolkit.UTF8, true);
    }

    public static String getFilePath(File file) {
        return FileUtil.toSystemIndependentName(file.getPath());
    }

    @NotNull
    public static CompilerConfiguration compilerConfigurationForTests(
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull File... extraClasspath
    ) {
        return compilerConfigurationForTests(configurationKind, jdkKind, Arrays.asList(extraClasspath), Collections.<File>emptyList());
    }

    @NotNull
    public static CompilerConfiguration compilerConfigurationForTests(
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<File> classpath,
            @NotNull List<File> javaSource
    ) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        JvmContentRootsKt.addJavaSourceRoots(configuration, javaSource);
        if (jdkKind == TestJdkKind.MOCK_JDK) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, findMockJdkRtJar());
        }
        else if (jdkKind == TestJdkKind.MODIFIED_MOCK_JDK) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, findMockJdkRtModified());
        }
        else if (jdkKind == TestJdkKind.ANDROID_API) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, findAndroidApiJar());
        }
        else {
            JvmContentRootsKt.addJvmClasspathRoots(configuration, PathUtil.getJdkClassesRoots());
        }

        if (configurationKind.getWithRuntime()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.runtimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.kotlinTestJarForTests());
        }
        else if (configurationKind.getWithMockRuntime()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.mockRuntimeJarForTests());
        }
        if (configurationKind.getWithReflection()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.reflectJarForTests());
        }

        JvmContentRootsKt.addJvmClasspathRoots(configuration, classpath);

        configuration.put(MODULE_NAME, "compilerConfigurationForTests");

        return configuration;
    }

    public static void resolveAllKotlinFiles(KotlinCoreEnvironment environment) throws IOException {
        List<ContentRoot> paths = environment.getConfiguration().get(CommonConfigurationKeys.CONTENT_ROOTS);
        if (paths == null) return;
        List<KtFile> jetFiles = Lists.newArrayList();
        for (ContentRoot root : paths) {
            if (!(root instanceof KotlinSourceRoot)) continue;

            String path = ((KotlinSourceRoot) root).getPath();
            File file = new File(path);
            if (file.isFile()) {
                jetFiles.add(loadJetFile(environment.getProject(), file));
            }
            else {
                //noinspection ConstantConditions
                for (File childFile : file.listFiles()) {
                    if (childFile.getName().endsWith(".kt")) {
                        jetFiles.add(loadJetFile(environment.getProject(), childFile));
                    }
                }
            }
        }
        LazyResolveTestUtil.resolve(environment.getProject(), jetFiles, environment);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull Editor editor) {
        String actualText = editor.getDocument().getText();
        String afterText = new StringBuilder(actualText).insert(editor.getCaretModel().getOffset(), "<caret>").toString();

        assertEqualsToFile(expectedFile, afterText);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual) {
        assertEqualsToFile(expectedFile, actual, new Function1<String, String>() {
            @Override
            public String invoke(String s) {
                return s;
            }
        });
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual, @NotNull Function1<String, String> sanitizer) {
        try {
            String actualText = JetTestUtilsKt.trimTrailingWhitespacesAndAddNewlineAtEOF(StringUtil.convertLineSeparators(actual.trim()));

            if (!expectedFile.exists()) {
                FileUtil.writeToFile(expectedFile, actualText);
                Assert.fail("Expected data file did not exist. Generating: " + expectedFile);
            }
            String expected = FileUtil.loadFile(expectedFile, CharsetToolkit.UTF8, true);

            String expectedText = JetTestUtilsKt.trimTrailingWhitespacesAndAddNewlineAtEOF(StringUtil.convertLineSeparators(expected.trim()));

            if (!Comparing.equal(sanitizer.invoke(expectedText), sanitizer.invoke(actualText))) {
                throw new FileComparisonFailure("Actual data differs from file content: " + expectedFile.getName(),
                                                expected, actual, expectedFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    public static void compileKotlinWithJava(
            @NotNull List<File> javaFiles,
            @NotNull List<File> ktFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @Nullable File javaErrorFile
    ) throws IOException {
        if (!ktFiles.isEmpty()) {
            compileKotlinToDirAndGetAnalysisResult(ktFiles, outDir, disposable, ALL, false);
        }
        else {
            boolean mkdirs = outDir.mkdirs();
            assert mkdirs : "Not created: " + outDir;
        }
        if (!javaFiles.isEmpty()) {
            compileJavaFiles(javaFiles, Arrays.asList(
                    "-classpath", outDir.getPath() + File.pathSeparator + ForTestCompileRuntime.runtimeJarForTests(),
                    "-d", outDir.getPath()
            ), javaErrorFile);
        }
    }

    public interface TestFileFactory<M, F> {
        F createFile(@Nullable M module, @NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives);
        M createModule(@NotNull String name, @NotNull List<String> dependencies);
    }

    public static abstract class TestFileFactoryNoModules<F> implements TestFileFactory<Void, F> {
        @Override
        public final F createFile(
                @Nullable Void module,
                @NotNull String fileName,
                @NotNull String text,
                @NotNull Map<String, String> directives
        ) {
            return create(fileName, text, directives);
        }

        @NotNull
        public abstract F create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives);

        @Override
        public Void createModule(@NotNull String name, @NotNull List<String> dependencies) {
            return null;
        }
    }

    @NotNull
    public static <M, F> List<F> createTestFiles(String testFileName, String expectedText, TestFileFactory<M, F> factory) {
        Map<String, String> directives = parseDirectives(expectedText);

        List<F> testFiles = Lists.newArrayList();
        Matcher matcher = FILE_OR_MODULE_PATTERN.matcher(expectedText);
        if (!matcher.find()) {
            // One file
            testFiles.add(factory.createFile(null, testFileName, expectedText, directives));
        }
        else {
            int processedChars = 0;
            M module = null;
            // Many files
            while (true) {
                String moduleName = matcher.group(1);
                String moduleDependencies = matcher.group(2);
                if (moduleName != null) {
                    module = factory.createModule(moduleName, parseDependencies(moduleDependencies));
                }

                String fileName = matcher.group(3);
                int start = processedChars;

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

                testFiles.add(factory.createFile(module, fileName, fileText, directives));

                if (!nextFileExists) break;
            }
            assert processedChars == expectedText.length() : "Characters skipped from " +
                                                             processedChars +
                                                             " to " +
                                                             (expectedText.length() - 1);
        }
        return testFiles;
    }

    private static List<String> parseDependencies(@Nullable String dependencies) {
        if (dependencies == null) return Collections.emptyList();

        Matcher matcher = Pattern.compile("\\w+").matcher(dependencies);
        List<String> result = new ArrayList<String>();
        while (matcher.find()) {
            result.add(matcher.group());
        }
        return result;
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
            String value = directiveMatcher.group(3);
            String oldValue = directives.put(name, value);
            Assert.assertNull("Directive overwritten: " + name + " old value: " + oldValue + " new value: " + value, oldValue);
            start = directiveMatcher.end() + 1;
        }
        return directives;
    }

    public static List<String> loadBeforeAfterText(String filePath) {
        String content;

        try {
            content = FileUtil.loadFile(new File(filePath), true);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<String> files = createTestFiles("", content, new TestFileFactoryNoModules<String>() {
            @NotNull
            @Override
            public String create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
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

    public enum CommentType {
        ALL,
        LINE_COMMENT,
        BLOCK_COMMENT
    }

    @NotNull
    public static String getLastCommentInFile(@NotNull KtFile file) {
        return CollectionsKt.first(getLastCommentsInFile(file, CommentType.ALL, true));
    }

    @NotNull
    public static List<String> getLastCommentsInFile(@NotNull KtFile file, CommentType commentType, boolean assertMustExist) {
        PsiElement lastChild = file.getLastChild();
        if (lastChild != null && lastChild.getNode().getElementType().equals(KtTokens.WHITE_SPACE)) {
            lastChild = lastChild.getPrevSibling();
        }
        assert lastChild != null;

        List<String> comments = ContainerUtil.newArrayList();

        while (true) {
            if (lastChild.getNode().getElementType().equals(KtTokens.BLOCK_COMMENT)) {
                if (commentType == CommentType.ALL || commentType == CommentType.BLOCK_COMMENT) {
                    String lastChildText = lastChild.getText();
                    comments.add(lastChildText.substring(2, lastChildText.length() - 2).trim());
                }
            }
            else if (lastChild.getNode().getElementType().equals(KtTokens.EOL_COMMENT)) {
                if (commentType == CommentType.ALL || commentType == CommentType.LINE_COMMENT) {
                    comments.add(lastChild.getText().substring(2).trim());
                }
            }
            else {
                break;
            }

            lastChild = lastChild.getPrevSibling();
        }

        if (comments.isEmpty() && assertMustExist) {
            throw new AssertionError(String.format(
                    "Test file '%s' should end in a comment of type %s; last node was: %s", file.getName(), commentType, lastChild));
        }

        return comments;
    }

    public static void compileJavaFiles(@NotNull Collection<File> files, List<String> options) throws IOException {
        compileJavaFiles(files, options, null);
    }

    public static void compileJavaFiles(@NotNull Collection<File> files, List<String> options, @Nullable File javaErrorFile) throws IOException {
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
            if (javaErrorFile == null || !javaErrorFile.exists()) {
                Assert.assertTrue(errorsToString(diagnosticCollector, true), success);
            }
            else {
                assertEqualsToFile(javaErrorFile, errorsToString(diagnosticCollector, false));
            }
        } finally {
            fileManager.close();
        }
    }

    @NotNull
    private static String errorsToString(@NotNull  DiagnosticCollector<JavaFileObject> diagnosticCollector, boolean humanReadable) {
        StringBuilder builder = new StringBuilder();
        for (javax.tools.Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            if (diagnostic.getKind() != javax.tools.Diagnostic.Kind.ERROR) continue;

            if (humanReadable) {
                builder.append(diagnostic).append("\n");
            }
            else {
                builder.append(diagnostic.getSource().getName()).append(":")
                        .append(diagnostic.getLineNumber()).append(":")
                        .append(diagnostic.getColumnNumber()).append(":")
                        .append(diagnostic.getCode()).append("\n");
            }
        }
        return builder.toString();
    }

    public static String navigationMetadata(@TestDataFile String testFile) {
        return testFile;
    }

    public static String getTestsRoot(@NotNull Class<?> testCaseClass) {
        TestMetadata testClassMetadata = testCaseClass.getAnnotation(TestMetadata.class);
        Assert.assertNotNull("No metadata for class: " + testCaseClass, testClassMetadata);
        return testClassMetadata.value();
    }

    public static void assertAllTestsPresentByMetadata(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull Pattern filenamePattern,
            boolean recursive,
            @NotNull String... excludeDirs
    ) {
        File rootFile = new File(getTestsRoot(testCaseClass));

        Set<String> filePaths = collectPathsMetadata(testCaseClass);
        Set<String> exclude = SetsKt.setOf(excludeDirs);

        File[] files = testDataDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (recursive && containsTestData(file, filenamePattern) && !exclude.contains(file.getName())) {
                        assertTestClassPresentByMetadata(testCaseClass, file);
                    }
                }
                else if (filenamePattern.matcher(file.getName()).matches()) {
                    assertFilePathPresent(file, rootFile, filePaths);
                }
            }
        }
    }

    public static void assertAllTestsPresentInSingleGeneratedClass(
            @NotNull Class<?> testCaseClass,
            @NotNull File testDataDir,
            @NotNull final Pattern filenamePattern
    ) {
        final File rootFile = new File(getTestsRoot(testCaseClass));

        final Set<String> filePaths = collectPathsMetadata(testCaseClass);

        FileUtil.processFilesRecursively(testDataDir, new Processor<File>() {
            @Override
            public boolean process(File file) {
                if (file.isFile() && filenamePattern.matcher(file.getName()).matches()) {
                    assertFilePathPresent(file, rootFile, filePaths);
                }

                return true;
            }
        });
    }

    private static void assertFilePathPresent(File file, File rootFile, Set<String> filePaths) {
        String path = FileUtil.getRelativePath(rootFile, file);
        if (path != null) {
            String relativePath = FileUtil.nameToCompare(path);
            if (!filePaths.contains(relativePath)) {
                Assert.fail("Test data file missing from the generated test class: " + file + "\n" + PLEASE_REGENERATE_TESTS);
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

    private static void assertTestClassPresentByMetadata(@NotNull Class<?> outerClass, @NotNull File testDataDir) {
        for (Class<?> nestedClass : outerClass.getDeclaredClasses()) {
            TestMetadata testMetadata = nestedClass.getAnnotation(TestMetadata.class);
            if (testMetadata != null && testMetadata.value().equals(getFilePath(testDataDir))) {
                return;
            }
        }
        Assert.fail("Test data directory missing from the generated test class: " + testDataDir + "\n" + PLEASE_REGENERATE_TESTS);
    }

    @NotNull
    public static KtFile loadJetFile(@NotNull Project project, @NotNull File ioFile) throws IOException {
        String text = FileUtil.loadFile(ioFile, true);
        return KtPsiFactoryKt.KtPsiFactory(project).createPhysicalFile(ioFile.getName(), text);
    }

    @NotNull
    public static List<KtFile> loadToJetFiles(@NotNull KotlinCoreEnvironment environment, @NotNull List<File> files) throws IOException {
        List<KtFile> jetFiles = Lists.newArrayList();
        for (File file : files) {
            jetFiles.add(loadJetFile(environment.getProject(), file));
        }
        return jetFiles;
    }

    @NotNull
    public static ModuleDescriptorImpl createEmptyModule() {
        return createEmptyModule("<empty-for-test>");
    }

    @NotNull
    public static ModuleDescriptorImpl createEmptyModule(@NotNull String name) {
        return createEmptyModule(name, TargetPlatform.Default.INSTANCE, DefaultBuiltIns.getInstance());
    }

    @NotNull
    public static ModuleDescriptorImpl createEmptyModule(@NotNull String name, @NotNull TargetPlatform platform, KotlinBuiltIns builtIns) {
        return new ModuleDescriptorImpl(
                Name.special(name), LockBasedStorageManager.NO_LOCKS, platform.getDefaultModuleParameters(), builtIns
        );
    }

    @NotNull
    public static File replaceExtension(@NotNull File file, @Nullable String newExtension) {
        return new File(file.getParentFile(), FileUtil.getNameWithoutExtension(file) + (newExtension == null ? "" : "." + newExtension));
    }

    @NotNull
    public static String replaceHashWithStar(@NotNull String string) {
        return replaceHash(string, "*");
    }

    public static String replaceHash(@NotNull String string, @NotNull String replacement) {
        //TODO: hashes are still used in SamWrapperCodegen
        Matcher matcher = STRIP_PACKAGE_PART_HASH_PATTERN.matcher(string);
        if (matcher.find()) {
            return matcher.replaceAll("\\$" + replacement);
        }
        return string;
    }

    public static boolean isAllFilesPresentTest(String testName) {
        //noinspection SpellCheckingInspection
        return testName.toLowerCase().startsWith("allfilespresentin");
    }
}
