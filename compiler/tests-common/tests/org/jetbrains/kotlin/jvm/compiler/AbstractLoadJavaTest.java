/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettingsKt;
import org.jetbrains.kotlin.cli.common.config.ContentRootsKt;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace;
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.GenerationUtils;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.jetbrains.kotlin.test.util.JUnit4Assertions;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.Configuration;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettingsKt.parseLanguageVersionSettings;
import static org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.kotlin.test.KotlinTestUtils.compileKotlinWithJava;
import static org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesAllowed;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.RECURSIVE;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor.compareDescriptors;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile;

/*
    The generated test compares package descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {
    // There are two modules in each test case (sources and dependencies), so we should render declarations from both of them
    public static final Configuration
            COMPARATOR_CONFIGURATION = DONT_INCLUDE_METHODS_OF_OBJECT.renderDeclarationsFromOtherModules(true);

    protected boolean withForeignAnnotations() { return false; }

    protected void doTestCompiledJava(@NotNull String javaFileName) {
        try {
            doTestCompiledJava(javaFileName, COMPARATOR_CONFIGURATION);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Java-Kotlin dependencies are not supported in this method for simplicity
    protected void doTestCompiledJavaAndKotlin(@NotNull String expectedFileName) {
        File expectedFile = new File(expectedFileName);
        File sourcesDir = new File(expectedFileName.replaceFirst("\\.txt$", ""));

        if (useJavacWrapper()) return;

        List<File> kotlinSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.kt"), sourcesDir);

        KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), newConfiguration(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, getClasspath(),
                                                       Collections.emptyList()), EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
        registerJavacIfNeeded(environment);
        configureEnvironment(environment);

        compileKotlinToDirAndGetModule(kotlinSources, tmpdir, environment);

        List<File> javaSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.java"), sourcesDir);
        Pair<PackageViewDescriptor, BindingContext> binaryPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                javaSources, tmpdir, ConfigurationKind.JDK_ONLY, null
        );

        checkJavaPackage(expectedFile, binaryPackageAndContext.first, binaryPackageAndContext.second, COMPARATOR_CONFIGURATION);
    }

    @NotNull
    protected File getExpectedFile(@NotNull String expectedFileName) {
        return new File(expectedFileName);
    }

    @NotNull
    private List<File> getClasspath(File... files) {
        List<File> classpath = new ArrayList<>(getExtraClasspath());
        classpath.add(KtTestUtil.getAnnotationsJar());
        classpath.addAll(Arrays.asList(files));
        return classpath;
    }

    @NotNull
    protected List<File> getExtraClasspath() {
        return Collections.emptyList();
    }

    protected void doTestCompiledJavaIncludeObjectMethods(@NotNull String javaFileName) {
        doTestCompiledJava(javaFileName, RECURSIVE.renderDeclarationsFromOtherModules(true));
    }

    protected void doTestCompiledKotlin(@NotNull String ktFileName) {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_ONLY, false);
    }

    protected void doTestCompiledKotlinWithTypeTable(@NotNull String ktFileName) {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_ONLY, true);
    }

    protected void doTestCompiledKotlinWithStdlib(@NotNull String ktFileName) {
        try {
            doTestCompiledKotlin(ktFileName, ConfigurationKind.ALL, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doTestCompiledKotlin(
            @NotNull String ktFileName, @NotNull ConfigurationKind configurationKind, boolean useTypeTableInSerializer
    ) {
        try {
            doTestCompiledKotlinImpl(ktFileName, configurationKind, useTypeTableInSerializer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doTestCompiledKotlinImpl(
            @NotNull String ktFileName, @NotNull ConfigurationKind configurationKind, boolean useTypeTableInSerializer
    ) throws Exception {
        File ktFile = new File(ktFileName);
        File txtFile = getTxtFileFromKtFile(ktFileName);

        CompilerConfiguration configuration = newConfiguration(configurationKind, getJdkKind(), getClasspath(), Collections.emptyList());
        updateConfiguration(configuration);
        if (useTypeTableInSerializer) {
            configuration.put(JVMConfigurationKeys.USE_TYPE_TABLE, true);
        }
        String fileContent = FileUtil.loadFile(ktFile, true);
        updateConfigurationWithDirectives(fileContent, configuration);

        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        registerJavacIfNeeded(environment);
        configureEnvironment(environment);
        ModuleDescriptor module = compileKotlinToDirAndGetModule(Collections.singletonList(ktFile), tmpdir, environment);

        PackageViewDescriptor packageFromSource = module.getPackage(TEST_PACKAGE_FQNAME);
        Assert.assertEquals("test", packageFromSource.getName().asString());

        PackageViewDescriptor packageFromBinary = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), getJdkKind(), configurationKind, true, false, useJavacWrapper(),
                withForeignAnnotations(), configuration.get(CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS),
                getExtraClasspath(), this::configureEnvironment
        ).first;

        for (DeclarationDescriptor descriptor : DescriptorUtils.getAllDescriptors(packageFromBinary.getMemberScope())) {
            if (descriptor instanceof ClassDescriptor) {
                assert descriptor instanceof DeserializedClassDescriptor : DescriptorUtils.getFqName(descriptor) + " is loaded as " + descriptor.getClass();
            }
        }

        DescriptorValidator.validate(errorTypesForbidden(), packageFromSource);
        DescriptorValidator.validate(new DeserializedScopeValidationVisitor(), packageFromBinary);
        Configuration comparatorConfiguration = COMPARATOR_CONFIGURATION.checkPrimaryConstructors(true).checkPropertyAccessors(true).checkFunctionContracts(true);

        if (InTextDirectivesUtils.isDirectiveDefined(fileContent, "NO_CHECK_SOURCE_VS_BINARY")) {
            // If NO_CHECK_SOURCE_VS_BINARY is enabled, only check that binary descriptors correspond to the .txt file content
            validateAndCompareDescriptorWithFile(packageFromBinary, comparatorConfiguration, txtFile);
        }
        else {
            compareDescriptors(packageFromSource, packageFromBinary, comparatorConfiguration, txtFile);
        }
    }

    public static void updateConfigurationWithDirectives(String content, CompilerConfiguration configuration) {
        Directives directives = KotlinTestUtils.parseDirectives(content);
        LanguageVersionSettings languageVersionSettings = parseLanguageVersionSettings(directives);
        if (languageVersionSettings == null) {
            languageVersionSettings = CompilerTestLanguageVersionSettingsKt.defaultLanguageVersionSettings();
        }

        CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, languageVersionSettings);

        if (InTextDirectivesUtils.isDirectiveDefined(content, "JVM_ANNOTATIONS")) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.jvmAnnotationsForTests());
        }
    }

    protected boolean usePsiClassFilesReading() {
        return false;
    }

    protected boolean useJavacWrapper() { return false; }

    protected void registerJavacIfNeeded(KotlinCoreEnvironment environment) {}

    protected void configureEnvironment(KotlinCoreEnvironment environment) {}

    protected void updateConfiguration(CompilerConfiguration configuration) {}

    protected void doTestJavaAgainstKotlin(String expectedFileName) {
        try {
            doTestJavaAgainstKotlinImpl(expectedFileName);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doTestJavaAgainstKotlinImpl(String expectedFileName) throws Exception {
        File expectedFile = new File(expectedFileName);
        File sourcesDir = new File(expectedFileName.replaceFirst("\\.txt$", ""));

        FileUtil.copyDir(sourcesDir, new File(tmpdir, "test"), pathname -> pathname.getName().endsWith(".java"));

        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, getJdkKind());
        updateConfiguration(configuration);
        ContentRootsKt.addKotlinSourceRoot(configuration, sourcesDir.getAbsolutePath());
        JvmContentRootsKt.addJavaSourceRoot(configuration, new File("compiler/testData/loadJava/include"));
        JvmContentRootsKt.addJavaSourceRoot(configuration, tmpdir);

        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        registerJavacIfNeeded(environment);
        configureEnvironment(environment);
        AnalysisResult result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                environment.getProject(), environment.getSourceFiles(), new NoScopeRecordCliBindingTrace(),
                configuration, environment::createPackagePartProvider
        );

        PackageViewDescriptor packageView = result.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        checkJavaPackage(expectedFile, packageView, result.getBindingContext(), COMPARATOR_CONFIGURATION);
    }

    protected void doTestKotlinAgainstCompiledJavaWithKotlin(@NotNull String expectedFileName) {
        try {
            doTestKotlinAgainstCompiledJavaWithKotlinImpl(expectedFileName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: add more tests on inherited parameter names, but currently impossible because of KT-4509
    private void doTestKotlinAgainstCompiledJavaWithKotlinImpl(@NotNull String expectedFileName) throws Exception {
        File kotlinSrc = new File(expectedFileName);
        File librarySrc = new File(expectedFileName.replaceFirst("\\.kt$", ""));
        File expectedFile = new File(expectedFileName.replaceFirst("\\.kt$", ".txt"));

        File libraryOut = new File(tmpdir, "libraryOut");
        compileKotlinWithJava(
                FileUtil.findFilesByMask(Pattern.compile(".+\\.java$"), librarySrc),
                FileUtil.findFilesByMask(Pattern.compile(".+\\.kt$"), librarySrc),
                libraryOut,
                getTestRootDisposable(),
                null
        );

        KotlinCoreEnvironment environment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(),
                newConfiguration(ConfigurationKind.JDK_ONLY, getJdkKind(), getClasspath(libraryOut), Collections.emptyList()),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
        registerJavacIfNeeded(environment);
        configureEnvironment(environment);
        KtFile ktFile = KtTestUtil.createFile(kotlinSrc.getPath(), FileUtil.loadFile(kotlinSrc, true), environment.getProject());

        ModuleDescriptor module = GenerationUtils.compileFiles(Collections.singletonList(ktFile), environment).getModule();
        PackageViewDescriptor packageView = module.getPackage(TEST_PACKAGE_FQNAME);
        assertFalse(packageView.isEmpty());

        validateAndCompareDescriptorWithFile(packageView, COMPARATOR_CONFIGURATION.withValidationStrategy(
                new DeserializedScopeValidationVisitor()
        ), expectedFile);
    }

    @NotNull
    protected TestJdkKind getJdkKind() {
        return TestJdkKind.MOCK_JDK;
    }

    protected void doTestSourceJava(@NotNull String javaFileName) {
        try {
            File originalJavaFile = new File(javaFileName);
            File expectedFile = getTxtFile(javaFileName);

            File testPackageDir = new File(tmpdir, "test");
            assertTrue(testPackageDir.mkdir());
            FileUtil.copy(originalJavaFile, new File(testPackageDir, originalJavaFile.getName()));

            Directives directives = KotlinTestUtils.parseDirectives(FileUtil.loadFile(originalJavaFile));
            LanguageVersionSettings languageVersionSettings = parseLanguageVersionSettings(directives);

            Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = loadTestPackageAndBindingContextFromJavaRoot(
                    tmpdir, getTestRootDisposable(), getJdkKind(), ConfigurationKind.JDK_ONLY, false,
                    false, useJavacWrapper(), withForeignAnnotations(), languageVersionSettings);

            checkJavaPackage(
                    expectedFile, javaPackageAndContext.first, javaPackageAndContext.second,
                    COMPARATOR_CONFIGURATION.withValidationStrategy(errorTypesAllowed())
            );
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void doTestCompiledJava(@NotNull String javaFileName, Configuration configuration) {
        File srcDir = new File(tmpdir, "src");
        File compiledDir = new File(tmpdir, "compiled");
        assertTrue(srcDir.mkdir());
        assertTrue(compiledDir.mkdir());
        String fileContent;
        try {
            fileContent = FileUtil.loadFile(new File(javaFileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        List<File> srcFiles = TestFiles.createTestFiles(
                new File(javaFileName).getName(), fileContent,
                new TestFiles.TestFileFactoryNoModules<File>() {
                    @NotNull
                    @Override
                    public File create(@NotNull String fileName, @NotNull String text, @NotNull Directives directives) {
                        File targetFile = new File(srcDir, fileName);
                        try {
                            FileUtil.writeToFile(targetFile, text);
                        }
                        catch (IOException e) {
                            throw new AssertionError(e);
                        }
                        return targetFile;
                    }
                });

        Directives directives = KotlinTestUtils.parseDirectives(fileContent);
        LanguageVersionSettings languageVersionSettings = parseLanguageVersionSettings(directives);

        Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                srcFiles, compiledDir, ConfigurationKind.ALL, languageVersionSettings
        );



        checkJavaPackage(getExpectedFile(
                useTxtSuffixIfFileExists(javaFileName.replaceFirst("\\.java$", ".txt"), "compiled")
        ), javaPackageAndContext.first, javaPackageAndContext.second, configuration);
    }

    public static String useTxtSuffixIfFileExists(String name, String suffix) {
        File differentResultFile = KotlinTestUtils.replaceExtension(new File(name), suffix + ".txt");
        if (differentResultFile.exists()) return differentResultFile.getPath();

        return name;
    }

    @NotNull
    private Pair<PackageViewDescriptor, BindingContext> compileJavaAndLoadTestPackageAndBindingContextFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull ConfigurationKind configurationKind,
            @Nullable LanguageVersionSettings explicitLanguageVersionSettings
    ) {
        try {
            compileJavaWithAnnotationsJar(javaFiles, outDir, getAdditionalJavacArgs(), getJdkHomeForJavac(), withForeignAnnotations());
            return loadTestPackageAndBindingContextFromJavaRoot(outDir, getTestRootDisposable(), getJdkKind(), configurationKind, true,
                                                                usePsiClassFilesReading(), useJavacWrapper(), withForeignAnnotations(), explicitLanguageVersionSettings,
                                                                getExtraClasspath(), this::configureEnvironment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected List<String> getAdditionalJavacArgs() {
        return Collections.emptyList();
    }

    @Nullable
    protected File getJdkHomeForJavac() {
        return null;
    }

    private static void checkJavaPackage(
            File txtFile,
            PackageViewDescriptor javaPackage,
            BindingContext bindingContext,
            Configuration configuration
    ) {
        boolean fail = false;
        try {
            ExpectedLoadErrorsUtil.checkForLoadErrors(javaPackage, bindingContext, JUnit4Assertions.INSTANCE);
        }
        catch (ComparisonFailure e) {
            // to let the next check run even if this one failed
            System.err.println("Expected: " + e.getExpected());
            System.err.println("Actual  : " + e.getActual());
            e.printStackTrace();
            fail = true;
        }
        catch (AssertionError e) {
            e.printStackTrace();
            fail = true;
        }

        validateAndCompareDescriptorWithFile(javaPackage, configuration, txtFile);

        if (fail) {
            fail("See error above");
        }
    }

    private File getTxtFile(String javaFileName) {
        try {
            String fileText = FileUtil.loadFile(new File(javaFileName));
            if (useJavacWrapper() && InTextDirectivesUtils.isDirectiveDefined(fileText, "// JAVAC_EXPECTED_FILE")) {
                return new File(javaFileName.replaceFirst("\\.java$", ".javac.txt"));
            }
            else return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
        }
        catch (IOException e) {
            return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
        }
    }

    private File getTxtFileFromKtFile(String ktFileName) {
        try {
            String fileText = FileUtil.loadFile(new File(ktFileName));
            if (useJavacWrapper() && InTextDirectivesUtils.isDirectiveDefined(fileText, "// JAVAC_EXPECTED_FILE")) {
                return new File(ktFileName.replaceFirst("\\.kt$", ".javac.txt"));
            }
            else return new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        }
        catch (IOException e) {
            return new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        }
    }

}
