/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test;

import com.google.common.collect.Lists;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.TestDataFile;
import junit.framework.TestCase;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings;
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettingsKt;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.config.ContentRootsKt;
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity;
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation;
import org.jetbrains.kotlin.cli.common.messages.MessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.test.util.JUnit4Assertions;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.util.StringUtilsKt;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.test.InTextDirectivesUtils.*;

public class KotlinTestUtils {
    public static String TEST_MODULE_NAME = "test-module";

    public static final String TEST_GENERATOR_NAME = "org.jetbrains.kotlin.generators.tests.TestsPackage";

    private static final boolean RUN_IGNORED_TESTS_AS_REGULAR =
            Boolean.getBoolean("org.jetbrains.kotlin.run.ignored.tests.as.regular");

    private static final boolean PRINT_STACKTRACE_FOR_IGNORED_TESTS =
            Boolean.getBoolean("org.jetbrains.kotlin.print.stacktrace.for.ignored.tests");

    private static final boolean DONT_IGNORE_TESTS_WORKING_ON_COMPATIBLE_BACKEND =
            Boolean.getBoolean("org.jetbrains.kotlin.dont.ignore.tests.working.on.compatible.backend");

    private static final boolean AUTOMATICALLY_UNMUTE_PASSED_TESTS = false;
    private static final boolean AUTOMATICALLY_MUTE_FAILED_TESTS = false;

    private static final Pattern DIRECTIVE_PATTERN = Pattern.compile("^//\\s*[!]?([A-Z_]+)(:[ \\t]*(.*))?$", Pattern.MULTILINE);

    private KotlinTestUtils() {
    }

    @NotNull
    public static AnalysisResult analyzeFile(@NotNull KtFile file, @NotNull KotlinCoreEnvironment environment) {
        return JvmResolveUtil.analyze(file, environment);
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
                disposable, newConfiguration(configurationKind, jdkKind, KtTestUtil.getAnnotationsJar()), EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    @NotNull
    public static KotlinCoreEnvironment createEnvironmentWithFullJdkAndIdeaAnnotations(Disposable disposable) {
        return createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(disposable, ConfigurationKind.ALL, TestJdkKind.FULL_JDK);
    }

    @NotNull
    public static File tmpDirForTest(TestCase test) throws IOException {
        return KtTestUtil.tmpDirForTest(test.getClass().getSimpleName(), test.getName());
    }

    @NotNull
    public static CompilerConfiguration newConfiguration() {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.put(CommonConfigurationKeys.MODULE_NAME, TEST_MODULE_NAME);

        if ("true".equals(System.getProperty("kotlin.ni"))) {
            // Enable new inference for tests which do not declare their own language version settings
            CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, new CompilerTestLanguageVersionSettings(
                    Collections.emptyMap(),
                    LanguageVersionSettingsImpl.DEFAULT.getApiVersion(),
                    LanguageVersionSettingsImpl.DEFAULT.getLanguageVersion(),
                    Collections.emptyMap()
            ));
        }

        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, new MessageCollector() {
            @Override
            public void clear() {
            }

            @Override
            public void report(
                    @NotNull CompilerMessageSeverity severity, @NotNull String message, @Nullable CompilerMessageSourceLocation location
            ) {
                if (severity == CompilerMessageSeverity.ERROR) {
                    String prefix = location == null
                                  ? ""
                                  : "(" + location.getPath() + ":" + location.getLine() + ":" + location.getColumn() + ") ";
                    throw new AssertionError(prefix + message);
                }
            }

            @Override
            public boolean hasErrors() {
                return false;
            }
        });

