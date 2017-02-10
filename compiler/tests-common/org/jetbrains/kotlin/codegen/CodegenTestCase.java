/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.TestDataFile;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFile;
import org.jetbrains.kotlin.checkers.CheckerTestUtil;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
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
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.codegen.AbstractBlackBoxCodegenTest.findJavaSourcesInDirectory;
import static org.jetbrains.kotlin.codegen.CodegenTestUtil.*;
import static org.jetbrains.kotlin.test.KotlinTestUtils.getAnnotationsJar;

public abstract class CodegenTestCase extends KtUsefulTestCase {
    private static final String DEFAULT_TEST_FILE_NAME = "a_test";

    protected KotlinCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;
    protected ClassFileFactory classFileFactory;
    protected GeneratedClassLoader initializedClassLoader;
    protected ConfigurationKind configurationKind = ConfigurationKind.JDK_ONLY;

    protected final void createEnvironmentWithMockJdkAndIdeaAnnotations(
            @NotNull ConfigurationKind configurationKind,
            @Nullable File... javaSourceRoots
    ) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind, Collections.<TestFile>emptyList(), javaSourceRoots);
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
            @Nullable File... javaSourceRoots
    ) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }

        CompilerConfiguration configuration = createConfiguration(
                configurationKind,
                TestJdkKind.MOCK_JDK,
                Collections.singletonList(getAnnotationsJar()),
                ArraysKt.filterNotNull(javaSourceRoots),
                testFilesWithConfigurationDirectives
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    @NotNull
    protected static CompilerConfiguration createConfiguration(
            @NotNull ConfigurationKind kind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<File> classpath,
            @NotNull List<File> javaSource,
            @NotNull List<TestFile> testFilesWithConfigurationDirectives
    ) {
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, javaSource);

        updateConfigurationByDirectivesInTestFiles(testFilesWithConfigurationDirectives, configuration);

        return configuration;
    }

    private static void updateConfigurationByDirectivesInTestFiles(
            @NotNull List<TestFile> testFilesWithConfigurationDirectives,
            @NotNull CompilerConfiguration configuration
    ) {
        List<String> kotlinConfigurationFlags = new ArrayList<String>(0);
        LanguageVersion explicitLanguageVersion = null;
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
                assert explicitLanguageVersion == null : "Should not specify LANGUAGE_VERSION twice";
                explicitLanguageVersion = LanguageVersion.fromVersionString(version);
            }
        }

        if (explicitLanguageVersion != null) {
            CommonConfigurationKeysKt.setLanguageVersionSettings(
                    configuration,
                    new LanguageVersionSettingsImpl(explicitLanguageVersion, ApiVersion.createByLanguageVersion(explicitLanguageVersion))
            );
        }

        updateConfigurationWithFlags(configuration, kotlinConfigurationFlags);
    }

    private static final Map<String, Class<?>> FLAG_NAMESPACE_TO_CLASS = ImmutableMap.of(
            "CLI", CLIConfigurationKeys.class,
            "JVM", JVMConfigurationKeys.class
    );

    private static final List<Class<?>> FLAG_CLASSES = ImmutableList.of(CLIConfigurationKeys.class, JVMConfigurationKeys.class);

    private static final Pattern BOOLEAN_FLAG_PATTERN = Pattern.compile("([+-])(([a-zA-Z_0-9]*)\\.)?([a-zA-Z_0-9]*)");

    private static void updateConfigurationWithFlags(@NotNull CompilerConfiguration configuration, @NotNull List<String> flags) {
        for (String flag : flags) {
            Matcher m = BOOLEAN_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                boolean flagEnabled = !"-".equals(m.group(1));
                String flagNamespace = m.group(3);
                String flagName = m.group(4);

                tryApplyBooleanFlag(configuration, flag, flagEnabled, flagNamespace, flagName);
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
            //noinspection unchecked
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
        } catch (IOException e) {
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

        List<KtFile> ktFiles = new ArrayList<KtFile>(files.size());
        for (TestFile file : files) {
            if (file.name.endsWith(".kt")) {
                String content = CheckerTestUtil.parseDiagnosedRanges(file.content, new ArrayList<CheckerTestUtil.DiagnosedRange>(0));
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
        String stringToCut = codegenTestBasePath();
        String systemIndependentPath = file.getPath().replace(File.separatorChar, '/');
        assert systemIndependentPath.startsWith(stringToCut) : "File path is not absolute: " + file;
        return systemIndependentPath.substring(stringToCut.length());
    }

    @NotNull
    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    protected GeneratedClassLoader generateAndCreateClassLoader() {
        if (initializedClassLoader != null) {
            fail("Double initialization of class loader in same test");
        }

        initializedClassLoader = createClassLoader();

        if (!verifyAllFilesWithAsm(generateClassesInFile(), initializedClassLoader)) {
            fail("Verification failed: see exceptions above");
        }

        return initializedClassLoader;
    }

    @NotNull
    protected GeneratedClassLoader createClassLoader() {
        return new GeneratedClassLoader(
                generateClassesInFile(),
                configurationKind.getWithReflection() ? ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
                                                      : ForTestCompileRuntime.runtimeJarClassLoader(),
                getClassPathURLs()
        );
    }

    @NotNull
    private URL[] getClassPathURLs() {
        List<URL> urls = Lists.newArrayList();
        for (File file : JvmContentRootsKt.getJvmClasspathRoots(myEnvironment.getConfiguration())) {
            try {
                urls.add(file.toURI().toURL());
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }

        return urls.toArray(new URL[urls.size()]);
    }

    @NotNull
    protected String generateToText() {
        if (classFileFactory == null) {
            classFileFactory = generateFiles(myEnvironment, myFiles);
        }
        return classFileFactory.createText();
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
        try {
            return generateAndCreateClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException e) {
            fail("No class file was generated for: " + name);
            return null;
        }
    }

    @NotNull
    protected ClassFileFactory generateClassesInFile() {
        if (classFileFactory == null) {
            try {
                classFileFactory = generateFiles(myEnvironment, myFiles);

                if (DxChecker.RUN_DX_CHECKER) {
                    DxChecker.check(classFileFactory);
                }
            }
            catch (Throwable e) {
                e.printStackTrace();
                System.err.println("Generating instructions as text...");
                try {
                    if (classFileFactory == null) {
                        System.out.println("Cannot generate text: exception was thrown during generation");
                    }
                    else {
                        System.out.println(classFileFactory.createText());
                    }
                }
                catch (Throwable e1) {
                    System.err.println("Exception thrown while trying to generate text, the actual exception follows:");
                    e1.printStackTrace();
                    System.err.println("-----------------------------------------------------------------------------");
                }
                fail("See exceptions above");
            }
        }
        return classFileFactory;
    }

    private static boolean verifyAllFilesWithAsm(ClassFileFactory factory, ClassLoader loader) {
        boolean noErrors = true;
        for (OutputFile file : ClassFileUtilsKt.getClassFiles(factory)) {
            noErrors &= verifyWithAsm(file, loader);
        }
        return noErrors;
    }

    private static boolean verifyWithAsm(@NotNull OutputFile file, ClassLoader loader) {
        ClassNode classNode = new ClassNode();
        new ClassReader(file.asByteArray()).accept(classNode, 0);

        SimpleVerifier verifier = new SimpleVerifier();
        verifier.setClassLoader(loader);
        Analyzer<BasicValue> analyzer = new Analyzer<BasicValue>(verifier);

        boolean noErrors = true;
        for (MethodNode method : classNode.methods) {
            try {
                analyzer.analyze(classNode.name, method);
            }
            catch (Throwable e) {
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
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    @NotNull
    protected Method generateFunction(@NotNull String name) {
        return findDeclaredMethodByName(generateFacadeClass(), name);
    }

    @NotNull
    public Class<? extends Annotation> loadAnnotationClassQuietly(@NotNull String fqName) {
        try {
            //noinspection unchecked
            return (Class<? extends Annotation>) initializedClassLoader.loadClass(fqName);
        }
        catch (ClassNotFoundException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    protected void updateConfiguration(CompilerConfiguration configuration) {

    }

    protected void compile(
            @NotNull List<TestFile> files,
            @Nullable File javaSourceDir,
            @NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<String> javacOptions
    ) {
        CompilerConfiguration configuration = createConfiguration(
                configurationKind, jdkKind,
                Collections.singletonList(getAnnotationsJar()),
                ArraysKt.filterNotNull(new File[] {javaSourceDir}),
                files
        );
        updateConfiguration(configuration);

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );

        loadMultiFiles(files);

        classFileFactory = GenerationUtils.compileFiles(myFiles.getPsiFiles(), myEnvironment).getFactory();

        if (javaSourceDir != null) {
            // If there are Java files, they should be compiled against the class files produced by Kotlin, so we dump them to the disk
            File kotlinOut;
            try {
                kotlinOut = KotlinTestUtils.tmpDir(toString());
            }
            catch (IOException e) {
                throw ExceptionUtilsKt.rethrow(e);
            }

            OutputUtilsKt.writeAllTo(classFileFactory, kotlinOut);

            File output = CodegenTestUtil.compileJava(
                    findJavaSourcesInDirectory(javaSourceDir), Collections.singletonList(kotlinOut.getPath()), javacOptions
            );
            // Add javac output to classpath so that the created class loader can find generated Java classes
            JvmContentRootsKt.addJvmClasspathRoot(configuration, output);
        }
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
        Ref<File> javaFilesDir = Ref.create();

        List<TestFile> testFiles = createTestFiles(file, expectedText, javaFilesDir);

        doMultiFileTest(file, testFiles, javaFilesDir.get());
    }

    @NotNull
    private static List<TestFile> createTestFiles(File file, String expectedText, final Ref<File> javaFilesDir) {
        return KotlinTestUtils.createTestFiles(file.getName(), expectedText, new KotlinTestUtils.TestFileFactoryNoModules<TestFile>() {
            @NotNull
            @Override
            public TestFile create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
                if (fileName.endsWith(".java")) {
                    if (javaFilesDir.isNull()) {
                        try {
                            javaFilesDir.set(KotlinTestUtils.tmpDir("java-files"));
                        }
                        catch (IOException e) {
                            throw ExceptionUtilsKt.rethrow(e);
                        }
                    }
                    writeSourceFile(fileName, text, javaFilesDir.get());
                }

                return new TestFile(fileName, text);
            }

            private void writeSourceFile(@NotNull String fileName, @NotNull String content, @NotNull File targetDir) {
                File file = new File(targetDir, fileName);
                KotlinTestUtils.mkdirs(file.getParentFile());
                FilesKt.writeText(file, content, Charsets.UTF_8);
            }
        });
    }

    protected void doMultiFileTest(@NotNull File wholeFile, @NotNull List<TestFile> files, @Nullable File javaFilesDir) throws Exception {
        throw new UnsupportedOperationException("Multi-file test cases are not supported in this test");
    }
}
