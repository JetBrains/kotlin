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

package org.jetbrains.jet.generators.tests;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.checkers.AbstractDiagnosticsTestWithEagerResolve;
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest;
import org.jetbrains.jet.codegen.AbstractBytecodeTextTest;
import org.jetbrains.jet.codegen.AbstractCheckLocalVariablesTableTest;
import org.jetbrains.jet.codegen.defaultConstructor.AbstractDefaultConstructorCodegenTest;
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest;
import org.jetbrains.jet.codegen.generated.AbstractBlackBoxCodegenTest;
import org.jetbrains.jet.jvm.compiler.*;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveNamespaceComparingTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveTest;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.AbstractSurroundWithTest;
import org.jetbrains.jet.plugin.highlighter.AbstractDeprecatedHighlightingTest;
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixMultiFileTest;
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixTest;
import org.jetbrains.jet.test.generator.SimpleTestClassModel;
import org.jetbrains.jet.test.generator.TestClassModel;
import org.jetbrains.jet.test.generator.TestGenerator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.regex.Pattern;

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

        GenerateRangesCodegenTestData.main(args);

        generateTest(
                "compiler/tests/",
                "BlackBoxCodegenTestGenerated",
                AbstractBlackBoxCodegenTest.class,
                testModel("compiler/testData/codegen/box", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "BlackBoxMultiFileCodegenTestGenerated",
                AbstractBlackBoxCodegenTest.class,
                new SimpleTestClassModel(new File("compiler/testData/codegen/boxMultiFile"), false, Pattern.compile("^(.+)$"), "doTestMultiFile")
        );

        generateTest(
                "compiler/tests/",
                "BlackBoxWithJavaCodegenTestGenerated",
                AbstractBlackBoxCodegenTest.class,
                testModel("compiler/testData/codegen/boxWithJava", "doTestWithJava")
        );

        generateTest(
                "compiler/tests/",
                "BlackBoxWithStdlibCodegenTestGenerated",
                AbstractBlackBoxCodegenTest.class,
                testModel("compiler/testData/codegen/boxWithStdlib", "doTestWithStdlib")
        );

        generateTest(
                "compiler/tests/",
                "BytecodeTextTestGenerated",
                AbstractBytecodeTextTest.class,
                testModel("compiler/testData/codegen/bytecodeText")
        );

        generateTest(
                "compiler/tests/",
                "CheckLocalVariablesTableTestGenerated",
                AbstractCheckLocalVariablesTableTest.class,
                testModel("compiler/testData/checkLocalVariablesTable", "doTest")
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

        generateTest("compiler/tests",
                     "LazyResolveTestGenerated",
                     AbstractLazyResolveTest.class,
                     testModel("compiler/testData/resolve/imports", false, "resolve", "doTest"));

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
                "QuickFixTestGenerated",
                AbstractQuickFixTest.class,
                new SimpleTestClassModel(new File("idea/testData/quickfix"), true, Pattern.compile("^before(\\w+)\\.kt$"), "doTest")
        );

        generateTest(
                "idea/tests/",
                "QuickFixMultiFileTestGenerated",
                AbstractQuickFixMultiFileTest.class,
                new SimpleTestClassModel(new File("idea/testData/quickfix"), true, Pattern.compile("^(\\w+)\\.before\\.Main\\.kt$"), "doTestWithExtraFile")
        );

        generateTest(
                "idea/tests/",
                "DeprecatedHighlightingTestGenerated",
                AbstractDeprecatedHighlightingTest.class,
                testModel("idea/testData/highlighter/deprecated")
        );

        generateTest(
                "idea/tests/",
                "SurroundWithTestGenerated",
                AbstractSurroundWithTest.class,
                testModel("idea/testData/codeInsight/surroundWith/if", "doTestWithIfSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/ifElse", "doTestWithIfElseSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/not", "doTestWithNotSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/parentheses", "doTestWithParenthesesSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/stringTemplate", "doTestWithStringTemplateSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/when", "doTestWithWhenSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/tryCatch", "doTestWithTryCatchSurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/tryCatchFinally", "doTestWithTryCatchFinallySurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/tryFinally", "doTestWithTryFinallySurrounder"),
                testModel("idea/testData/codeInsight/surroundWith/functionLiteral", "doTestWithFunctionLiteralSurrounder")
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
        return new SimpleTestClassModel(new File(rootPath), recursive, Pattern.compile("^(.+)\\." + extension + "$"), doTestMethodName);
    }

    private GenerateTests() {
    }
}