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

package org.jetbrains.jet.generators;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.checkers.AbstractDiagnosticsTestWithEagerResolve;
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest;
import org.jetbrains.jet.codegen.AbstractDataClassCodegenTest;
import org.jetbrains.jet.codegen.AbstractIntrinsicsTestCase;
import org.jetbrains.jet.codegen.AbstractMultiDeclTestCase;
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest;
import org.jetbrains.jet.codegen.labels.AbstractLabelGenTest;
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
                new SimpleTestClassModel(new File("compiler/testData/diagnostics/tests"), true, "kt", "doTest"),
                new SimpleTestClassModel(new File("compiler/testData/diagnostics/tests/script"), true, "ktscript", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "DataClassCodegenTestGenerated",
                AbstractDataClassCodegenTest.class,
                new SimpleTestClassModel(new File("compiler/testData/codegen/dataClasses"), true, "kt", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "IntrinsicsTestGenerated",
                AbstractIntrinsicsTestCase.class,
                new SimpleTestClassModel(new File("compiler/testData/codegen/intrinsics"), true, "kt", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "MultiDeclTestGenerated",
                AbstractMultiDeclTestCase.class,
                new SimpleTestClassModel(new File("compiler/testData/codegen/multiDecl"), true, "kt", "blackBoxFileByFullPath")
        );

        generateTest(
                "compiler/tests/",
                "WriteFlagsTestGenerated",
                AbstractWriteFlagsTest.class,
                new SimpleTestClassModel(new File("compiler/testData/writeFlags"), true, "kt", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "LabelGenTestGenerated",
                AbstractLabelGenTest.class,
                new SimpleTestClassModel(new File("compiler/testData/codegen/label"), true, "kt", "doTest")
        );


        generateTest(
                "compiler/tests/",
                "LoadCompiledKotlinTestGenerated",
                AbstractLoadCompiledKotlinTest.class,
                new SimpleTestClassModel(new File("compiler/testData/loadKotlin"), true, "kt", "doTest")
        );


        generateTest(
                "compiler/tests/",
                "LoadJavaTestGenerated",
                AbstractLoadJavaTest.class,
                new SimpleTestClassModel(new File("compiler/testData/loadJava"), true, "java", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "LazyResolveDescriptorRendererTestGenerated",
                AbstractLazyResolveDescriptorRendererTest.class,
                new SimpleTestClassModel(new File("compiler/testData/renderer"), true, "kt", "doTest"),
                new SimpleTestClassModel(new File("compiler/testData/lazyResolve/descriptorRenderer"), true, "kt", "doTest")
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
                new SimpleTestClassModel(new File("compiler/testData/loadKotlin"), true, "kt", "doTestSinglePackage"),
                new SimpleTestClassModel(new File("compiler/testData/loadJava"), true, "kt", "doTestSinglePackage"),
                new SimpleTestClassModel(new File("compiler/testData/lazyResolve/namespaceComparator"), true, "kt", "doTestSinglePackage")
        );

        generateTest(
                "idea/tests/",
                "JetPsiCheckerTestGenerated",
                AbstractJetPsiCheckerTest.class,
                new SimpleTestClassModel(new File("idea/testData/checker"), false, "kt", "doTest"),
                new SimpleTestClassModel(new File("idea/testData/checker/regression"), true, "kt", "doTest"),
                new SimpleTestClassModel(new File("idea/testData/checker/rendering"), true, "kt", "doTest"),
                new SimpleTestClassModel(new File("idea/testData/checker/infos"), true, "kt", "doTestWithInfos")
        );

        generateTest(
                "idea/tests/",
                "DeprecatedHighlightingTestGenerated",
                AbstractDeprecatedHighlightingTest.class,
                new SimpleTestClassModel(new File("idea/testData/highlighter/deprecated"), true, "kt", "doTest")
        );
    }

    private GenerateTests() {
    }
}