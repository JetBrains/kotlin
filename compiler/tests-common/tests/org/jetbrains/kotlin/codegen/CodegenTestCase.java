/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.TestDataFile;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.TestHelperGeneratorKt;
import org.jetbrains.kotlin.TestsCompilerError;
import org.jetbrains.kotlin.TestsCompiletimeError;
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection;
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil;
import org.jetbrains.kotlin.cli.common.output.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider;
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.clientserver.TestProxy;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.jetbrains.kotlin.cli.common.output.OutputUtilsKt.writeAllTo;
import static org.jetbrains.kotlin.codegen.CodegenTestUtil.*;
import static org.jetbrains.kotlin.codegen.TestUtilsKt.extractUrls;
import static org.jetbrains.kotlin.codegen.CodegenTestUtilsKt.*;
import static org.jetbrains.kotlin.test.util.KtTestUtil.getAnnotationsJar;

public abstract class CodegenTestCase extends KotlinBaseTest<KotlinBaseTest.TestFile> {
    private static final String DEFAULT_TEST_FILE_NAME = "a_test";
    private static final String DEFAULT_JVM_TARGET = System.getProperty("kotlin.test.default.jvm.target");
    public static final String BOX_IN_SEPARATE_PROCESS_PORT = System.getProperty("kotlin.test.box.in.separate.process.port");

    protected KotlinCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;
    protected ClassFileFactory classFileFactory;
    protected GeneratedClassLoader initializedClassLoader;
    protected File javaClassesOutputDirectory = null;
    protected List<File> additionalDependencies = null;

    protected ConfigurationKind configurationKind = ConfigurationKind.JDK_ONLY;

