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

package org.jetbrains.kotlin.jvm.compiler;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.kotlin.test.KotlinTestUtils.*;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesAllowed;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.*;

/*
    The generated test compares package descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {
    // There are two modules in each test case (sources and dependencies), so we should render declarations from both of them
    public static final Configuration COMPARATOR_CONFIGURATION = DONT_INCLUDE_METHODS_OF_OBJECT.renderDeclarationsFromOtherModules(true);

    protected void doTestCompiledJava(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, COMPARATOR_CONFIGURATION);
    }

    // Java-Kotlin dependencies are not supported in this method for simplicity
    protected void doTestCompiledJavaAndKotlin(@NotNull String expectedFileName) throws Exception {
        File expectedFile = new File(expectedFileName);
        File sourcesDir = new File(expectedFileName.replaceFirst("\\.txt$", ""));

        if (useJavacWrapper()) return;

        List<File> kotlinSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.kt"), sourcesDir);
        KotlinCoreEnvironment environment =
                KotlinTestUtils.createEnvironmentWithMockJdkAndIdeaAnnotations(myTestRootDisposable, ConfigurationKind.JDK_ONLY);
        registerJavacIfNeeded(environment);

        compileKotlinToDirAndGetModule(kotlinSources, tmpdir, environment);

        List<File> javaSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.java"), sourcesDir);
        Pair<PackageViewDescriptor, BindingContext> binaryPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                javaSources, tmpdir, ConfigurationKind.JDK_ONLY
        );

        checkJavaPackage(expectedFile, binaryPackageAndContext.first, binaryPackageAndContext.second, COMPARATOR_CONFIGURATION);
    }

    @NotNull
    protected File getExpectedFile(@NotNull String expectedFileName) {
        return new File(expectedFileName);
    }

    protected void doTestCompiledJavaIncludeObjectMethods(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, RECURSIVE.renderDeclarationsFromOtherModules(true));
    }

    protected void doTestCompiledKotlin(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_ONLY, false);
    }

    protected void doTestCompiledKotlinWithTypeTable(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_ONLY, true);
    }

    protected void doTestCompiledKotlinWithStdlib(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.ALL, false);
    }

    private void doTestCompiledKotlin(
            @NotNull String ktFileName, @NotNull ConfigurationKind configurationKind, boolean useTypeTableInSerializer
    ) throws Exception {
        File ktFile = new File(ktFileName);
        File txtFile = getTxtFileFromKtFile(ktFileName);

        CompilerConfiguration configuration = newConfiguration(configurationKind, TestJdkKind.MOCK_JDK, getAnnotationsJar());
        if (useTypeTableInSerializer) {
            configuration.put(JVMConfigurationKeys.USE_TYPE_TABLE, true);
        }
        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        registerJavacIfNeeded(environment);
        ModuleDescriptor module = compileKotlinToDirAndGetModule(Collections.singletonList(ktFile), tmpdir, environment);

        PackageViewDescriptor packageFromSource = module.getPackage(TEST_PACKAGE_FQNAME);
        Assert.assertEquals("test", packageFromSource.getName().asString());

        PackageViewDescriptor packageFromBinary = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), getJdkKind(), configurationKind, true, false, useJavacWrapper()
        ).first;

        for (DeclarationDescriptor descriptor : DescriptorUtils.getAllDescriptors(packageFromBinary.getMemberScope())) {
            if (descriptor instanceof ClassDescriptor) {
                assert descriptor instanceof DeserializedClassDescriptor : DescriptorUtils.getFqName(descriptor) + " is loaded as " + descriptor.getClass();
            }
        }

        DescriptorValidator.validate(errorTypesForbidden(), packageFromSource);
        DescriptorValidator.validate(new DeserializedScopeValidationVisitor(), packageFromBinary);
        Configuration comparatorConfiguration = COMPARATOR_CONFIGURATION.checkPrimaryConstructors(true).checkPropertyAccessors(true);
        compareDescriptors(packageFromSource, packageFromBinary, comparatorConfiguration, txtFile);
    }

    protected boolean useFastClassFilesReading() {
        return false;
    }

    protected boolean useJavacWrapper() { return false; }

    protected void registerJavacIfNeeded(KotlinCoreEnvironment environment) {}

    protected void doTestJavaAgainstKotlin(String expectedFileName) throws Exception {
        File expectedFile = new File(expectedFileName);
        File sourcesDir = new File(expectedFileName.replaceFirst("\\.txt$", ""));

        FileUtil.copyDir(sourcesDir, new File(tmpdir, "test"), pathname -> pathname.getName().endsWith(".java"));

        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(ConfigurationKind.JDK_ONLY, getJdkKind());
        ContentRootsKt.addKotlinSourceRoot(configuration, sourcesDir.getAbsolutePath());
        JvmContentRootsKt.addJavaSourceRoot(configuration, new File("compiler/testData/loadJava/include"));
        JvmContentRootsKt.addJavaSourceRoot(configuration, tmpdir);

        KotlinCoreEnvironment environment =
                KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
        registerJavacIfNeeded(environment);
        AnalysisResult result = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                environment.getProject(), environment.getSourceFiles(), new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace(),
                configuration, environment::createPackagePartProvider
        );

        PackageViewDescriptor packageView = result.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        checkJavaPackage(expectedFile, packageView, result.getBindingContext(), COMPARATOR_CONFIGURATION);
    }

    // TODO: add more tests on inherited parameter names, but currently impossible because of KT-4509
    protected void doTestKotlinAgainstCompiledJavaWithKotlin(@NotNull String expectedFileName) throws Exception {
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
                newConfiguration(ConfigurationKind.JDK_ONLY, getJdkKind(), getAnnotationsJar(), libraryOut),
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
        registerJavacIfNeeded(environment);
        KtFile ktFile = KotlinTestUtils.createFile(kotlinSrc.getPath(), FileUtil.loadFile(kotlinSrc, true), environment.getProject());

        ModuleDescriptor module = JvmResolveUtil.analyzeAndCheckForErrors(Collections.singleton(ktFile), environment).getModuleDescriptor();
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

    protected void doTestSourceJava(@NotNull String javaFileName) throws Exception {
        File originalJavaFile = new File(javaFileName);
        File expectedFile = getTxtFile(javaFileName);

        File testPackageDir = new File(tmpdir, "test");
        assertTrue(testPackageDir.mkdir());
        FileUtil.copy(originalJavaFile, new File(testPackageDir, originalJavaFile.getName()));

        Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), getJdkKind(), ConfigurationKind.JDK_ONLY, false,
                false, useJavacWrapper());

        checkJavaPackage(
                expectedFile, javaPackageAndContext.first, javaPackageAndContext.second,
                COMPARATOR_CONFIGURATION.withValidationStrategy(errorTypesAllowed())
        );
    }

    private void doTestCompiledJava(@NotNull String javaFileName, Configuration configuration) throws Exception {
        File srcDir = new File(tmpdir, "src");
        File compiledDir = new File(tmpdir, "compiled");
        assertTrue(srcDir.mkdir());
        assertTrue(compiledDir.mkdir());

        List<File> srcFiles = KotlinTestUtils.createTestFiles(
                new File(javaFileName).getName(), FileUtil.loadFile(new File(javaFileName), true),
                new TestFileFactoryNoModules<File>() {
                    @NotNull
                    @Override
                    public File create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
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

        Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                srcFiles, compiledDir, ConfigurationKind.ALL
        );

        checkJavaPackage(getTxtFile(javaFileName), javaPackageAndContext.first, javaPackageAndContext.second, configuration);
    }

    @NotNull
    private Pair<PackageViewDescriptor, BindingContext> compileJavaAndLoadTestPackageAndBindingContextFromBinary(
            @NotNull Collection<File> javaFiles,
            @NotNull File outDir,
            @NotNull ConfigurationKind configurationKind
    ) throws IOException {
        compileJavaWithAnnotationsJar(javaFiles, outDir);
        return loadTestPackageAndBindingContextFromJavaRoot(outDir, myTestRootDisposable, getJdkKind(), configurationKind, true,
                                                            useFastClassFilesReading(), useJavacWrapper());
    }

    private static void checkJavaPackage(
            File txtFile,
            PackageViewDescriptor javaPackage,
            BindingContext bindingContext,
            Configuration configuration
    ) {
        boolean fail = false;
        try {
            ExpectedLoadErrorsUtil.checkForLoadErrors(javaPackage, bindingContext);
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

    private static File getTxtFile(String javaFileName) {
        try {
            String fileText = FileUtil.loadFile(new File(javaFileName));
            if (InTextDirectivesUtils.isDirectiveDefined(fileText, "// JAVAC_EXPECTED_FILE")) {
                return new File(javaFileName.replaceFirst("\\.java$", ".javac.txt"));
            }
            else return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
        }
        catch (IOException e) {
            return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
        }
    }

    private static File getTxtFileFromKtFile(String ktFileName) {
        try {
            String fileText = FileUtil.loadFile(new File(ktFileName));
            if (InTextDirectivesUtils.isDirectiveDefined(fileText, "// JAVAC_EXPECTED_FILE")) {
                return new File(ktFileName.replaceFirst("\\.kt$", ".javac.txt"));
            }
            else return new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        }
        catch (IOException e) {
            return new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        }
    }

}
