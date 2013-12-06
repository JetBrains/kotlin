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
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.scope.JavaFullPackageScope;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
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

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES;
import static org.jetbrains.jet.test.util.RecursiveDescriptorComparator.*;

/*
    The generated test compares namespace descriptors loaded from kotlin sources and read from compiled java.
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
        File ktFile = new File(ktFileName);
        File txtFile = new File(ktFileName.replaceFirst("\\.kt$", ".txt"));
        AnalyzeExhaust exhaust = compileKotlinToDirAndGetAnalyzeExhaust(Collections.singletonList(ktFile), tmpdir, getTestRootDisposable(),
                                                                        ConfigurationKind.JDK_ONLY);

        PackageViewDescriptor packageFromSource = exhaust.getModuleDescriptor().getPackage(TEST_PACKAGE_FQNAME);
        assert packageFromSource != null;
        junit.framework.Assert.assertEquals("test", packageFromSource.getName().asString());

        PackageViewDescriptor packageFromBinary = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY).first;

        checkUsageOfDeserializedScope(DescriptorUtils.getExactlyOnePackageFragment(packageFromBinary.getModule(), TEST_PACKAGE_FQNAME));

        for (DeclarationDescriptor descriptor : packageFromBinary.getMemberScope().getAllDescriptors()) {
            if (descriptor instanceof ClassDescriptor) {
                assert descriptor instanceof DeserializedClassDescriptor : DescriptorUtils.getFqName(descriptor) + " is loaded as " + descriptor.getClass();
            }
        }

        validateAndCompareDescriptors(packageFromSource, packageFromBinary,
                                      RecursiveDescriptorComparator.DONT_INCLUDE_METHODS_OF_OBJECT
                                              .checkPrimaryConstructors(true)
                                              .checkPropertyAccessors(true),
                                      txtFile);
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

        InjectorForTopDownAnalyzerForJvm injectorForAnalyzer = new InjectorForTopDownAnalyzerForJvm(
                environment.getProject(),
                new TopDownAnalysisParameters(
                        Predicates.<PsiFile>alwaysFalse(), false, false, Collections.<AnalyzerScriptParameter>emptyList()),
                trace,
                module);

        module.addFragmentProvider(injectorForAnalyzer.getJavaPackageFragmentProvider());

        injectorForAnalyzer.getTopDownAnalyzer().analyzeFiles(environment.getSourceFiles(), Collections.<AnalyzerScriptParameter>emptyList());

        PackageViewDescriptor packageView = module.getPackage(TEST_PACKAGE_FQNAME);
        assert packageView != null : "Test namespace not found";

        checkJavaPackage(expectedFile, packageView, trace.getBindingContext(), DONT_INCLUDE_METHODS_OF_OBJECT);
    }

    protected void doTestSourceJava(@NotNull String javaFileName) throws Exception {
        File originalJavaFile = new File(javaFileName);
        File expectedFile = getTxtFile(javaFileName);

        File testPackageDir = new File(tmpdir, "test");
        assertTrue(testPackageDir.mkdir());
        FileUtil.copy(originalJavaFile, new File(testPackageDir, originalJavaFile.getName()));

        Pair<PackageViewDescriptor, BindingContext> javaNamespaceAndContext = loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY);

        checkJavaPackage(expectedFile, javaNamespaceAndContext.first, javaNamespaceAndContext.second,
                         DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(ALLOW_ERROR_TYPES));
    }

    private void doTestCompiledJava(@NotNull String javaFileName, Configuration configuration) throws Exception {
        final File srcDir = new File(tmpdir, "src");
        File compiledDir = new File(tmpdir, "compiled");
        assertTrue(srcDir.mkdir());
        assertTrue(compiledDir.mkdir());

        List<File> srcFiles = JetTestUtils.createTestFiles(
                new File(javaFileName).getName(), FileUtil.loadFile(new File(javaFileName)),
                new JetTestUtils.TestFileFactory<File>() {
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
            @NotNull Runnable compareNamespacesRunnable
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

        compareNamespacesRunnable.run();
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

    private static void checkUsageOfDeserializedScope(@NotNull PackageFragmentDescriptor packageFromBinary) {
        JetScope scope = packageFromBinary.getMemberScope();
        boolean hasOwnMembers = false;
        for (DeclarationDescriptor declarationDescriptor : scope.getAllDescriptors()) {
            if (declarationDescriptor instanceof CallableMemberDescriptor) {
                hasOwnMembers = true;
            }
        }
        if (hasOwnMembers) {
            assert scope instanceof JavaFullPackageScope : "If namespace has members, members should be inside deserialized scope.";
        }
        else {
            //NOTE: should probably change
            assert !(scope instanceof JavaFullPackageScope) : "We don't use deserialized scopes for namespaces without members.";
        }
    }
}