    protected final void createEnvironmentWithMockJdkAndIdeaAnnotations(
            @NotNull ConfigurationKind configurationKind,
            @NotNull File... javaSourceRoots
    ) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind, Collections.emptyList(), TestJdkKind.MOCK_JDK, javaSourceRoots);
    }

    protected final void createEnvironmentWithMockJdkAndIdeaAnnotations(
            @NotNull ConfigurationKind configurationKind,
            @NotNull List<TestFile> testFilesWithConfigurationDirectives,
            @NotNull TestJdkKind testJdkKind,
            @NotNull File... javaSourceRoots
    ) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }

        CompilerConfiguration configuration = createConfiguration(
                configurationKind,
                testJdkKind,
                getBackend(),
                Collections.singletonList(getAnnotationsJar()),
                ArraysKt.filterNotNull(javaSourceRoots),
                testFilesWithConfigurationDirectives
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        setupEnvironment(myEnvironment);
    }

    @Override
    protected void tearDown() throws Exception {
        myFiles = null;
        myEnvironment = null;
        classFileFactory = null;

        if (initializedClassLoader != null) {
            initializedClassLoader.dispose();
            initializedClassLoader = null;
        }

        super.tearDown();
    }

    protected void loadText(@NotNull String text) {
        myFiles = CodegenTestFiles.create(DEFAULT_TEST_FILE_NAME + ".kt", text, myEnvironment.getProject());
    }

    @NotNull
    protected String loadFile(@NotNull @TestDataFile String name) {
        return loadFileByFullPath(KtTestUtil.getTestDataPathBase() + "/codegen/" + name);
    }

    @NotNull
    protected String loadFileByFullPath(@NotNull String fullPath) {
        try {
            File file = new File(fullPath);
            String content = FileUtil.loadFile(file, Charsets.UTF_8.name(), true);
            assert myFiles == null : "Should not initialize myFiles twice";
            myFiles = CodegenTestFiles.create(file.getName(), content, myEnvironment.getProject());
            return content;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFiles(@NotNull String... names) {
        myFiles = CodegenTestFiles.create(myEnvironment.getProject(), names);
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".kt");
    }

    protected void loadMultiFiles(@NotNull List<TestFile> files) {
        myFiles = loadMultiFiles(files, myEnvironment.getProject());
    }

    @NotNull
    public static CodegenTestFiles loadMultiFiles(@NotNull List<TestFile> files, @NotNull Project project) {
        Collections.sort(files);

        List<KtFile> ktFiles = new ArrayList<>(files.size());
        for (TestFile file : files) {
            if (file.name.endsWith(".kt") || file.name.endsWith(".kts")) {
                // `rangesToDiagnosticNames` parameter is not-null only for diagnostic tests, it's using for lazy diagnostics
                String content = CheckerTestUtil.INSTANCE.parseDiagnosedRanges(file.content, new ArrayList<>(0), null);
                ktFiles.add(KtTestUtil.createFile(file.name, content, project));
            }
        }

        return CodegenTestFiles.create(ktFiles);
    }

    @NotNull
    protected String codegenTestBasePath() {
        return "compiler/testData/codegen/";
    }

    @NotNull
    protected String relativePath(@NotNull File file) {
        return FilesKt.toRelativeString(file.getAbsoluteFile(), new File(codegenTestBasePath()).getAbsoluteFile());
    }

    @NotNull
    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    protected GeneratedClassLoader generateAndCreateClassLoader(boolean reportProblems) {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test");
        }

        initializedClassLoader = createClassLoader();

        if (!CodegenTestUtil.verifyAllFilesWithAsm(generateClassesInFile(reportProblems), initializedClassLoader, reportProblems)) {
            fail("Verification failed: see exceptions above");
        }

        return initializedClassLoader;
    }

    @NotNull
    protected GeneratedClassLoader createClassLoader() {
        ClassLoader classLoader;
        if (configurationKind.getWithReflection()) {
            classLoader = ForTestCompileRuntime.runtimeAndReflectJarClassLoader();
        }
        else {
            classLoader = ForTestCompileRuntime.runtimeJarClassLoader();
        }

        return new GeneratedClassLoader(
                generateClassesInFile(),
                classLoader,
                getClassPathURLs()
        );
    }

    @NotNull
    protected URL[] getClassPathURLs() {
        List<File> files = new ArrayList<>();
        if (javaClassesOutputDirectory != null) {
            files.add(javaClassesOutputDirectory);
        }
        if (additionalDependencies != null) {
            files.addAll(additionalDependencies);
        }

        ScriptDependenciesProvider externalImportsProvider =
                ScriptDependenciesProvider.Companion.getInstance(myEnvironment.getProject());
        if (externalImportsProvider != null) {
            myEnvironment.getSourceFiles().forEach(
                    file -> {
                        ScriptCompilationConfigurationWrapper refinedConfiguration = externalImportsProvider.getScriptConfiguration(file);
                        if (refinedConfiguration != null) {
                            files.addAll(refinedConfiguration.getDependenciesClassPath());
                        }
                    }
            );
        }

        try {
            URL[] result = new URL[files.size()];
            for (int i = 0; i < files.size(); i++) {
                result[i] = files.get(i).toURI().toURL();
            }
            return result;
        }
        catch (MalformedURLException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @NotNull
    protected String generateToText() {
        return generateToText(null);
    }

    @NotNull
    protected String generateToText(@Nullable String ignorePathPrefix) {
        if (classFileFactory == null) {
            classFileFactory = generateFiles(myEnvironment, myFiles);
        }
        return classFileFactory.createText(ignorePathPrefix);
    }

    @NotNull
    protected Map<String, String> generateEachFileToText() {
        if (classFileFactory == null) {
            classFileFactory = generateFiles(myEnvironment, myFiles);
        }
        return classFileFactory.createTextForEachFile();
    }

    @NotNull
    protected Class<?> generateFacadeClass() {
        FqName facadeClassFqName = JvmFileClassUtil.getFileClassInfoNoResolve(myFiles.getPsiFile()).getFacadeClassFqName();
        return generateClass(facadeClassFqName.asString());
    }

    @NotNull
    protected Class<?> generateClass(@NotNull String name) {
        return generateClass(name, true);
    }

    @NotNull
    protected Class<?> generateClass(@NotNull String name, boolean reportProblems) {
        try {
            return generateAndCreateClassLoader(reportProblems).loadClass(name);
        }
        catch (ClassNotFoundException e) {
            fail("No class file was generated for: " + name);
            return null;
        }
    }

    @NotNull
    protected ClassFileFactory generateClassesInFile() {
        return generateClassesInFile(true);
    }

    @NotNull
    private ClassFileFactory generateClassesInFile(boolean reportProblems) {
        if (classFileFactory != null) return classFileFactory;

        try {
            GenerationState generationState = GenerationUtils.compileFiles(
                    myFiles.getPsiFiles(), myEnvironment, getClassBuilderFactory(),
                    new NoScopeRecordCliBindingTrace()
            );
            classFileFactory = generationState.getFactory();

            // Some names are not allowed in the dex file format and the VM will reject the program
            // if they are used. Therefore, a few tests cannot be dexed as they use such names that
            // are valid on the JVM but not on the Android Runtime.
            boolean ignoreDexing = myFiles.getPsiFiles().stream().anyMatch(
                it -> InTextDirectivesUtils.isDirectiveDefined(it.getText(), "IGNORE_DEXING")
            );
            if (verifyWithDex() && D8Checker.RUN_D8_CHECKER && !ignoreDexing) {
                D8Checker.check(classFileFactory);
            }
        }
        catch (TestsCompiletimeError e) {
            if (reportProblems) {
                e.getOriginal().printStackTrace();
                generateInstructionsAsText();
                System.err.println("See exceptions above");
            }
            else {
                System.err.println("Compilation failure");
            }
            throw e;
        }
        catch (Throwable e) {
            if (reportProblems) {
                generateInstructionsAsText();
            }
            throw new TestsCompilerError(e);
        }
        return classFileFactory;
    }

    private void generateInstructionsAsText() {
        System.err.println("Generating instructions as text...");
        try {
            if (classFileFactory == null) {
                System.err.println("Cannot generate text: exception was thrown during generation");
            }
            else {
                System.err.println(classFileFactory.createText());
            }
        }
        catch (Throwable e1) {
            System.err.println("Exception thrown while trying to generate text, the actual exception follows:");
            e1.printStackTrace();
            System.err.println("-----------------------------------------------------------------------------");
        }
    }

    protected boolean verifyWithDex() {
        return true;
    }

    @NotNull
    protected Method generateFunction() {
        Class<?> aClass = generateFacadeClass();
        try {
            return findTheOnlyMethod(aClass);
        }
        catch (Error e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    @NotNull
    protected Method generateFunction(@NotNull String name) {
        return findDeclaredMethodByName(generateFacadeClass(), name);
    }

    @Override
    protected void updateConfiguration(@NotNull CompilerConfiguration configuration) {
        setCustomDefaultJvmTarget(configuration);
    }

    protected ClassBuilderFactory getClassBuilderFactory() {
        return ClassBuilderFactories.TEST;
    }

    private static void setCustomDefaultJvmTarget(CompilerConfiguration configuration) {
        if (DEFAULT_JVM_TARGET != null) {
            JvmTarget customDefaultTarget = JvmTarget.fromString(DEFAULT_JVM_TARGET);
            assert customDefaultTarget != null : "Can't construct JvmTarget for " + DEFAULT_JVM_TARGET;
            JvmTarget originalTarget = configuration.get(JVMConfigurationKeys.JVM_TARGET);
            if (originalTarget == null || customDefaultTarget.getMajorVersion() > originalTarget.getMajorVersion()) {
                // It's not safe to substitute target in general
                // cause it can affect generated bytecode and original behaviour should be tested somehow.
                // Original behaviour testing is perfomed by
                //
                //      codegenTest(target = 6, jvm = "Last", jdk = mostRecentJdk)
                //      codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk)
                //
                // in compiler/tests-different-jdk/build.gradle.kts
                configuration.put(JVMConfigurationKeys.JVM_TARGET, customDefaultTarget);
            }
        }
    }

    protected void compile(@NotNull List<TestFile> files) {
        compile(files, true, false);
    }

    protected void compile(@NotNull List<TestFile> files, boolean reportProblems, boolean dumpKotlinFiles) {
        File javaSourceDir = writeJavaFiles(files);

        configurationKind = extractConfigurationKind(files);

        CompilerConfiguration configuration = createConfiguration(
                configurationKind, getTestJdkKind(files), getBackend(),
                Collections.singletonList(getAnnotationsJar()),
                ArraysKt.filterNotNull(new File[] {javaSourceDir}),
                files
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
        setupEnvironment(myEnvironment);

        loadMultiFiles(files);

        generateClassesInFile(reportProblems);

        boolean compileJavaFiles = javaSourceDir != null && javaClassesOutputDirectory == null;
        File kotlinOut = null;

        // If there are Java files, they should be compiled against the class files produced by Kotlin, so we dump them to the disk
        if (dumpKotlinFiles || compileJavaFiles) {
            kotlinOut = getKotlinClassesOutputDirectory();
            OutputUtilsKt.writeAllTo(classFileFactory, kotlinOut);
        }

        javaClassesOutputDirectory = null;
        if (compileJavaFiles) {
            List<String> javaClasspath = new ArrayList<>();
            javaClasspath.add(kotlinOut.getPath());

            updateJavaClasspath(javaClasspath);

            javaClassesOutputDirectory = getJavaClassesOutputDirectory();
            List<String> javacOptions = extractJavacOptions(
                    files,
                    configuration.get(JVMConfigurationKeys.JVM_TARGET),
                    configuration.getBoolean(JVMConfigurationKeys.ENABLE_JVM_PREVIEW)
            );
            List<String> finalJavacOptions = prepareJavacOptions(javaClasspath, javacOptions, javaClassesOutputDirectory);

            try {
                runJavacTask(
                        findJavaSourcesInDirectory(javaSourceDir).stream().map(File::new).collect(Collectors.toList()),
                        finalJavacOptions
                );
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }
        }
        if (kotlinOut != null) {
            postCompile(kotlinOut, javaClassesOutputDirectory);
        }
    }

    protected void postCompile(@NotNull File kotlinOut, @Nullable File javaOut) {

    }

    protected void runJavacTask(@NotNull Collection<File> files, @NotNull List<String> options) throws IOException {
        KotlinTestUtils.compileJavaFiles(files, options);
    }

    protected void updateJavaClasspath(@NotNull List<String> javaClasspath) {}

    @NotNull
    protected static List<String> extractJavacOptions(
            @NotNull List<TestFile> files,
            @Nullable JvmTarget kotlinTarget,
            boolean isJvmPreviewEnabled
    ) {
        List<String> javacOptions = new ArrayList<>(0);
        for (TestFile file : files) {
            javacOptions.addAll(InTextDirectivesUtils.findListWithPrefixes(file.content, "// JAVAC_OPTIONS:"));
        }

        if (kotlinTarget != null && isJvmPreviewEnabled) {
            javacOptions.add("--release");
            javacOptions.add(kotlinTarget.getDescription());
            javacOptions.add("--enable-preview");
            return javacOptions;
        }

        String javaTarget = CodegenTestUtil.computeJavaTarget(javacOptions, kotlinTarget);
        if (javaTarget != null) {
            javacOptions.add("-source");
            javacOptions.add(javaTarget);
            javacOptions.add("-target");
            javacOptions.add(javaTarget);
        }
        return javacOptions;
    }

    @NotNull
    @Override
    protected TargetBackend getBackend() {
        return TargetBackend.JVM;
    }

    @Override
    protected void doTest(@NotNull String filePath) {
        doTestWithTransformer(filePath, s -> s);
    }

    protected void doTestWithTransformer(@NotNull String filePath, @NotNull Function<String, String> sourceTransformer) {
        File file = new File(filePath);

        String expectedText = sourceTransformer.apply(KtTestUtil.doLoadFile(file));
        List<TestFile> testFiles = createTestFilesFromFile(file, expectedText);

        try {
            doMultiFileTest(file, testFiles);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @NotNull
    protected List<TestFile> createTestFilesFromFile(@NotNull File file, @NotNull String expectedText) {
        return createTestFilesFromFile(file, expectedText, parseDirectivesPerFiles(), getBackend());
    }

    @NotNull
    public static List<TestFile> createTestFilesFromFile(
            @NotNull File file,
            @NotNull String expectedText,
            boolean parseDirectivesPerFiles,
            @NotNull TargetBackend backend
    ) {
        List<TestFile> testFiles =
                TestFiles.createTestFiles(file.getName(), expectedText, new TestFiles.TestFileFactoryNoModules<TestFile>() {
                    @NotNull
                    @Override
                    public TestFile create(@NotNull String fileName, @NotNull String text, @NotNull Directives directives) {
                        return new TestFile(fileName, text, directives);
                    }
                }, false, parseDirectivesPerFiles);
        if (InTextDirectivesUtils.isDirectiveDefined(expectedText, "WITH_HELPERS")) {
            testFiles.add(new TestFile("CodegenTestHelpers.kt", TestHelperGeneratorKt.createTextForCodegenTestHelpers(backend)));
        }
        return testFiles;
    }


    @NotNull
    protected File getJavaSourcesOutputDirectory() {
        return createTempDirectory("java-files");
    }

    @NotNull
    protected File getJavaClassesOutputDirectory() {
        return createTempDirectory("java-classes");
    }

    protected File getKotlinClassesOutputDirectory() {
        return createTempDirectory(toString());
    }

    @NotNull
    private static File createTempDirectory(String prefix) {
        try {
            return KtTestUtil.tmpDir(prefix);
        } catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @Nullable
    protected File writeJavaFiles(@NotNull List<TestFile> files) {
        List<TestFile> javaFiles = CollectionsKt.filter(files, file -> file.name.endsWith(".java"));
        if (javaFiles.isEmpty()) return null;

        File dir = getJavaSourcesOutputDirectory();

        for (TestFile testFile : javaFiles) {
            File file = new File(dir, testFile.name);
            KtTestUtil.mkdirs(file.getParentFile());
            FilesKt.writeText(file, testFile.content, Charsets.UTF_8);
        }

        return dir;
    }

    protected void callBoxMethodAndCheckResult(URLClassLoader classLoader, String className)
            throws IOException, InvocationTargetException, IllegalAccessException {
        Class<?> aClass = getGeneratedClass(classLoader, className);
        Method method = getBoxMethodOrNull(aClass);
        assertNotNull("Can't find box method in " + aClass, method);
        callBoxMethodAndCheckResult(classLoader, aClass, method);
    }

    private void callBoxMethodAndCheckResult(URLClassLoader classLoader, Class<?> aClass, Method method)
            throws IOException, IllegalAccessException, InvocationTargetException {
        callBoxMethodAndCheckResult(classLoader, aClass, method, false);
    }

    protected void callBoxMethodAndCheckResult(
            URLClassLoader classLoader,
            Class<?> aClass,
            Method method,
            boolean unexpectedBehaviour
    ) throws IOException, IllegalAccessException, InvocationTargetException {
        String result;
        if (BOX_IN_SEPARATE_PROCESS_PORT != null) {
            result = invokeBoxInSeparateProcess(classLoader, aClass);
        }
        else {
            ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
            if (savedClassLoader != classLoader) {
                // otherwise the test infrastructure used in the test may conflict with the one from the context classloader
                Thread.currentThread().setContextClassLoader(classLoader);
            }
            try {
                result = runBoxMethod(method);
            }
            finally {
                if (savedClassLoader != classLoader) {
                    Thread.currentThread().setContextClassLoader(savedClassLoader);
                }
            }
        }
        if (unexpectedBehaviour) {
            assertNotSame("OK", result);
        } else {
            assertEquals("OK", result);
        }
    }

    @NotNull
    private String invokeBoxInSeparateProcess(URLClassLoader classLoader, Class<?> aClass) throws IOException {
        List<URL> classPath = extractUrls(classLoader);
        if (classLoader instanceof GeneratedClassLoader) {
            File outDir = KotlinTestUtils.tmpDirForTest(this);
            SimpleOutputFileCollection currentOutput =
                    new SimpleOutputFileCollection(((GeneratedClassLoader) classLoader).getAllGeneratedFiles());
            writeAllTo(currentOutput, outDir);
            classPath.add(0, outDir.toURI().toURL());
        }

        return new TestProxy(Integer.valueOf(BOX_IN_SEPARATE_PROCESS_PORT), aClass.getCanonicalName(), classPath).runTest();
    }

    protected void printReport(File wholeFile) {
        boolean isIgnored = InTextDirectivesUtils.isIgnoredTarget(getBackend(), wholeFile, getIgnoreBackendDirectivePrefix());
        if (!isIgnored) {
            System.out.println(generateToText());
        }
    }

    protected String[] getIgnoreBackendDirectivePrefix() {
        return InTextDirectivesUtils.IGNORE_BACKEND_DIRECTIVE_PREFIXES;
    }
}
