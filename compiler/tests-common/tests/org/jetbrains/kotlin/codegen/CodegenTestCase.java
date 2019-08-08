/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.TestDataFile;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.script.experimental.api.ErrorHandlingKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.TestsCompilerError;
import org.jetbrains.kotlin.TestsCompiletimeError;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.backend.common.output.SimpleOutputFileCollection;
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings;
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.output.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.scripting.definitions.ScriptDependenciesProvider;
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationWrapper;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.clientserver.TestProxy;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.tree.ClassNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.Analyzer;
import org.jetbrains.org.objectweb.asm.tree.analysis.AnalyzerException;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;
import org.jetbrains.org.objectweb.asm.tree.analysis.SimpleVerifier;
import org.jetbrains.org.objectweb.asm.util.Textifier;
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettingsKt.parseLanguageVersionSettings;
import static org.jetbrains.kotlin.cli.common.output.OutputUtilsKt.writeAllTo;
import static org.jetbrains.kotlin.codegen.CodegenTestUtil.*;
import static org.jetbrains.kotlin.codegen.TestUtilsKt.extractUrls;
import static org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getBoxMethodOrNull;
import static org.jetbrains.kotlin.test.clientserver.TestProcessServerKt.getGeneratedClass;

public abstract class CodegenTestCase extends KtUsefulTestCase {
    private static final String DEFAULT_TEST_FILE_NAME = "a_test";
    private static final String DEFAULT_JVM_TARGET = System.getProperty("kotlin.test.default.jvm.target");
    public static final String BOX_IN_SEPARATE_PROCESS_PORT = System.getProperty("kotlin.test.box.in.separate.process.port");
    private static final String JAVA_COMPILATION_TARGET = System.getProperty("kotlin.test.java.compilation.target");

    protected KotlinCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;
    protected ClassFileFactory classFileFactory;
    protected GeneratedClassLoader initializedClassLoader;
    protected File javaClassesOutputDirectory = null;
    protected List<File> additionalDependencies = null;
    protected String coroutinesPackage = "";

    protected ConfigurationKind configurationKind = ConfigurationKind.JDK_ONLY;

