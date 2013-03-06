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
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForTopDownAnalyzerForJvm;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.AnalyzerScriptParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.TopDownAnalysisParameters;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.TestCaseWithTmpdir;
import org.junit.Assert;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.*;
import static org.jetbrains.jet.test.util.NamespaceComparator.*;

/*
    The generated test compares namespace descriptors loaded from kotlin sources and read from compiled java.
*/
public abstract class AbstractLoadJavaTest extends TestCaseWithTmpdir {

    public void doTest(@NotNull String javaFileName) throws Exception {
        Assert.assertTrue("A java file expected: " + javaFileName, javaFileName.endsWith(".java"));
        File javaFile = new File(javaFileName);
        File ktFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".kt"));
        File txtFile = new File(javaFile.getPath().replaceFirst("\\.java$", ".txt"));
        NamespaceDescriptor kotlinNamespace = analyzeKotlinAndLoadTestNamespace(ktFile, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext = compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(
                Arrays.asList(javaFile),
                tmpdir, myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS);
        checkLoadedNamespaces(txtFile, kotlinNamespace, javaNamespaceAndContext);
    }

    protected void doTestCompiledJava(@NotNull String expectedFileName, @NotNull String... javaFileNames) throws Exception {
        JetTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                getTestRootDisposable(), ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.MOCK_JDK);

        List<File> files = ContainerUtil.map(Arrays.asList(javaFileNames), new Function<String, File>() {
            @Override
            public File fun(String s) {
                return new File(s);
            }
        });
        File expectedFile = new File(expectedFileName);
        File tmpDir = JetTestUtils.tmpDir(expectedFile.getName());

        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndBindingContext
                = compileJavaAndLoadTestNamespaceAndBindingContextFromBinary(files, tmpDir, getTestRootDisposable(),
                                                                             ConfigurationKind.JDK_ONLY);

        checkJavaNamespace(expectedFile, javaNamespaceAndBindingContext);
    }

    protected void doTestSourceJava(@NotNull String expectedFileName, @NotNull String javaRoot) throws Exception {
        File expectedFile = new File(expectedFileName);

        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndBindingContext
                = loadTestNamespaceAndBindingContextFromJavaRoot(new File(javaRoot), getTestRootDisposable(), ConfigurationKind.JDK_ONLY);

        checkJavaNamespace(expectedFile, javaNamespaceAndBindingContext);
    }

    protected void doTestJavaAgainstKotlin(String path) throws Exception {
        File dir = new File(path);

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, new File(dir, "java"));
        configuration.put(CommonConfigurationKeys.SOURCE_ROOTS_KEY, Arrays.asList(new File(dir, "kotlin").getAbsolutePath()));
        JetCoreEnvironment environment = new JetCoreEnvironment(getTestRootDisposable(), configuration);

        ModuleDescriptor moduleDescriptor = new ModuleDescriptor(Name.special("<test module>"));

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

        injectorForAnalyzer.getTopDownAnalyzer().analyzeFiles(environment.getSourceFiles(), Collections.<AnalyzerScriptParameter>emptyList());

        JavaDescriptorResolver javaDescriptorResolver = injectorForJava.getJavaDescriptorResolver();
        NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(
                LoadDescriptorUtil.TEST_PACKAGE_FQNAME, DescriptorSearchRule.INCLUDE_KOTLIN);
        assert namespaceDescriptor != null;

        compareNamespaceWithFile(namespaceDescriptor, DONT_INCLUDE_METHODS_OF_OBJECT,
                                 new File(dir, "expected.txt"));

        ExpectedLoadErrorsUtil.checkForLoadErrors(namespaceDescriptor, trace.getBindingContext());
    }

    private static void checkForLoadErrorsAndCompare(
            @NotNull Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext,
            @NotNull Runnable compareNamespacesRunnable
    ) {
        NamespaceDescriptor javaNamespace = javaNamespaceAndContext.first;

        boolean fail = false;
        try {
            ExpectedLoadErrorsUtil.checkForLoadErrors(javaNamespace, javaNamespaceAndContext.second);
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

    public static void checkLoadedNamespaces(
            final File txtFile,
            final NamespaceDescriptor kotlinNamespace,
            final Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext
    ) {
        checkForLoadErrorsAndCompare(javaNamespaceAndContext, new Runnable() {
            @Override
            public void run() {
                compareNamespaces(kotlinNamespace, javaNamespaceAndContext.first, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
            }
        });
    }

    public static void checkJavaNamespace(
            final File txtFile,
            final Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndContext
    ) {
        checkForLoadErrorsAndCompare(javaNamespaceAndContext, new Runnable() {
            @Override
            public void run() {
                compareNamespaceWithFile(javaNamespaceAndContext.first, DONT_INCLUDE_METHODS_OF_OBJECT, txtFile);
            }
        });
    }
}
