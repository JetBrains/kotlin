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
import org.jetbrains.jet.checkers.AbstractJetJsCheckerTest;
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest;
import org.jetbrains.jet.codegen.AbstractBytecodeTextTest;
import org.jetbrains.jet.codegen.AbstractCheckLocalVariablesTableTest;
import org.jetbrains.jet.codegen.AbstractTopLevelMembersInvocationTest;
import org.jetbrains.jet.codegen.defaultConstructor.AbstractDefaultConstructorCodegenTest;
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest;
import org.jetbrains.jet.codegen.generated.AbstractBlackBoxCodegenTest;
import org.jetbrains.jet.completion.AbstractJavaCompletionTest;
import org.jetbrains.jet.completion.AbstractJavaWithLibCompletionTest;
import org.jetbrains.jet.completion.AbstractJetJSCompletionTest;
import org.jetbrains.jet.completion.AbstractKeywordCompletionTest;
import org.jetbrains.jet.jvm.compiler.*;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveNamespaceComparingTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveTest;
import org.jetbrains.jet.modules.xml.AbstractModuleXmlParserTest;
import org.jetbrains.jet.plugin.codeInsight.unwrap.AbstractUnwrapRemoveTest;
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationTest;
import org.jetbrains.jet.plugin.codeInsight.moveUpDown.AbstractCodeMoverTest;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.AbstractSurroundWithTest;
import org.jetbrains.jet.plugin.folding.AbstractKotlinFoldingTest;
import org.jetbrains.jet.plugin.hierarchy.AbstractHierarchyTest;
import org.jetbrains.jet.plugin.highlighter.AbstractDeprecatedHighlightingTest;
import org.jetbrains.jet.plugin.navigation.JetAbstractGotoSuperTest;
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixMultiFileTest;
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixTest;
import org.jetbrains.jet.psi.AbstractJetPsiMatcherTest;
import org.jetbrains.jet.resolve.AbstractResolveTest;
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

        generateTest(
                "compiler/tests/",
                "JetResolveTestGenerated",
                AbstractResolveTest.class,
                testModel("compiler/testData/resolve", true, "resolve", "doTest")
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
                testModelWithDirectories(("compiler/testData/codegen/boxMultiFile"), "doTestMultiFile")
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
                "TopLevelMembersInvocationTestGenerated",
                AbstractTopLevelMembersInvocationTest.class,
                testModelWithDirectories("compiler/testData/codegen/topLevelMemberInvocation", "doTest")
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
                testModel("compiler/testData/loadKotlin", "doTestWithAccessors")
        );

        generateTest(
                "compiler/tests/",
                "LoadJavaTestGenerated",
                AbstractLoadJavaTest.class,
                testModel("compiler/testData/loadJava/compiledJavaCompareWithKotlin", true, "java", "doTest"),
                testModel("compiler/testData/loadJava/compiledJavaIncludeObjectMethods", true, "java", "doTestCompiledJavaIncludeObjectMethods"),
                testModel("compiler/testData/loadJava/compiledJava", true, "java", "doTestCompiledJava"),
                testModel("compiler/testData/loadJava/sourceJava", true, "java", "doTestSourceJava"),
                testModel("compiler/testData/loadJava/javaAgainstKotlin", true, "txt", "doTestJavaAgainstKotlin")
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

        generateTest("compiler/tests",
                     "LazyResolveTestGenerated",
                     AbstractLazyResolveTest.class,
                     testModel("compiler/testData/resolve/imports", false, "resolve", "doTest"));

        generateTest(
                "compiler/tests/",
                "LazyResolveNamespaceComparingTestGenerated",
                AbstractLazyResolveNamespaceComparingTest.class,
                testModel("compiler/testData/loadKotlin", "doTestCheckingPrimaryConstructorsAndAccessors"),
                testModel("compiler/testData/loadJava/compiledJavaCompareWithKotlin", "doTestNotCheckingPrimaryConstructors"),
                testModel("compiler/testData/lazyResolve/namespaceComparator", "doTestCheckingPrimaryConstructors")
        );

        generateTest(
                "compiler/tests/",
                "ModuleXmlParserTestGenerated",
                AbstractModuleXmlParserTest.class,
                testModel("compiler/testData/modules.xml", true, "xml", "doTest")
        );

        generateTest(
                "idea/tests/",
                "JetPsiMatcherTest",
                AbstractJetPsiMatcherTest.class,
                testModel("idea/testData/jetPsiMatcher/expressions", "doTestExpressions"),
                testModel("idea/testData/jetPsiMatcher/types", "doTestTypes")
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
                "JetJsCheckerTestGenerated",
                AbstractJetJsCheckerTest.class,
                testModel("idea/testData/checker/js", false, "kt", "doTest")
        );

        generateTest(
                "idea/tests/",
                "QuickFixTestGenerated",
                AbstractQuickFixTest.class,
                new SimpleTestClassModel(new File("idea/testData/quickfix"), true, Pattern.compile("^before(\\w+)\\.kt$"), "doTest")
        );

        generateTest(
                "idea/tests/",
                "JetBasicJSCompletionTestGenerated",
                AbstractJetJSCompletionTest.class,
                testModel("idea/testData/completion/basic/common"),
                testModel("idea/testData/completion/basic/js")
        );

        generateTest(
                "idea/tests/",
                "JetBasicJavaCompletionTestGenerated",
                AbstractJavaCompletionTest.class,
                testModel("idea/testData/completion/basic/common"),
                testModel("idea/testData/completion/basic/java")
        );

        generateTest(
                "idea/tests/",
                "JetKeywordCompletionTestGenerated",
                AbstractKeywordCompletionTest.class,
                testModel("idea/testData/completion/keywords", false, "doTest")
        );

        generateTest(
                "idea/tests",
                "JetJavaLibCompletionTestGenerated",
                AbstractJavaWithLibCompletionTest.class,
                testModel("idea/testData/completion/basic/custom", false, "doTestWithJar"));

        generateTest(
                "idea/tests",
                "JetGotoSuperTestGenerated",
                JetAbstractGotoSuperTest.class,
                testModel("idea/testData/navigation/gotoSuper", false, "test", "doTest"));

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
                "KotlinFoldingTestGenerated",
                AbstractKotlinFoldingTest.class,
                testModel("idea/testData/folding/noCollapse", "doTest"),
                testModel("idea/testData/folding/checkCollapse", "doSettingsFoldingTest")
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

        generateTest(
                "idea/tests/",
                "CodeTransformationsTestGenerated",
                AbstractCodeTransformationTest.class,
                testModel("idea/testData/intentions/branched/folding/ifToAssignment", "doTestFoldIfToAssignment"),
                testModel("idea/testData/intentions/branched/folding/ifToReturn", "doTestFoldIfToReturn"),
                testModel("idea/testData/intentions/branched/folding/ifToReturnAsymmetrically", "doTestFoldIfToReturnAsymmetrically"),
                testModel("idea/testData/intentions/branched/folding/whenToAssignment", "doTestFoldWhenToAssignment"),
                testModel("idea/testData/intentions/branched/folding/whenToReturn", "doTestFoldWhenToReturn"),
                testModel("idea/testData/intentions/branched/unfolding/assignmentToIf", "doTestUnfoldAssignmentToIf"),
                testModel("idea/testData/intentions/branched/unfolding/assignmentToWhen", "doTestUnfoldAssignmentToWhen"),
                testModel("idea/testData/intentions/branched/unfolding/propertyToIf", "doTestUnfoldPropertyToIf"),
                testModel("idea/testData/intentions/branched/unfolding/propertyToWhen", "doTestUnfoldPropertyToWhen"),
                testModel("idea/testData/intentions/branched/unfolding/returnToIf", "doTestUnfoldReturnToIf"),
                testModel("idea/testData/intentions/branched/unfolding/returnToWhen", "doTestUnfoldReturnToWhen"),
                testModel("idea/testData/intentions/branched/ifWhen/ifToWhen", "doTestIfToWhen"),
                testModel("idea/testData/intentions/branched/ifWhen/whenToIf", "doTestWhenToIf"),
                testModel("idea/testData/intentions/branched/when/flatten", "doTestFlattenWhen"),
                testModel("idea/testData/intentions/branched/when/introduceSubject", "doTestIntroduceWhenSubject"),
                testModel("idea/testData/intentions/branched/when/eliminateSubject", "doTestEliminateWhenSubject"),
                testModel("idea/testData/intentions/declarations/split", "doTestSplitProperty"),
                testModel("idea/testData/intentions/declarations/join", "doTestJoinProperty"),
                testModel("idea/testData/intentions/removeUnnecessaryParentheses", "doTestRemoveUnnecessaryParentheses")
        );

        generateTest(
                "idea/tests/",
                "HierarchyTestGenerated",
                AbstractHierarchyTest.class,
                testModelWithDirectories("idea/testData/hierarchy/class/type", "doTypeClassHierarchyTest"),
                testModelWithDirectories("idea/testData/hierarchy/class/super", "doSuperClassHierarchyTest"),
                testModelWithDirectories("idea/testData/hierarchy/class/sub", "doSubClassHierarchyTest")
        );

        generateTest(
                "idea/tests/",
                "CodeMoverTestGenerated",
                AbstractCodeMoverTest.class,
                testModel("idea/testData/codeInsight/moveUpDown/classBodyDeclarations", "doTestClassBodyDeclaration"),
                testModel("idea/testData/codeInsight/moveUpDown/closingBraces", "doTestExpression"),
                testModel("idea/testData/codeInsight/moveUpDown/expressions", "doTestExpression")
        );

        generateTest(
                "idea/tests/",
                "UnwrapRemoveTestGenerated",
                AbstractUnwrapRemoveTest.class,
                testModel("idea/testData/codeInsight/unwrapAndRemove/removeExpression", "doTestExpressionRemover"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapThen", "doTestThenUnwrapper"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapElse", "doTestElseUnwrapper"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/removeElse", "doTestElseRemover"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapLoop", "doTestLoopUnwrapper"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapTry", "doTestTryUnwrapper"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapCatch", "doTestCatchUnwrapper"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/removeCatch", "doTestCatchRemover"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapFinally", "doTestFinallyUnwrapper"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/removeFinally", "doTestFinallyRemover"),
                testModel("idea/testData/codeInsight/unwrapAndRemove/unwrapLambda", "doTestLambdaUnwrapper")
        );
    }

    private static SimpleTestClassModel testModel(@NotNull String rootPath) {
        return testModel(rootPath, true, "kt", "doTest");
    }

    private static SimpleTestClassModel testModel(@NotNull String rootPath, @NotNull String methodName) {
        return testModel(rootPath, true, "kt", methodName);
    }

    private static SimpleTestClassModel testModel(@NotNull String rootPath, boolean recursive, @NotNull String methodName) {
        return testModel(rootPath, recursive, "kt", methodName);
    }

    private static SimpleTestClassModel testModelWithDirectories(@NotNull String rootPath, @NotNull String methodName) {
        return new SimpleTestClassModel(new File(rootPath), false,  Pattern.compile("^(.+)$"), methodName);
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