    protected final void createEnvironmentWithMockJdkAndIdeaAnnotations(
            @NotNull ConfigurationKind configurationKind,
            @NotNull File... javaSourceRoots
    ) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind, Collections.emptyList(), TestJdkKind.MOCK_JDK, javaSourceRoots);
    }

    @NotNull
    protected static TestJdkKind getJdkKind(@NotNull List<TestFile> files) {
        for (TestFile file : files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "FULL_JDK")) {
                return TestJdkKind.FULL_JDK;
            }
        }
        return TestJdkKind.MOCK_JDK;
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
                Collections.singletonList(getAnnotationsJar()),
                ArraysKt.filterNotNull(javaSourceRoots),
                testFilesWithConfigurationDirectives
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    @NotNull
    protected CompilerConfiguration createConfiguration(
            @NotNull ConfigurationKind kind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<File> classpath,
            @NotNull List<File> javaSource,
            @NotNull List<TestFile> testFilesWithConfigurationDirectives
    ) {
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, javaSource);

        updateConfigurationByDirectivesInTestFiles(testFilesWithConfigurationDirectives, configuration, coroutinesPackage);
        updateConfiguration(configuration);
        setCustomDefaultJvmTarget(configuration);

        return configuration;
    }

    protected static void updateConfigurationByDirectivesInTestFiles(
            @NotNull List<TestFile> testFilesWithConfigurationDirectives,
            @NotNull CompilerConfiguration configuration
    ) {
        updateConfigurationByDirectivesInTestFiles(testFilesWithConfigurationDirectives, configuration, "");
    }

    private static void updateConfigurationByDirectivesInTestFiles(
            @NotNull List<TestFile> testFilesWithConfigurationDirectives,
            @NotNull CompilerConfiguration configuration,
            @NotNull String coroutinesPackage
    ) {
        LanguageVersionSettings explicitLanguageVersionSettings = null;
        boolean disableReleaseCoroutines = false;

        List<String> kotlinConfigurationFlags = new ArrayList<>(0);
        for (TestFile testFile : testFilesWithConfigurationDirectives) {
            kotlinConfigurationFlags.addAll(InTextDirectivesUtils.findListWithPrefixes(testFile.content, "// KOTLIN_CONFIGURATION_FLAGS:"));

            List<String> lines = InTextDirectivesUtils.findLinesWithPrefixesRemoved(testFile.content, "// JVM_TARGET:");
            if (!lines.isEmpty()) {
                String targetString = CollectionsKt.single(lines);
                JvmTarget jvmTarget = JvmTarget.Companion.fromString(targetString);
                assert jvmTarget != null : "Unknown target: " + targetString;
                configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget);
            }

            String version = InTextDirectivesUtils.findStringWithPrefixes(testFile.content, "// LANGUAGE_VERSION:");
            if (version != null) {
                throw new AssertionError(
                        "Do not use LANGUAGE_VERSION directive in compiler tests because it's prone to limiting the test\n" +
                        "to a specific language version, which will become obsolete at some point and the test won't check\n" +
                        "things like feature intersection with newer releases. Use `// !LANGUAGE: [+-]FeatureName` directive instead,\n" +
                        "where FeatureName is an entry of the enum `LanguageFeature`\n"
                );
            }

            if (!InTextDirectivesUtils.findLinesWithPrefixesRemoved(testFile.content, "// COMMON_COROUTINES_TEST").isEmpty()) {
                assert !testFile.content.contains("COROUTINES_PACKAGE") : "Must replace COROUTINES_PACKAGE prior to tests compilation";
                if (coroutinesPackage.equals("kotlin.coroutines.experimental")) {
                    disableReleaseCoroutines = true;
                }
            }

            Map<String, String> directives = KotlinTestUtils.parseDirectives(testFile.content);

            LanguageVersionSettings fileLanguageVersionSettings = parseLanguageVersionSettings(directives);
            if (fileLanguageVersionSettings != null) {
                assert explicitLanguageVersionSettings == null : "Should not specify !LANGUAGE directive twice";
                explicitLanguageVersionSettings = fileLanguageVersionSettings;
            }
        }

        if (disableReleaseCoroutines) {
            explicitLanguageVersionSettings = new CompilerTestLanguageVersionSettings(
                    Collections.singletonMap(LanguageFeature.ReleaseCoroutines, LanguageFeature.State.DISABLED),
                    ApiVersion.LATEST_STABLE,
                    LanguageVersion.LATEST_STABLE,
                    Collections.emptyMap()
            );
        }

        if (explicitLanguageVersionSettings != null) {
            CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, explicitLanguageVersionSettings);
        }

        updateConfigurationWithFlags(configuration, kotlinConfigurationFlags);
    }

    private static final Map<String, Class<?>> FLAG_NAMESPACE_TO_CLASS = ImmutableMap.of(
            "CLI", CLIConfigurationKeys.class,
            "JVM", JVMConfigurationKeys.class
    );

    private static final List<Class<?>> FLAG_CLASSES = ImmutableList.of(CLIConfigurationKeys.class, JVMConfigurationKeys.class);

    private static final Pattern BOOLEAN_FLAG_PATTERN = Pattern.compile("([+-])(([a-zA-Z_0-9]*)\\.)?([a-zA-Z_0-9]*)");
    private static final Pattern CONSTRUCTOR_CALL_NORMALIZATION_MODE_FLAG_PATTERN = Pattern.compile(
            "CONSTRUCTOR_CALL_NORMALIZATION_MODE=([a-zA-Z_\\-0-9]*)");
    private static final Pattern ASSERTIONS_MODE_FLAG_PATTERN = Pattern.compile("ASSERTIONS_MODE=([a-zA-Z_0-9-]*)");

    private static void updateConfigurationWithFlags(@NotNull CompilerConfiguration configuration, @NotNull List<String> flags) {
        for (String flag : flags) {
            Matcher m = BOOLEAN_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                boolean flagEnabled = !"-".equals(m.group(1));
                String flagNamespace = m.group(3);
                String flagName = m.group(4);

                tryApplyBooleanFlag(configuration, flag, flagEnabled, flagNamespace, flagName);
                continue;
            }

            m = CONSTRUCTOR_CALL_NORMALIZATION_MODE_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                String flagValueString = m.group(1);
                JVMConstructorCallNormalizationMode mode = JVMConstructorCallNormalizationMode.fromStringOrNull(flagValueString);
                assert mode != null : "Wrong CONSTRUCTOR_CALL_NORMALIZATION_MODE value: " + flagValueString;
                configuration.put(JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE, mode);
            }

            m = ASSERTIONS_MODE_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                String flagValueString = m.group(1);
                JVMAssertionsMode mode = JVMAssertionsMode.fromStringOrNull(flagValueString);
                assert mode != null : "Wrong ASSERTIONS_MODE value: " + flagValueString;
                configuration.put(JVMConfigurationKeys.ASSERTIONS_MODE, mode);
            }
        }
    }

    private static void tryApplyBooleanFlag(
            @NotNull CompilerConfiguration configuration,
            @NotNull String flag,
            boolean flagEnabled,
            @Nullable String flagNamespace,
            @NotNull String flagName
    ) {
        Class<?> configurationKeysClass;
        Field configurationKeyField = null;
        if (flagNamespace == null) {
            for (Class<?> flagClass : FLAG_CLASSES) {
                try {
                    configurationKeyField = flagClass.getField(flagName);
                    break;
                }
                catch (Exception ignored) {
                }
            }
        }
        else {
            configurationKeysClass = FLAG_NAMESPACE_TO_CLASS.get(flagNamespace);
            assert configurationKeysClass != null : "Expected [+|-][namespace.]configurationKey, got: " + flag;
            try {
                configurationKeyField = configurationKeysClass.getField(flagName);
            }
            catch (Exception e) {
                configurationKeyField = null;
            }
        }
        assert configurationKeyField != null : "Expected [+|-][namespace.]configurationKey, got: " + flag;

        try {
            @SuppressWarnings("unchecked")
            CompilerConfigurationKey<Boolean> configurationKey = (CompilerConfigurationKey<Boolean>) configurationKeyField.get(null);
            configuration.put(configurationKey, flagEnabled);
        }
        catch (Exception e) {
            assert false : "Expected [+|-][namespace.]configurationKey, got: " + flag;
        }
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
        return loadFileByFullPath(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + name);
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
            if (file.name.endsWith(".kt")) {
                // `rangesToDiagnosticNames` parameter is not-null only for diagnostic tests, it's using for lazy diagnostics
                String content = CheckerTestUtil.INSTANCE.parseDiagnosedRanges(file.content, new ArrayList<>(0), null);
                ktFiles.add(KotlinTestUtils.createFile(file.name, content, project));
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
    protected GeneratedClassLoader generateAndCreateClassLoader() {
        return generateAndCreateClassLoader(true);
    }

    @NotNull
    protected GeneratedClassLoader generateAndCreateClassLoader(boolean reportProblems) {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test");
        }

        initializedClassLoader = createClassLoader();

        if (!verifyAllFilesWithAsm(generateClassesInFile(reportProblems), initializedClassLoader, reportProblems)) {
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
                        ScriptCompilationConfigurationWrapper refinedConfiguration = ErrorHandlingKt.valueOrNull(externalImportsProvider.getScriptConfigurationResult(file));
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

            if (verifyWithDex() && DxChecker.RUN_DX_CHECKER) {
                DxChecker.check(classFileFactory);
            }
        }
        catch (TestsCompiletimeError e) {
            if (reportProblems) {
                e.getOriginal().printStackTrace();
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
                System.err.println("See exceptions above");
            }
            else {
                System.err.println("Compilation failure");
            }
            throw e;
        }
        catch (Throwable e) {
            throw new TestsCompilerError(e);
        }
        return classFileFactory;
    }

    protected boolean verifyWithDex() {
        return true;
    }

    private static boolean verifyAllFilesWithAsm(ClassFileFactory factory, ClassLoader loader, boolean reportProblems) {
        boolean noErrors = true;
        for (OutputFile file : ClassFileUtilsKt.getClassFiles(factory)) {
            noErrors &= verifyWithAsm(file, loader, reportProblems);
        }
        return noErrors;
    }

    private static boolean verifyWithAsm(@NotNull OutputFile file, ClassLoader loader, boolean reportProblems) {
        ClassNode classNode = new ClassNode();
        new ClassReader(file.asByteArray()).accept(classNode, 0);

        SimpleVerifier verifier = new SimpleVerifier();
        verifier.setClassLoader(loader);
        Analyzer<BasicValue> analyzer = new Analyzer<>(verifier);

        boolean noErrors = true;
        for (MethodNode method : classNode.methods) {
            try {
                analyzer.analyze(classNode.name, method);
            }
            catch (Throwable e) {
                if (reportProblems) {
                    System.err.println(file.asText());
                    System.err.println(classNode.name + "::" + method.name + method.desc);

                    //noinspection InstanceofCatchParameter
                    if (e instanceof AnalyzerException) {
                        // Print the erroneous instruction
                        TraceMethodVisitor tmv = new TraceMethodVisitor(new Textifier());
                        ((AnalyzerException) e).node.accept(tmv);
                        PrintWriter pw = new PrintWriter(System.err);
                        tmv.p.print(pw);
                        pw.flush();
                    }

                    e.printStackTrace();
                }
                noErrors = false;
            }
        }
        return noErrors;
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

    protected void updateConfiguration(@NotNull CompilerConfiguration configuration) {

    }

    protected ClassBuilderFactory getClassBuilderFactory() {
        return ClassBuilderFactories.TEST;
    }

    protected void setupEnvironment(@NotNull KotlinCoreEnvironment environment) {

    }

    protected void setCustomDefaultJvmTarget(CompilerConfiguration configuration) {
        if (DEFAULT_JVM_TARGET != null) {
            JvmTarget customDefaultTarget = JvmTarget.fromString(DEFAULT_JVM_TARGET);
            assert customDefaultTarget != null : "Can't construct JvmTarget for " + DEFAULT_JVM_TARGET;
            JvmTarget originalTarget = configuration.get(JVMConfigurationKeys.JVM_TARGET);
            if (originalTarget == null || customDefaultTarget.getBytecodeVersion() > originalTarget.getBytecodeVersion()) {
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
        compile(files, true);
    }

    protected void compile(@NotNull List<TestFile> files, boolean reportProblems) {
        File javaSourceDir = writeJavaFiles(files);

        configurationKind = extractConfigurationKind(files);
        boolean loadAndroidAnnotations = files.stream().anyMatch(
                it -> InTextDirectivesUtils.isDirectiveDefined(it.content, "ANDROID_ANNOTATIONS")
        );

        List<String> javacOptions = extractJavacOptions(files);
        List<File> classpath = new ArrayList<>();
        classpath.add(getAnnotationsJar());

        if (loadAndroidAnnotations) {
            classpath.add(ForTestCompileRuntime.androidAnnotationsForTests());
        }

        CompilerConfiguration configuration = createConfiguration(
                configurationKind, getJdkKind(files),
                classpath,
                ArraysKt.filterNotNull(new File[] {javaSourceDir}),
                files
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
        setupEnvironment(myEnvironment);

        loadMultiFiles(files);

        generateClassesInFile(reportProblems);

        if (javaSourceDir != null && javaClassesOutputDirectory == null) {
            // If there are Java files, they should be compiled against the class files produced by Kotlin, so we dump them to the disk
            File kotlinOut;
            try {
                kotlinOut = KotlinTestUtils.tmpDir(toString());
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }

            OutputUtilsKt.writeAllTo(classFileFactory, kotlinOut);

            List<String> javaClasspath = new ArrayList<>();
            javaClasspath.add(kotlinOut.getPath());

            if (loadAndroidAnnotations) {
                javaClasspath.add(ForTestCompileRuntime.androidAnnotationsForTests().getPath());
            }

            javaClassesOutputDirectory = CodegenTestUtil.compileJava(
                    findJavaSourcesInDirectory(javaSourceDir), javaClasspath, javacOptions
            );
        }
    }


    protected ConfigurationKind extractConfigurationKind(@NotNull List<TestFile> files) {
        boolean addRuntime = false;
        boolean addReflect = false;
        for (TestFile file : files) {
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_RUNTIME")) {
                addRuntime = true;
            }
            if (InTextDirectivesUtils.isDirectiveDefined(file.content, "WITH_REFLECT")) {
                addReflect = true;
            }
        }

        return addReflect ? ConfigurationKind.ALL :
               addRuntime ? ConfigurationKind.NO_KOTLIN_REFLECT :
               ConfigurationKind.JDK_ONLY;
    }

    @NotNull
    protected static List<String> extractJavacOptions(@NotNull List<TestFile> files) {
        List<String> javacOptions = new ArrayList<>(0);
        for (TestFile file : files) {
            javacOptions.addAll(InTextDirectivesUtils.findListWithPrefixes(file.content, "// JAVAC_OPTIONS:"));
        }
        updateJavacOptions(javacOptions);
        return javacOptions;
    }

    private static void updateJavacOptions(@NotNull List<String> javacOptions) {
        if (JAVA_COMPILATION_TARGET != null && !javacOptions.contains("-target")) {
            javacOptions.add("-source");
            javacOptions.add(JAVA_COMPILATION_TARGET);
            javacOptions.add("-target");
            javacOptions.add(JAVA_COMPILATION_TARGET);
        }
    }

    protected TargetBackend getBackend() {
        return TargetBackend.JVM;
    }

    public static class TestFile implements Comparable<TestFile> {
        public final String name;
        public final String content;

        public TestFile(@NotNull String name, @NotNull String content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public int compareTo(@NotNull TestFile o) {
            return name.compareTo(o.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof TestFile && ((TestFile) obj).name.equals(name);
        }

        @Override
        public String toString() {
            return name;
        }
    }

    protected void doTest(String filePath) throws Exception {
        File file = new File(filePath);

        String expectedText = KotlinTestUtils.doLoadFile(file);
        if (!coroutinesPackage.isEmpty()) {
            expectedText = expectedText.replace("COROUTINES_PACKAGE", coroutinesPackage);
        }

        List<TestFile> testFiles = createTestFiles(file, expectedText, coroutinesPackage);

        doMultiFileTest(file, testFiles);
    }

    protected void doTestWithCoroutinesPackageReplacement(String filePath, String packageName) throws Exception {
        this.coroutinesPackage = packageName;
        doTest(filePath);
    }

    @NotNull
    private static List<TestFile> createTestFiles(File file, String expectedText, String coroutinesPackage) {
        return KotlinTestUtils.createTestFiles(file.getName(), expectedText, new KotlinTestUtils.TestFileFactoryNoModules<TestFile>() {
            @NotNull
            @Override
            public TestFile create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
                return new TestFile(fileName, text);
            }
        }, coroutinesPackage);
    }

    @Nullable
    protected static File writeJavaFiles(@NotNull List<TestFile> files) {
        List<TestFile> javaFiles = CollectionsKt.filter(files, file -> file.name.endsWith(".java"));
        if (javaFiles.isEmpty()) return null;

        File dir;
        try {
            dir = KotlinTestUtils.tmpDir("java-files");
        }
        catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }

        for (TestFile testFile : javaFiles) {
            File file = new File(dir, testFile.name);
            KotlinTestUtils.mkdirs(file.getParentFile());
            FilesKt.writeText(file, testFile.content, Charsets.UTF_8);
        }

        return dir;
    }

    protected void doMultiFileTest(
            @NotNull File wholeFile,
            @NotNull List<TestFile> files
    ) throws Exception {
        throw new UnsupportedOperationException("Multi-file test cases are not supported in this test");
    }

    protected void callBoxMethodAndCheckResult(URLClassLoader classLoader, String className)
            throws IOException, InvocationTargetException, IllegalAccessException {
        Class<?> aClass = getGeneratedClass(classLoader, className);
        Method method = getBoxMethodOrNull(aClass);
        assertNotNull("Can't find box method in " + aClass, method);
        callBoxMethodAndCheckResult(classLoader, aClass, method);
    }

    protected void callBoxMethodAndCheckResult(URLClassLoader classLoader, Class<?> aClass, Method method)
            throws IOException, IllegalAccessException, InvocationTargetException {
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
                result = (String) method.invoke(null);
            }
            finally {
                if (savedClassLoader != classLoader) {
                    Thread.currentThread().setContextClassLoader(savedClassLoader);
                }
            }
        }
        assertEquals("OK", result);
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
        boolean isIgnored = InTextDirectivesUtils.isIgnoredTarget(getBackend(), wholeFile);
        if (!isIgnored) {
            System.out.println(generateToText());
        }
    }
}