        return configuration;
    }

    @NotNull
    public static CompilerConfiguration newConfiguration(
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull File... extraClasspath
    ) {
        return newConfiguration(configurationKind, jdkKind, Arrays.asList(extraClasspath), Collections.emptyList());
    }

    @NotNull
    public static CompilerConfiguration newConfiguration(
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<File> classpath,
            @NotNull List<File> javaSource
    ) {
        CompilerConfiguration configuration = newConfiguration();
        JvmContentRootsKt.addJavaSourceRoots(configuration, javaSource);
        if (jdkKind == TestJdkKind.MOCK_JDK) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, KtTestUtil.findMockJdkRtJar());
            configuration.put(JVMConfigurationKeys.NO_JDK, true);
        }
        else if (jdkKind == TestJdkKind.MODIFIED_MOCK_JDK) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, KtTestUtil.findMockJdkRtModified());
            configuration.put(JVMConfigurationKeys.NO_JDK, true);
        }
        else if (jdkKind == TestJdkKind.ANDROID_API) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, KtTestUtil.findAndroidApiJar());
            configuration.put(JVMConfigurationKeys.NO_JDK, true);
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_6) {
            String jdk6 = System.getenv("JDK_16");
            assert jdk6 != null : "Environment variable JDK_16 is not set";
            configuration.put(JVMConfigurationKeys.JDK_HOME, new File(jdk6));
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_9) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk9Home());
        }
        else if (jdkKind == TestJdkKind.FULL_JDK_15) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, KtTestUtil.getJdk15Home());
        }
        else if (SystemInfo.IS_AT_LEAST_JAVA9) {
            configuration.put(JVMConfigurationKeys.JDK_HOME, new File(System.getProperty("java.home")));
        }

        if (configurationKind.getWithRuntime()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.runtimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.scriptRuntimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.kotlinTestJarForTests());
        }
        else if (configurationKind.getWithMockRuntime()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.minimalRuntimeJarForTests());
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.scriptRuntimeJarForTests());
        }
        if (configurationKind.getWithReflection()) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.reflectJarForTests());
        }

        JvmContentRootsKt.addJvmClasspathRoots(configuration, classpath);

        return configuration;
    }

    public static void resolveAllKotlinFiles(KotlinCoreEnvironment environment) throws IOException {
        List<KotlinSourceRoot> roots = ContentRootsKt.getKotlinSourceRoots(environment.getConfiguration());
        if (roots.isEmpty()) return;
        List<KtFile> ktFiles = new ArrayList<>();
        for (KotlinSourceRoot root : roots) {
            File file = new File(root.getPath());
            if (file.isFile()) {
                ktFiles.add(loadJetFile(environment.getProject(), file));
            }
            else {
                //noinspection ConstantConditions
                for (File childFile : file.listFiles()) {
                    if (childFile.getName().endsWith(".kt") || childFile.getName().endsWith(".kts")) {
                        ktFiles.add(loadJetFile(environment.getProject(), childFile));
                    }
                }
            }
        }
        JvmResolveUtil.analyze(ktFiles, environment);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull Editor editor) {
        assertEqualsToFile(expectedFile, editor, true);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull Editor editor, Boolean enableSelectionTags) {
        Caret caret = editor.getCaretModel().getCurrentCaret();
        List<TagsTestDataUtil.TagInfo> tags = Lists.newArrayList(
                new TagsTestDataUtil.TagInfo<>(caret.getOffset(), true, "caret")
        );

        if (enableSelectionTags) {
            int selectionStart = caret.getSelectionStart();
            int selectionEnd = caret.getSelectionEnd();

            tags.add(new TagsTestDataUtil.TagInfo<>(selectionStart, true, "selection"));
            tags.add(new TagsTestDataUtil.TagInfo<>(selectionEnd, false, "selection"));
        }

        String afterText = TagsTestDataUtil.insertTagsInText(tags, editor.getDocument().getText(), (TagsTestDataUtil.TagInfo t) -> null);

        assertEqualsToFile(expectedFile, afterText);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual) {
        assertEqualsToFile(expectedFile, actual, s -> s);
    }

    public static void assertEqualsToFile(@NotNull String message, @NotNull File expectedFile, @NotNull String actual) {
        assertEqualsToFile(message, expectedFile, actual, s -> s);
    }

    public static void assertEqualsToFile(@NotNull File expectedFile, @NotNull String actual, @NotNull Function1<String, String> sanitizer) {
        assertEqualsToFile("Actual data differs from file content", expectedFile, actual, sanitizer);
    }

    public static void assertEqualsToFile(@NotNull String message, @NotNull File expectedFile, @NotNull String actual, @NotNull Function1<String, String> sanitizer) {
        try {
            String actualText = StringUtilsKt.trimTrailingWhitespacesAndAddNewlineAtEOF(StringUtil.convertLineSeparators(actual.trim()));

            if (!expectedFile.exists()) {
                FileUtil.writeToFile(expectedFile, actualText);
                Assert.fail("Expected data file did not exist. Generating: " + expectedFile);
            }
            String expected = FileUtil.loadFile(expectedFile, CharsetToolkit.UTF8, true);

            String expectedText = StringUtilsKt.trimTrailingWhitespacesAndAddNewlineAtEOF(StringUtil.convertLineSeparators(expected.trim()));

            if (!Objects.equals(sanitizer.invoke(expectedText), sanitizer.invoke(actualText))) {
                throw new FileComparisonFailure(message + ": " + expectedFile.getName(),
                                                expected, actual, expectedFile.getAbsolutePath());
            }
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    public static boolean compileKotlinWithJava(
            @NotNull List<File> javaFiles,
            @NotNull List<File> ktFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @Nullable File javaErrorFile
    ) throws IOException {
        return compileKotlinWithJava(javaFiles, ktFiles, outDir, disposable, javaErrorFile, null);
    }

    public static boolean compileKotlinWithJava(
            @NotNull List<File> javaFiles,
            @NotNull List<File> ktFiles,
            @NotNull File outDir,
            @NotNull Disposable disposable,
            @Nullable File javaErrorFile,
            @Nullable Function1<CompilerConfiguration, Unit> updateConfiguration
    ) throws IOException {
        if (!ktFiles.isEmpty()) {
            KotlinCoreEnvironment environment = createEnvironmentWithFullJdkAndIdeaAnnotations(disposable);
            CompilerTestLanguageVersionSettingsKt.setupLanguageVersionSettingsForMultifileCompilerTests(ktFiles, environment);
            if (updateConfiguration != null) {
                updateConfiguration.invoke(environment.getConfiguration());
            }
            LoadDescriptorUtil.compileKotlinToDirAndGetModule(ktFiles, outDir, environment);
        }
        else {
            boolean mkdirs = outDir.mkdirs();
            assert mkdirs : "Not created: " + outDir;
        }
        if (javaFiles.isEmpty()) return true;

        return compileJavaFiles(javaFiles, Arrays.asList(
                "-classpath", outDir.getPath() + File.pathSeparator + ForTestCompileRuntime.runtimeJarForTests(),
                "-d", outDir.getPath()
        ), javaErrorFile);
    }

    @NotNull
    public static Directives parseDirectives(String expectedText) {
        return parseDirectives(expectedText, new Directives());
    }

    @NotNull
    public static Directives parseDirectives(String expectedText, @NotNull Directives directives) {
        Matcher directiveMatcher = DIRECTIVE_PATTERN.matcher(expectedText);
        while (directiveMatcher.find()) {
            String name = directiveMatcher.group(1);
            String value = directiveMatcher.group(3);
            directives.put(name, value);
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

        List<String> files = TestFiles.createTestFiles("", content, new TestFiles.TestFileFactoryNoModules<String>() {
            @NotNull
            @Override
            public String create(@NotNull String fileName, @NotNull String text, @NotNull Directives directives) {
                int firstLineEnd = text.indexOf('\n');
                return StringUtil.trimTrailing(text.substring(firstLineEnd + 1));
            }
        });

        Assert.assertTrue("Exactly two files expected: ", files.size() == 2);

        return files;
    }

    public static String getLastCommentedLines(@NotNull Document document) {
        List<CharSequence> resultLines = new ArrayList<>();
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

        List<String> comments = new ArrayList<>();

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

    public static boolean compileJavaFiles(@NotNull Collection<File> files, List<String> options) throws IOException {
        return compileJavaFiles(files, options, null);
    }

    private static boolean compileJavaFiles(@NotNull Collection<File> files, List<String> options, @Nullable File javaErrorFile) throws IOException {
        return JvmCompilationUtils.compileJavaFiles(files, options, javaErrorFile, JUnit4Assertions.INSTANCE);
    }

    public static boolean compileJavaFilesExternallyWithJava9(@NotNull Collection<File> files, @NotNull List<String> options) {
        return JvmCompilationUtils.compileJavaFilesExternally(files, options, KtTestUtil.getJdk9Home());
    }

    public static boolean compileJavaFilesExternally(@NotNull Collection<File> files, @NotNull List<String> options, @NotNull File jdkHome) {
        return JvmCompilationUtils.compileJavaFilesExternally(files, options, jdkHome);
    }

    public static String navigationMetadata(@TestDataFile String testFile) {
        return testFile;
    }

    public interface DoTest {
        void invoke(@NotNull String filePath) throws Exception;
    }

    public static void runTest(@NotNull DoTest test, @NotNull TestCase testCase, @TestDataFile String testDataFile) throws Exception {
        runTestImpl(testWithCustomIgnoreDirective(test, TargetBackend.ANY, IGNORE_BACKEND_DIRECTIVE_PREFIX), testCase, testDataFile);
    }

    public static void runTest(@NotNull TestCase testCase, @NotNull Function0<Unit> test) {
        MuteWithDatabaseKt.runTest(testCase, test);
    }

    public static void runTestWithThrowable(@NotNull TestCase testCase, @NotNull RunnableWithThrowable test) {
        MuteWithDatabaseKt.runTest(testCase, () -> {
            try {
                test.run();
            }
            catch (Throwable throwable) {
                throw new IllegalStateException(throwable);
            }
            return null;
        });
    }

    // In this test runner version the `testDataFile` parameter is annotated by `TestDataFile`.
    // So only file paths passed to this parameter will be used in navigation actions, like "Navigate to testdata" and "Related Symbol..."
    public static void runTest(DoTest test, TargetBackend targetBackend, @TestDataFile String testDataFile) throws Exception {
        runTest0(test, targetBackend, testDataFile);
    }

    public static void runTestWithCustomIgnoreDirective(DoTest test, TargetBackend targetBackend, @TestDataFile String testDataFile, String ignoreDirective) throws Exception {
        runTestImpl(testWithCustomIgnoreDirective(test, targetBackend, ignoreDirective), null, testDataFile);
    }

    // In this test runner version, NONE of the parameters are annotated by `TestDataFile`.
    // So DevKit will use test name to determine related files in navigation actions, like "Navigate to testdata" and "Related Symbol..."
    //
    // Pro:
    // * in most cases, it shows all related files including generated js files, for example.
    // Cons:
    // * sometimes, for too common/general names, it shows many variants to navigate
    // * it adds an additional step for navigation -- you must choose an exact file to navigate
    public static void runTest0(DoTest test, TargetBackend targetBackend, String testDataFilePath) throws Exception {
        runTestImpl(testWithCustomIgnoreDirective(test, targetBackend, IGNORE_BACKEND_DIRECTIVE_PREFIX), null, testDataFilePath);
    }

    private static void runTestImpl(@NotNull DoTest test, @Nullable TestCase testCase, String testDataFilePath) throws Exception {
        if (testCase != null && !isRunTestOverridden(testCase)) {
            Function0<Unit> wrapWithMuteInDatabase = MuteWithDatabaseKt.wrapWithMuteInDatabase(testCase, () -> {
                try {
                    test.invoke(testDataFilePath);
                }
                catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                return null;
            });
            if (wrapWithMuteInDatabase != null) {
                wrapWithMuteInDatabase.invoke();
                return;
            }
        }
        test.invoke(testDataFilePath);
    }

    private static boolean isRunTestOverridden(TestCase testCase) {
        Class<?> type = testCase.getClass();
        while (type != null) {
            for (Annotation annotation : type.getDeclaredAnnotations()) {
                if (annotation.annotationType().equals(WithMutedInDatabaseRunTest.class)) {
                    return true;
                }
            }
            type = type.getSuperclass();
        }
        return false;
    }

    private static DoTest testWithCustomIgnoreDirective(DoTest test, TargetBackend targetBackend, String ignoreDirective) throws Exception {
        return filePath -> {
            File testDataFile = new File(filePath);

            boolean isIgnored = isIgnoredTarget(targetBackend, testDataFile, ignoreDirective);

            if (DONT_IGNORE_TESTS_WORKING_ON_COMPATIBLE_BACKEND) {
                // Only ignore if it is ignored for both backends
                // Motivation: this backend works => all good, even if compatible backend fails
                // This backend fails, compatible works => need to know
                isIgnored &= isIgnoredTarget(targetBackend.getCompatibleWith(), testDataFile);
            }

            try {
                test.invoke(filePath);
            }
            catch (Throwable e) {
                if (!isIgnored && AUTOMATICALLY_MUTE_FAILED_TESTS) {
                    String text = KtTestUtil.doLoadFile(testDataFile);
                    String directive = ignoreDirective + targetBackend.name() + "\n";

                    String newText;
                    if (text.startsWith("// !")) {
                        StringBuilder prefixBuilder = new StringBuilder();
                        int l = 0;
                        while (text.startsWith("// !", l)) {
                            int r = text.indexOf("\n", l) + 1;
                            if (r <= 0) r = text.length();
                            prefixBuilder.append(text.substring(l, r));
                            l = r;
                        }
                        prefixBuilder.append(directive);
                        prefixBuilder.append(text.substring(l));

                        newText = prefixBuilder.toString();
                    } else {
                        newText = directive + text;
                    }

                    if (!newText.equals(text)) {
                        System.err.println("\"" + directive + "\" was added to \"" + testDataFile + "\"");
                        FileUtil.writeToFile(testDataFile, newText);
                    }
                }

                if (RUN_IGNORED_TESTS_AS_REGULAR || !isIgnored) {
                    throw e;
                }

                if (PRINT_STACKTRACE_FOR_IGNORED_TESTS) {
                    e.printStackTrace();
                }
                return;
            }

            if (isIgnored) {
                if (AUTOMATICALLY_UNMUTE_PASSED_TESTS) {
                    String text = KtTestUtil.doLoadFile(testDataFile);
                    String directive = ignoreDirective + targetBackend.name();
                    String newText = Pattern.compile("^" + directive + "\n", Pattern.MULTILINE).matcher(text).replaceAll("");
                    if (!newText.equals(text)) {
                        System.err.println("\"" + directive + "\" was removed from \"" + testDataFile + "\"");
                        FileUtil.writeToFile(testDataFile, newText);
                    }
                }

                throw new AssertionError(String.format("Looks like this test can be unmuted. Remove \"%s%s\" directive.", ignoreDirective, targetBackend));
            }
        };
    }

    /**
     * @return test data file name specified in the metadata of test method
     */
    @Nullable
    public static String getTestDataFileName(@NotNull Class<?> testCaseClass, @NotNull String testName) {
        try {
            Method method = testCaseClass.getDeclaredMethod(testName);
            return KtTestUtil.getMethodMetadata(method);
        }
        catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
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
        return createEmptyModule(name, DefaultBuiltIns.getInstance());
    }

    @NotNull
    public static ModuleDescriptorImpl createEmptyModule(@NotNull String name, @NotNull KotlinBuiltIns builtIns) {
        return new ModuleDescriptorImpl(Name.special(name), LockBasedStorageManager.NO_LOCKS, builtIns);
    }

    @NotNull
    public static File replaceExtension(@NotNull File file, @Nullable String newExtension) {
        return new File(file.getParentFile(), FileUtil.getNameWithoutExtension(file) + (newExtension == null ? "" : "." + newExtension));
    }

    public static boolean isAllFilesPresentTest(String testName) {
        //noinspection SpellCheckingInspection
        return testName.toLowerCase().startsWith("allfilespresentin");
    }

    public static boolean isMultiExtensionName(@NotNull String name) {
        int firstDotIndex = name.indexOf('.');
        if (firstDotIndex == -1) {
            return false;
        }
        // Several extension if name contains another dot
        return name.indexOf('.', firstDotIndex + 1) != -1;
    }
}
