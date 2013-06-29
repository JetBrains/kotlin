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
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.jet.test.util.DescriptorValidator.ValidationVisitor.ALLOW_ERROR_TYPES;
import static org.jetbrains.jet.test.util.NamespaceComparator.*;

/*
    The generated test compares namespace descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {

    protected void doTest(@NotNull String javaFileName) throws Exception {
        Assert.assertTrue("A java file expected: " + javaFileName, javaFileName.endsWith(".java"));
        File javaFile = new File(javaFileName);
        File ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        File txtFile = getTxtFile(javaFile.getPath());
        NamespaceDescriptor kotlinNamespace = analyzeKotlinAndLoadTestNamespace(ktFile, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext = compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(
                Arrays.asList(javaFile), tmpdir, myTestRootDisposable, ConfigurationKind.ALL);
        checkLoadedNamespaces(txtFile, kotlinNamespace, javaNamespaceAndContext.first, javaNamespaceAndContext.second);
    }

    protected void doTestCompiledJava(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, DONT_INCLUDE_METHODS_OF_OBJECT);
    }

    protected void doTestCompiledJavaIncludeObjectMethods(@NotNull String javaFileName) throws Exception {
        doTestCompiledJava(javaFileName, RECURSIVE);
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
                    public File create(String fileName, String text) {
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

        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext = compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(
                srcFiles, compiledDir, getTestRootDisposable(), ConfigurationKind.ALL);

        checkJavaNamespace(getTxtFile(javaFileName), javaNamespaceAndContext.first, javaNamespaceAndContext.second, configuration);
    }

    protected void doTestSourceJava(@NotNull String javaFileName) throws Exception {
        File originalJavaFile = new File(javaFileName);
        File expectedFile = getTxtFile(javaFileName);

        File testPackageDir = new File(tmpdir, "test");
        assertTrue(testPackageDir.mkdir());
        FileUtil.copy(originalJavaFile, new File(testPackageDir, originalJavaFile.getName()));

        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext = loadTestNamespaceAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), ConfigurationKind.JDK_ONLY);

        checkJavaNamespace(expectedFile, javaNamespaceAndContext.first, javaNamespaceAndContext.second,
                           DONT_INCLUDE_METHODS_OF_OBJECT.withValidationStrategy(ALLOW_ERROR_TYPES));
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
        JetCoreEnvironment environment = new JetCoreEnvironment(getTestRootDisposable(), configuration);

        ModuleDescriptorImpl moduleDescriptor = AnalyzerFacadeForJVM.createJavaModule("<test module>");

        // we need the same binding trace for resolve from Java and Kotlin
        BindingTrace trace = CliLightClassGenerationSupport.getInstanceForCli(environment.getProject()).getTrace();

        InjectorForJavaDescriptorResolver injectorForJava = new InjectorForJavaDescriptorResolver(environment.getProject(),
                                                                                                  trace,
                                                                                                  moduleDescriptor);

        InjectorForTopDownAnalyzerForJvm injectorForAnalyzer = new InjectorForTopDownAnalyzerForJvm(
                environment.getProject(),
                new TopDownAnalysisParameters(
                        Predicates.<PsiFile>alwaysFalse(), false, false, Collections.<AnalyzerScriptParameter>emptyList()),
                trace,
                moduleDescriptor);
        moduleDescriptor.setModuleConfiguration(injectorForAnalyzer.getJavaBridgeConfiguration());

        injectorForAnalyzer.getTopDownAnalyzer().analyzeFiles(environment.getSourceFiles(), Collections.<AnalyzerScriptParameter>emptyList());

        JavaDescriptorResolver javaDescriptorResolver = injectorForJava.getJavaDescriptorResolver();
        NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(
                LoadDescriptorUtil.TEST_PACKAGE_FQNAME, DescriptorSearchRule.INCLUDE_KOTLIN);
        assert namespaceDescriptor != null : "Test namespace not found";

        checkJavaNamespace(expectedFile, namespaceDescriptor, trace.getBindingContext(), DONT_INCLUDE_METHODS_OF_OBJECT);
    }

    private static void checkForLoadErrorsAndCompare(
            @NotNull NamespaceDescriptor javaNamespace,
            @NotNull BindingContext bindingContext,
            @NotNull Runnable compareNamespacesRunnable
    ) {
        boolean fail = false;
        try {
            ExpectedLoadErrorsUtil.checkForLoadErrors(javaNamespace, bindingContext);
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

    private static void checkLoadedNamespaces(
            final File txtFile,
            final NamespaceDescriptor kotlinNamespace,
            final NamespaceDescriptor javaNamespace,
            BindingContext bindingContext
    ) {
        checkForLoadErrorsAndCompare(javaNamespace, bindingContext, new Runnable() {
            @Override
            public void run() {
                validateAndCompareNamespaces(kotlinNamespace, javaNamespace, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
            }
        });
    }

    private static void checkJavaNamespace(
            final File txtFile,
            final NamespaceDescriptor javaNamespace,
            BindingContext bindingContext,
            final Configuration configuration
    ) {
        checkForLoadErrorsAndCompare(javaNamespace, bindingContext, new Runnable() {
            @Override
            public void run() {
                validateAndCompareNamespaceWithFile(javaNamespace, configuration, txtFile);
            }
        });
    }

    private static File getTxtFile(String javaFileName) {
        return new File(javaFileName.replaceFirst("\\.java$", ".txt"));
    }
}
