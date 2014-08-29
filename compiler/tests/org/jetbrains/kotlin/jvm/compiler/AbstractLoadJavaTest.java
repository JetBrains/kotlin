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
import org.jetbrains.kotlin.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeys;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTrace;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.TopDownAnalysisParameters;
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.LockBasedStorageManager;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestCaseWithTmpdir;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.test.util.DescriptorValidator;
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.kotlin.test.JetTestUtils.*;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesAllowed;
import static org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden;
import static org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.*;

/*
    The generated test compares package descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {
    protected void doTestCompiledJava(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, DONT_INCLUDE_METHODS_OF_OBJECT);
    }

    // Java-Kotlin dependencies are not supported in this method for simplicity
    protected void doTestCompiledJavaAndKotlin(@NotNull String expectedFileName) throws Exception {
        File expectedFile = new File(expectedFileName);
        File sourcesDir = new File(expectedFileName.replaceFirst("\\.txt$", ""));

        List<File> kotlinSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.kt"), sourcesDir);
        compileKotlinToDirAndGetAnalysisResult(kotlinSources, tmpdir, myTestRootDisposable, ConfigurationKind.JDK_ONLY);

        List<File> javaSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.java"), sourcesDir);
        Pair<PackageViewDescriptor, BindingContext> binaryPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                javaSources, tmpdir, ConfigurationKind.JDK_ONLY
        );

        checkJavaPackage(expectedFile, binaryPackageAndContext.first, binaryPackageAndContext.second, DONT_INCLUDE_METHODS_OF_OBJECT);
    }

    protected void doTestCompiledJavaIncludeObjectMethods(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, RECURSIVE);
    }

    protected void doTestCompiledKotlin(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    protected void doTestCompiledKotlinWithStdlib(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.ALL);
    }

    protected void doTestCompiledKotlin(@NotNull String ktFileName, @NotNull ConfigurationKind configurationKind) throws Exception {
        File ktFile = new File(ktFileName);
        File txtFile = new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        AnalysisResult result = compileKotlinToDirAndGetAnalysisResult(Collections.singletonList(ktFile), tmpdir, getTestRootDisposable(),
                                                                        configurationKind);

        PackageViewDescriptor packageFromSource = result.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        assert packageFromSource != null : "Package " + TEST_PACKAGE_FQNAME + " not found";
        Assert.assertEquals("test", packageFromSource.getName().asString());

        PackageViewDescriptor packageFromBinary = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), TestJdkKind.MOCK_JDK, configurationKind
        ).first;

        for (DeclarationDescriptor descriptor : packageFromBinary.getMemberScope().getAllDescriptors()) {
            if (descriptor instanceof ClassDescriptor) {
                assert descriptor instanceof DeserializedClassDescriptor : DescriptorUtils.getFqName(descriptor) + " is loaded as " + descriptor.getClass();
            }
        }

        DescriptorValidator.validate(errorTypesForbidden(), packageFromSource);
        DescriptorValidator.validate(new DeserializedScopeValidationVisitor(), packageFromBinary);
        Configuration configuration = RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                .checkPrimaryConstructors(true)
                .checkPropertyAccessors(true);
        compareDescriptors(packageFromSource, packageFromBinary, configuration, txtFile);
    }

    protected void doTestJavaAgainstKotlin(String expectedFileName) throws Exception {
        File expectedFile = new File(expectedFileName);
        File sourcesDir = new File(expectedFileName.replaceFirst("\\.txt$", ""));

        FileUtil.copyDir(sourcesDir, new File(tmpdir, "test"), new FileFilter() {
            @Override
            public boolean accept(@NotNull File pathname) {
                return pathname.getName().endsWith(".java");
            }
        });

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, tmpdir);
        configuration.put(CommonConfigurationKeys.SOURCE_ROOTS_KEY, Arrays.asList(sourcesDir.getAbsolutePath()));
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, new File("compiler/tests")); // for @ExpectLoadError annotation
        JetCoreEnvironment environment =
                JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        BindingTrace trace = new CliLightClassGenerationSupport.NoScopeRecordCliBindingTrace();
        ModuleDescriptorImpl module = TopDownAnalyzerFacadeForJVM.createSealedJavaModule();

        TopDownAnalysisParameters parameters = TopDownAnalysisParameters.create(
                new LockBasedStorageManager(),
                new ExceptionTracker(), // dummy
                false,
                false
        );

        TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegrationNoIncremental(
                environment.getProject(),
                environment.getSourceFiles(),
                trace,
                parameters,
                module);

        PackageViewDescriptor packageView = module.getPackage(TEST_PACKAGE_FQNAME);
        assert packageView != null : "Test package not found";

        checkJavaPackage(expectedFile, packageView, trace.getBindingContext(), DONT_INCLUDE_METHODS_OF_OBJECT);
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
                getTestRootDisposable()
        );

        JetCoreEnvironment environment = JetCoreEnvironment.createForTests(
                getTestRootDisposable(),
                compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, getAnnotationsJar(), libraryOut),
                EnvironmentConfigFiles.JVM_CONFIG_FILES);

        JetFile jetFile = JetTestUtils.createFile(kotlinSrc.getPath(), FileUtil.loadFile(kotlinSrc, true), environment.getProject());

        AnalysisResult result = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                environment.getProject(), Collections.singleton(jetFile)
        );
        PackageViewDescriptor packageView = result.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        assertNotNull(packageView);

        validateAndCompareDescriptorWithFile(packageView, DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(
                new DeserializedScopeValidationVisitor()
        ), expectedFile);
    }

    protected void doTestSourceJava(@NotNull String javaFileName) throws Exception {
        File originalJavaFile = new File(javaFileName);
        File expectedFile = getTxtFile(javaFileName);

        File testPackageDir = new File(tmpdir, "test");
        assertTrue(testPackageDir.mkdir());
        FileUtil.copy(originalJavaFile, new File(testPackageDir, originalJavaFile.getName()));

        Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), TestJdkKind.MOCK_JDK, ConfigurationKind.JDK_ONLY
        );

        checkJavaPackage(expectedFile, javaPackageAndContext.first, javaPackageAndContext.second,
                         DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(errorTypesAllowed()));
    }

    private void doTestCompiledJava(@NotNull String javaFileName, Configuration configuration) throws Exception {
        final File srcDir = new File(tmpdir, "src");
        File compiledDir = new File(tmpdir, "compiled");
        assertTrue(srcDir.mkdir());
        assertTrue(compiledDir.mkdir());

        List<File> srcFiles = JetTestUtils.createTestFiles(
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

        JetTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                getTestRootDisposable(), ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.MOCK_JDK);

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
        return loadTestPackageAndBindingContextFromJavaRoot(outDir, myTestRootDisposable, TestJdkKind.MOCK_JDK, configurationKind);
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
        return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
    }
}
