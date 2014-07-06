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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.base.Predicates;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.analyzer.AnalyzeExhaust;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedClassDescriptor;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.DependencyKind;
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil;
import org.jetbrains.jet.storage.ExceptionTracker;
import org.jetbrains.jet.storage.LockBasedStorageManager;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.jetbrains.jet.test.util.RecursiveDescriptorComparator;
import org.junit.Assert;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.jetbrains.jet.JetTestUtils.*;
import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES;
import static org.jetbrains.jet.test.util.RecursiveDescriptorComparator.*;

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
        compileKotlinToDirAndGetAnalyzeExhaust(kotlinSources, tmpdir, myTestRootDisposable, ConfigurationKind.JDK_ONLY);

        List<File> javaSources = FileUtil.findFilesByMask(Pattern.compile(".+\\.java"), sourcesDir);
        Pair<PackageViewDescriptor, BindingContext> binaryPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                javaSources, tmpdir, myTestRootDisposable, ConfigurationKind.JDK_ONLY);

        checkJavaPackage(expectedFile, binaryPackageAndContext.first, binaryPackageAndContext.second, DONT_INCLUDE_METHODS_OF_OBJECT);
    }

    protected void doTestCompiledJavaCompareWithKotlin(@NotNull String javaFileName) throws Exception {
        Assert.assertTrue("A java file expected: " + javaFileName, javaFileName.endsWith(".java"));
        File javaFile = new File(javaFileName);
        File ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        File txtFile = getTxtFile(javaFile.getPath());
        PackageViewDescriptor kotlinPackage = analyzeKotlinAndLoadTestPackage(ktFile, myTestRootDisposable, ConfigurationKind.ALL);
        Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = compileJavaAndLoadTestPackageAndBindingContextFromBinary(
                Arrays.asList(javaFile), tmpdir, myTestRootDisposable, ConfigurationKind.ALL);
        checkLoadedPackages(txtFile, kotlinPackage, javaPackageAndContext.first, javaPackageAndContext.second);
    }

    protected void doTestCompiledJavaIncludeObjectMethods(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, RECURSIVE);
    }

    protected void doTestCompiledKotlin(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.JDK_ONLY);
    }

    protected void doTestCompiledKotlinWithStdlib(@NotNull String ktFileName) throws Exception {
        doTestCompiledKotlin(ktFileName, ConfigurationKind.ALL);
    }

    protected void doTestCompiledKotlin(@NotNull String ktFileName, @NotNull ConfigurationKind configurationKind) throws Exception {
        File ktFile = new File(ktFileName);
        File txtFile = new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        AnalyzeExhaust exhaust = compileKotlinToDirAndGetAnalyzeExhaust(Collections.singletonList(ktFile), tmpdir, getTestRootDisposable(),
                                                                        configurationKind);

        PackageViewDescriptor packageFromSource = exhaust.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        assert packageFromSource != null;
        junit.framework.Assert.assertEquals("test", packageFromSource.getName().asString());

        PackageViewDescriptor packageFromBinary = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), configurationKind).first;

        for (DeclarationDescriptor descriptor : packageFromBinary.getMemberScope().getAllDescriptors()) {
            if (descriptor instanceof ClassDescriptor) {
                assert descriptor instanceof DeserializedClassDescriptor : DescriptorUtils.getFqName(descriptor) + " is loaded as " + descriptor.getClass();
            }
        }

        validateAndCompareDescriptors(packageFromSource, packageFromBinary,
                                      RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                                              .checkPrimaryConstructors(true)
                                              .checkPropertyAccessors(true),
                                      txtFile
        );
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
        JetCoreEnvironment environment = JetCoreEnvironment.createForTests(getTestRootDisposable(), configuration);

        // we need the same binding trace for resolve from Java and Kotlin
        CliLightClassGenerationSupport support = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject());
        BindingTrace trace = support.getTrace();
        ModuleDescriptorImpl module = support.getModule();

        TopDownAnalysisParameters parameters = TopDownAnalysisParameters.create(
                new LockBasedStorageManager(),
                new ExceptionTracker(), // dummy
                Predicates.<PsiFile>alwaysFalse(),
                false,
                false
        );
        InjectorForTopDownAnalyzerForJvm injectorForAnalyzer = new InjectorForTopDownAnalyzerForJvm(
                environment.getProject(),
                parameters,
                trace,
                module);

        module.addFragmentProvider(DependencyKind.BINARIES, injectorForAnalyzer.getJavaDescriptorResolver().getPackageFragmentProvider());

        injectorForAnalyzer.getTopDownAnalyzer().analyzeFiles(parameters, environment.getSourceFiles());

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
                Arrays.asList(librarySrc.listFiles(JetTestCaseBuilder.filterByExtension("java"))),
                Arrays.asList(librarySrc.listFiles(JetTestCaseBuilder.filterByExtension("kt"))),
                libraryOut,
                getTestRootDisposable()
        );

        JetCoreEnvironment environment = JetCoreEnvironment.createForTests(
                getTestRootDisposable(), compilerConfigurationForTests(ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK,
                                                                       getAnnotationsJar(), libraryOut)
        );
        JetFile jetFile = JetTestUtils.createFile(kotlinSrc.getPath(), FileUtil.loadFile(kotlinSrc, true), environment.getProject());

        AnalyzeExhaust exhaust = JvmResolveUtil.analyzeFilesWithJavaIntegrationAndCheckForErrors(
                environment.getProject(), Collections.singleton(jetFile), Predicates.<PsiFile>alwaysTrue()
        );
        PackageViewDescriptor packageView = exhaust.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        assertNotNull(packageView);

        validateAndCompareDescriptorWithFile(packageView, DONT_INCLUDE_METHODS_OF_OBJECT, expectedFile);
    }

    protected void doTestSourceJava(@NotNull String javaFileName) throws Exception {
        File originalJavaFile = new File(javaFileName);
        File expectedFile = getTxtFile(javaFileName);

        File testPackageDir = new File(tmpdir, "test");
        assertTrue(testPackageDir.mkdir());
        FileUtil.copy(originalJavaFile, new File(testPackageDir, originalJavaFile.getName()));

        Pair<PackageViewDescriptor, BindingContext> javaPackageAndContext = loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY);

        checkJavaPackage(expectedFile, javaPackageAndContext.first, javaPackageAndContext.second,
                         DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(ALLOW_ERROR_TYPES));
    }

    private void doTestCompiledJava(@NotNull String javaFileName, Configuration configuration) throws Exception {
        final File srcDir = new File(tmpdir, "src");
        File compiledDir = new File(tmpdir, "compiled");
        assertTrue(srcDir.mkdir());
        assertTrue(compiledDir.mkdir());

        List<File> srcFiles = JetTestUtils.createTestFiles(
                new File(javaFileName).getName(), FileUtil.loadFile(new File(javaFileName), true),
                new TestFileFactoryNoModules<File>() {
                    @Override
                    public File create(String fileName, String text, Map<String, String> directives) {
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
                srcFiles, compiledDir, getTestRootDisposable(), ConfigurationKind.ALL);

        checkJavaPackage(getTxtFile(javaFileName), javaPackageAndContext.first, javaPackageAndContext.second, configuration);
    }

    private static void checkForLoadErrorsAndCompare(
            @NotNull PackageViewDescriptor javaPackage,
            @NotNull BindingContext bindingContext,
            @NotNull Runnable comparePackagesRunnable
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

        comparePackagesRunnable.run();
        if (fail) {
            fail("See error above");
        }
    }

    private static void checkLoadedPackages(
            final File txtFile,
            final PackageViewDescriptor kotlinPackage,
            final PackageViewDescriptor javaPackage,
            BindingContext bindingContext
    ) {
        checkForLoadErrorsAndCompare(javaPackage, bindingContext, new Runnable() {
            @Override
            public void run() {
                validateAndCompareDescriptors(kotlinPackage, javaPackage, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
            }
        });
    }

    private static void checkJavaPackage(
            final File txtFile,
            final PackageViewDescriptor javaPackage,
            BindingContext bindingContext,
            final Configuration configuration
    ) {
        checkForLoadErrorsAndCompare(javaPackage, bindingContext, new Runnable() {
            @Override
            public void run() {
                validateAndCompareDescriptorWithFile(javaPackage, configuration, txtFile);
            }
        });
    }

    private static File getTxtFile(String javaFileName) {
        return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
    }
}
