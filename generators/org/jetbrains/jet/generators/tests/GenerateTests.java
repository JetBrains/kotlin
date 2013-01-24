/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.generators.tests;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.checkers.AbstractDiagnosticsTestWithEagerResolve;
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest;
import org.jetbrains.jet.codegen.AbstractCheckLocalVariablesTableTest;
import org.jetbrains.jet.codegen.AbstractDataClassCodegenTest;
import org.jetbrains.jet.codegen.defaultConstructor.AbstractDefaultConstructorCodegenTest;
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest;
import org.jetbrains.jet.codegen.generated.AbstractCodegenTest;
import org.jetbrains.jet.jvm.compiler.AbstractCompileJavaAgainstKotlinTest;
import org.jetbrains.jet.jvm.compiler.AbstractCompileKotlinAgainstKotlinTest;
import org.jetbrains.jet.jvm.compiler.AbstractCompileKotlinAgainstCustomJavaTest;
import org.jetbrains.jet.jvm.compiler.AbstractLoadCompiledKotlinTest;
import org.jetbrains.jet.jvm.compiler.AbstractLoadJavaTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveNamespaceComparingTest;
import org.jetbrains.jet.plugin.highlighter.AbstractDeprecatedHighlightingTest;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GenerateTests {
    private static void generateTest(
            @NotNull String baseDir,
            @NotNull String suiteClass,
            @NotNull Class<? extends TestCase> baseTestClass,
            @NotNull TestClassModel... testClassModels
    ) throws IOException {
        new TestGenerator(
                baseDir,
                baseTestClass.getPackage().getName(),
                suiteClass,
                baseTestClass,
                Arrays.asList(testClassModels),
                GenerateTests.class
        ).generateAndSave();
    }

    public static void main(String[] args) throws IOException {
        generateTest(
                "compiler/tests/",
                "JetDiagnosticsTestGenerated",
                AbstractDiagnosticsTestWithEagerResolve.class,
                testModel("compiler/testData/diagnostics/tests"),
                testModel("compiler/testData/diagnostics/tests/script", true, "ktscript", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "DataClassCodegenTestGenerated",
                AbstractDataClassCodegenTest.class,
                testModel("compiler/testData/codegen/dataClasses", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "CheckLocalVariablesTableTestGenerated",
                AbstractCheckLocalVariablesTableTest.class,
                testModel("compiler/testData/checkLocalVariablesTable", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "IntrinsicsTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/intrinsics", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "MultiDeclTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/multiDecl", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "VisibilityGenWithJavaTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/visibility/withJava/protected_static", "blackBoxFileWithJavaByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "WriteFlagsTestGenerated",
                AbstractWriteFlagsTest.class,
                testModel("compiler/testData/writeFlags")
        );

        generateTest(
                "compiler/tests/",
                "DefaultArgumentsReflectionTestGenerated",
                AbstractDefaultConstructorCodegenTest.class,
                testModel("compiler/testData/codegen/defaultArguments/reflection")
        );

        generateTest(
                "compiler/tests/",
                "DefaultArgumentsBlackBoxTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/defaultArguments/blackBox")
        );

        generateTest(
                "compiler/tests/",
                "LabelGenTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/label")
        );

        generateTest(
                "compiler/tests/",
                "InnerNestedGenTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/innerNested")
        );

        generateTest(
                "compiler/tests/",
                "InstructionsTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/instructions")
        );

        generateTest(
                "compiler/tests/",
                "OperatorConventionsTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/operatorConventions")
        );

        generateTest(
                "compiler/tests/",
                "ExtensionPropertiesTestGenerated",
                AbstractCodegenTest.class,
                testModel("compiler/testData/codegen/extensionProperties")
        );

        generateTest(
                "compiler/tests/",
                "LoadCompiledKotlinTestGenerated",
                AbstractLoadCompiledKotlinTest.class,
                testModel("compiler/testData/loadKotlin")
        );


        generateTest(
                "compiler/tests/",
                "LoadJavaTestGenerated",
                AbstractLoadJavaTest.class,
                testModel("compiler/testData/loadJava", true, "java", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "CompileJavaAgainstKotlinTestGenerated",
                AbstractCompileJavaAgainstKotlinTest.class,
                testModel("compiler/testData/compileJavaAgainstKotlin", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "CompileKotlinAgainstKotlinTestGenerated",
                AbstractCompileKotlinAgainstKotlinTest.class,
                testModel("compiler/testData/compileKotlinAgainstKotlin", true, "A.kt", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "CompileKotlinAgainstCustomJavaGenerated",
                AbstractCompileKotlinAgainstCustomJavaTest.class,
                testModel("compiler/testData/compileKotlinAgainstCustomJava")
        );

        generateTest(
                "compiler/tests/",
                "LazyResolveDescriptorRendererTestGenerated",
                AbstractLazyResolveDescriptorRendererTest.class,
                testModel("compiler/testData/renderer")
        );

        // TODO test is temporarily disabled
        //generateTest(
        //        "compiler/tests/",
        //        "org.jetbrains.jet.lang.resolve.lazy",
        //        "LazyResolveDiagnosticsTestGenerated",
        //        AbstractLazyResolveDiagnosticsTest.class,
        //        new SimpleTestClassModel(AbstractLazyResolveDiagnosticsTest.TEST_DATA_DIR, true, "kt", "doTest")
        //);

        generateTest(
                "compiler/tests/",
                "LazyResolveNamespaceComparingTestGenerated",
                AbstractLazyResolveNamespaceComparingTest.class,
                testModel("compiler/testData/loadKotlin", "doTestCheckingPrimaryConstructors"),
                testModel("compiler/testData/loadJava", "doTestNotCheckingPrimaryConstructors"),
                testModel("compiler/testData/lazyResolve/namespaceComparator", "doTestCheckingPrimaryConstructors")
        );

        generateTest(
                "idea/tests/",
                "JetPsiCheckerTestGenerated",
                AbstractJetPsiCheckerTest.class,
                testModel("idea/testData/checker", false, "kt", "doTest"),
                testModel("idea/testData/checker/regression"),
                testModel("idea/testData/checker/rendering"),
                testModel("idea/testData/checker/infos", false, "kt", "doTestWithInfos")
        );

        generateTest(
                "idea/tests/",
                "DeprecatedHighlightingTestGenerated",
                AbstractDeprecatedHighlightingTest.class,
                testModel("idea/testData/highlighter/deprecated")
        );
    }

    private static SimpleTestClassModel testModel(@NotNull String rootPath) {
        return testModel(rootPath, true, "kt", "doTest");
    }

    private static SimpleTestClassModel testModel(@NotNull String rootPath, @NotNull String methodName) {
        return testModel(rootPath, true, "kt", methodName);
    }

    private static SimpleTestClassModel testModel(
            @NotNull String rootPath,
            boolean recursive,
            @NotNull String extension,
            @NotNull String doTestMethodName
    ) {
        return new SimpleTestClassModel(new File(rootPath), recursive, extension, doTestMethodName);
    }

    private GenerateTests() {
    }
}