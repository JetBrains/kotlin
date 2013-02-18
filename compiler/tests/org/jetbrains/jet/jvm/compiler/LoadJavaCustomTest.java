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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.KotlinTestWithEnvironmentManagement;
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
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.test.util.NamespaceComparator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.compileJavaAndLoadTestNamespaceAndBindingContextFromBinary;
import static org.jetbrains.jet.jvm.compiler.LoadDescriptorUtil.loadTestNamespaceAndBindingContextFromJavaRoot;
import static org.jetbrains.jet.test.util.NamespaceComparator.compareNamespaceWithFile;

/*
   LoadJavaTestGenerated should be used instead if possible.
 */
public final class LoadJavaCustomTest extends KotlinTestWithEnvironment {
    @NotNull
    private static final String PATH = "compiler/testData/loadJavaCustom";

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.JDK_AND_ANNOTATIONS);
    }

    private void doTest(@NotNull String expectedFileName, @NotNull String... javaFileNames) throws Exception {
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

        AbstractLoadJavaTest.checkJavaNamespace(expectedFile, javaNamespaceAndBindingContext);
    }

    private void doTestNoCompile(@NotNull String expectedFileName, @NotNull String javaRoot) throws Exception {
        File expectedFile = new File(expectedFileName);

        Pair<NamespaceDescriptor, BindingContext> javaNamespaceAndBindingContext
                = loadTestNamespaceAndBindingContextFromJavaRoot(new File(javaRoot), getTestRootDisposable(), ConfigurationKind.JDK_ONLY);

        AbstractLoadJavaTest.checkJavaNamespace(expectedFile, javaNamespaceAndBindingContext);
    }

    public void testPackageLocalVisibility() throws Exception {
        String dir = PATH + "/packageLocalVisibility/simple/";
        String javaDir = dir + "/java";
        doTest(dir + "/expected.txt",
               javaDir + "/test/JFrame.java",
               javaDir + "/awt/Frame.java");
    }

    public void testInner() throws Exception {
        doSimpleTest();
    }

    public void testProtectedStaticVisibility() throws Exception {
        String dir = PATH + "/protectedStaticVisibility/constructor/";
        doTest(dir + "ConstructorInProtectedStaticNestedClass.txt",
               dir + "ConstructorInProtectedStaticNestedClass.java");
    }

    public void testProtectedPackageFun() throws Exception {
        String dir = PATH + "/protectedPackageVisibility/";
        doTest(dir + "ProtectedPackageFun.txt",
               dir + "ProtectedPackageFun.java");
    }

    public void testProtectedPackageConstructor() throws Exception {
        String dir = PATH + "/protectedPackageVisibility/";
        doTest(dir + "ProtectedPackageConstructor.txt",
               dir + "ProtectedPackageConstructor.java");
    }

    public void testProtectedPackageProperty() throws Exception {
        String dir = PATH + "/protectedPackageVisibility/";
        doTest(dir + "ProtectedPackageProperty.txt",
               dir + "ProtectedPackageProperty.java");
    }

    public void testStaticFinal() throws Exception {
        String dir = "/staticFinal/";
        doTest(PATH + dir + "expected.txt",
               PATH + dir + "test.java");
    }

    private void doSimpleTest() throws Exception {
        doTest(PATH + "/" + getTestName(true) + ".txt",
               PATH + "/" + getTestName(true) + ".java");
    }

    public void testKotlinSignatureTwoSuperclassesInconsistentGenericTypes() throws Exception {
        String dir = PATH + "/kotlinSignature/";
        doTest(dir + "TwoSuperclassesInconsistentGenericTypes.txt",
               dir + "TwoSuperclassesInconsistentGenericTypes.java");
    }

    public void testKotlinSignatureTwoSuperclassesVarargAndNot() throws Exception {
        String dir = PATH + "/kotlinSignature/";
        doTest(dir + "TwoSuperclassesVarargAndNot.txt",
               dir + "TwoSuperclassesVarargAndNot.java");
    }

    //TODO: move to LoadJavaTestGenerated when possible
    public void testEnum() throws Exception {
        String dir = PATH + "/enum";
        String javaDir = dir + "/java";
        doTest(dir + "/expected.txt",
               javaDir + "/MyEnum.java");
    }

    public void testRawSuperType() throws Exception {
        String dir = PATH + "/rawSuperType/";
        doTest(dir + "RawSuperType.txt",
               dir + "RawSuperType.java");
    }

    public void testSubclassWithRawType() throws Exception {
        String dir = PATH + "/subclassWithRawType/";
        doTest(dir + "SubclassWithRawType.txt",
               dir + "SubclassWithRawType.java");
    }

    public void testArraysInSubtypes() throws Exception {
        String dir = PATH + "/arraysInSubtypes/";
        doTest(dir + "ArraysInSubtypes.txt",
               dir + "ArraysInSubtypes.java");
    }

    public void testMethodTypeParameterErased() throws Exception {
        String dir = PATH + "/methodTypeParameterErased/";
        doTest(dir + "MethodTypeParameterErased.txt",
               dir + "MethodTypeParameterErased.java");
    }

    public void testReturnNotSubtype() throws Exception {
        String dir = PATH + "/returnNotSubtype/";
        doTestNoCompile(dir + "ReturnNotSubtype.txt", dir);
    }

    public static class SubclassingKotlinInJavaTest extends KotlinTestWithEnvironmentManagement {
        public void testSubclassingKotlinInJava() throws Exception {
            doTest(PATH + "/" + getTestName(true));
        }

        public void testDeepSubclassingKotlinInJava() throws Exception {
            doTest(PATH + "/" + getTestName(true));
        }

        public void testPackageInheritance() throws Exception {
            doTest(PATH + "/packageLocalVisibility/inheritance");
        }

        public void testProtectedPackageInheritance() throws Exception {
            doTest(PATH + "/protectedPackageVisibility/inheritance");
        }

        public void doTest(String path) throws Exception {
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
                    new TopDownAnalysisParameters(Predicates.<PsiFile>alwaysFalse(), false, false, Collections.<AnalyzerScriptParameter>emptyList()),
                    trace,
                    moduleDescriptor);

            injectorForAnalyzer.getTopDownAnalyzer().analyzeFiles(environment.getSourceFiles(), Collections.<AnalyzerScriptParameter>emptyList());

            JavaDescriptorResolver javaDescriptorResolver = injectorForJava.getJavaDescriptorResolver();
            NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(
                    LoadDescriptorUtil.TEST_PACKAGE_FQNAME, DescriptorSearchRule.INCLUDE_KOTLIN);
            assert namespaceDescriptor != null;

            compareNamespaceWithFile(namespaceDescriptor, NamespaceComparator.DONT_INCLUDE_METHODS_OF_OBJECT,
                                     new File(dir, "expected.txt"));

            ExpectedLoadErrorsUtil.checkForLoadErrors(namespaceDescriptor, trace.getBindingContext());
        }
    }
}
