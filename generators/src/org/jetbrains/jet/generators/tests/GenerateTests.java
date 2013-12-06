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
import org.jetbrains.jet.cfg.AbstractControlFlowTest;
import org.jetbrains.jet.checkers.AbstractDiagnosticsTestWithEagerResolve;
import org.jetbrains.jet.checkers.AbstractJetJsCheckerTest;
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest;
import org.jetbrains.jet.cli.AbstractKotlincExecutableTest;
import org.jetbrains.jet.codegen.AbstractBytecodeTextTest;
import org.jetbrains.jet.codegen.AbstractCheckLocalVariablesTableTest;
import org.jetbrains.jet.codegen.AbstractTopLevelMembersInvocationTest;
import org.jetbrains.jet.codegen.defaultConstructor.AbstractDefaultConstructorCodegenTest;
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest;
import org.jetbrains.jet.codegen.generated.AbstractBlackBoxCodegenTest;
import org.jetbrains.jet.completion.*;
import org.jetbrains.jet.completion.weighers.AbstractCompletionWeigherTest;
import org.jetbrains.jet.descriptors.serialization.AbstractDescriptorSerializationTest;
import org.jetbrains.jet.editor.quickDoc.AbstractJetQuickDocProviderTest;
import org.jetbrains.jet.evaluate.AbstractEvaluateExpressionTest;
import org.jetbrains.jet.findUsages.AbstractJetFindUsagesTest;
import org.jetbrains.jet.formatter.AbstractJetFormatterTest;
import org.jetbrains.jet.generators.tests.generator.SimpleTestClassModel;
import org.jetbrains.jet.generators.tests.generator.SingleClassTestModel;
import org.jetbrains.jet.generators.tests.generator.TestClassModel;
import org.jetbrains.jet.generators.tests.generator.TestGenerator;
import org.jetbrains.jet.jvm.compiler.*;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveNamespaceComparingTest;
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveTest;
import org.jetbrains.jet.modules.xml.AbstractModuleXmlParserTest;
import org.jetbrains.jet.parsing.AbstractJetParsingTest;
import org.jetbrains.jet.plugin.codeInsight.AbstractOutOfBlockModificationTest;
import org.jetbrains.jet.plugin.codeInsight.moveUpDown.AbstractCodeMoverTest;
import org.jetbrains.jet.plugin.codeInsight.surroundWith.AbstractSurroundWithTest;
import org.jetbrains.jet.plugin.codeInsight.unwrap.AbstractUnwrapRemoveTest;
import org.jetbrains.jet.plugin.configuration.AbstractConfigureProjectByChangingFileTest;
import org.jetbrains.jet.plugin.folding.AbstractKotlinFoldingTest;
import org.jetbrains.jet.plugin.hierarchy.AbstractHierarchyTest;
import org.jetbrains.jet.plugin.highlighter.AbstractDiagnosticMessageTest;
import org.jetbrains.jet.plugin.highlighter.AbstractHighlightingTest;
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationTest;
import org.jetbrains.jet.plugin.navigation.JetAbstractGotoSuperTest;
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixMultiFileTest;
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixTest;
import org.jetbrains.jet.plugin.refactoring.inline.AbstractInlineTest;
import org.jetbrains.jet.plugin.refactoring.rename.AbstractRenameTest;
import org.jetbrains.jet.psi.AbstractJetPsiMatcherTest;
import org.jetbrains.jet.resolve.AbstractResolveBaseTest;
import org.jetbrains.jet.resolve.AbstractResolveTest;
import org.jetbrains.jet.resolve.AbstractResolveWithLibTest;
import org.jetbrains.jet.resolve.annotation.AbstractAnnotationParameterTest;
import org.jetbrains.jet.resolve.calls.AbstractResolvedCallsTest;
import org.jetbrains.jet.safeDelete.AbstractJetSafeDeleteTest;

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
        System.setProperty("java.awt.headless", "true");
        generateTest(
                "compiler/tests/",
                "JetDiagnosticsTestGenerated",
                AbstractDiagnosticsTestWithEagerResolve.class,
                testModel("compiler/testData/diagnostics/tests"),
                testModel("compiler/testData/diagnostics/tests/script", true, "ktscript", "doTest"),
                testModel("compiler/testData/codegen/box/functions/tailRecursion")
        );

        generateTest(
                "compiler/tests/",
                "JetResolveTestGenerated",
                AbstractResolveTest.class,
                testModel("compiler/testData/resolve", true, "resolve", "doTest")
        );

        generateTest(
                "compiler/tests",
                "JetResolvedCallsTestGenerated",
                AbstractResolvedCallsTest.class,
                testModel("compiler/testData/resolvedCalls")
        );

        generateTest(
                "compiler/tests/",
                "JetParsingTestGenerated",
                AbstractJetParsingTest.class,
                testModel("compiler/testData/psi", true, "kt", "doParsingTest")
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
                "LoadJavaTestGenerated",
                AbstractLoadJavaTest.class,
                testModel("compiler/testData/loadJava/compiledJava", true, "java", "doTestCompiledJava"),
                testModel("compiler/testData/loadJava/compiledJavaAndKotlin", true, "txt", "doTestCompiledJavaAndKotlin"),
                testModel("compiler/testData/loadJava/compiledJavaCompareWithKotlin", true, "java", "doTestCompiledJavaCompareWithKotlin"),
                testModel("compiler/testData/loadJava/compiledJavaIncludeObjectMethods", true, "java",
                          "doTestCompiledJavaIncludeObjectMethods"),
                testModel("compiler/testData/loadJava/compiledKotlin", true, "kt", "doTestCompiledKotlin"),
                testModel("compiler/testData/loadJava/javaAgainstKotlin", true, "txt", "doTestJavaAgainstKotlin"),
                testModel("compiler/testData/loadJava/sourceJava", true, "java", "doTestSourceJava")
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
                testModel("compiler/testData/loadJava/compiledKotlin", "doTestCheckingPrimaryConstructorsAndAccessors"),
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
                "compiler/tests/",
                "DescriptorSerializationTestGenerated",
                AbstractDescriptorSerializationTest.class,
                testModel("compiler/testData/loadJava/compiledKotlin/class"),
                testModel("compiler/testData/loadJava/compiledKotlin/classFun"),
                testModel("compiler/testData/loadJava/compiledKotlin/classObject"),
                testModel("compiler/testData/loadJava/compiledKotlin/constructor"),
                testModel("compiler/testData/loadJava/compiledKotlin/fun"),
                testModel("compiler/testData/loadJava/compiledKotlin/prop"),
                testModel("compiler/testData/loadJava/compiledKotlin/type"),
                testModel("compiler/testData/loadJava/compiledKotlin/visibility")
        );

        generateTest(
                "compiler/tests/",
                "WriteSignatureTestGenerated",
                AbstractWriteSignatureTest.class,
                testModel("compiler/testData/writeSignature", true, "kt", "doTest")
        );

        generateTest(
                "compiler/tests/",
                "KotlincExecutableTestGenerated",
                AbstractKotlincExecutableTest.class,
                testModel("compiler/testData/cli/jvm", true, "args", "doJvmTest"),
                testModel("compiler/testData/cli/js", true, "args", "doJsTest")
        );

        generateTest(
                "compiler/tests/",
                "ControlFlowTestGenerated",
                AbstractControlFlowTest.class,
                testModel("compiler/testData/cfg")
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
                AbstractJSBasicCompletionTest.class,
                testModel("idea/testData/completion/basic/common"),
                testModel("idea/testData/completion/basic/js")
        );

        generateTest(
                "idea/tests/",
                "JetBasicJavaCompletionTestGenerated",
                AbstractJvmBasicCompletionTest.class,
                testModel("idea/testData/completion/basic/common"),
                testModel("idea/testData/completion/basic/java")
        );

        generateTest(
                "idea/tests/",
                "JetSmartCompletionTestGenerated",
                AbstractJvmSmartCompletionTest.class,
                testModel("idea/testData/completion/smart")
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
                AbstractJvmWithLibBasicCompletionTest.class,
                testModel("idea/testData/completion/basic/custom", false, "doTest"));

        generateTest(
                "idea/tests",
                "JetGotoSuperTestGenerated",
                JetAbstractGotoSuperTest.class,
                testModel("idea/testData/navigation/gotoSuper", false, "test", "doTest"));

        generateTest(
                "idea/tests/",
                "QuickFixMultiFileTestGenerated",
                AbstractQuickFixMultiFileTest.class,
                new SimpleTestClassModel(new File("idea/testData/quickfix"), true, Pattern.compile("^(\\w+)\\.before\\.Main\\.kt$"),
                                         "doTestWithExtraFile")
        );

        generateTest(
                "idea/tests/",
                "HighlightingTestGenerated",
                AbstractHighlightingTest.class,
                testModel("idea/testData/highlighter")
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
                testModel("idea/testData/intentions/branched/when/merge", "doTestMergeWhen"),
                testModel("idea/testData/intentions/branched/when/introduceSubject", "doTestIntroduceWhenSubject"),
                testModel("idea/testData/intentions/branched/when/eliminateSubject", "doTestEliminateWhenSubject"),
                testModel("idea/testData/intentions/declarations/split", "doTestSplitProperty"),
                testModel("idea/testData/intentions/declarations/join", "doTestJoinProperty"),
                testModel("idea/testData/intentions/declarations/convertMemberToExtension", "doTestConvertMemberToExtension"),
                testModel("idea/testData/intentions/reconstructedType", "doTestReconstructType"),
                testModel("idea/testData/intentions/removeUnnecessaryParentheses", "doTestRemoveUnnecessaryParentheses")
        );

        generateTest(
                "idea/tests/",
                "HierarchyTestGenerated",
                AbstractHierarchyTest.class,
                testModelWithDirectories("idea/testData/hierarchy/class/type", "doTypeClassHierarchyTest"),
                testModelWithDirectories("idea/testData/hierarchy/class/super", "doSuperClassHierarchyTest"),
                testModelWithDirectories("idea/testData/hierarchy/class/sub", "doSubClassHierarchyTest"),
                testModelWithDirectories("idea/testData/hierarchy/calls/callers", "doCallerHierarchyTest"),
                testModelWithDirectories("idea/testData/hierarchy/calls/callees", "doCalleeHierarchyTest")
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
                "InlineTestGenerated",
                AbstractInlineTest.class,
                testModel("idea/testData/refactoring/inline", "doTest")
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

        generateTest(
                "idea/tests/",
                "JetQuickDocProviderTestGenerated",
                AbstractJetQuickDocProviderTest.class,
                testModelWithPattern("idea/testData/editor/quickDoc", "^([^_]+)\\.[^\\.]*$", "doTest")
        );

        generateTest(
                "idea/tests/",
                "JetSafeDeleteTestGenerated",
                AbstractJetSafeDeleteTest.class,
                testModel("idea/testData/safeDelete/deleteClass/kotlinClass", "doClassTest"),
                testModel("idea/testData/safeDelete/deleteObject/kotlinObject", "doObjectTest"),
                testModel("idea/testData/safeDelete/deleteFunction/kotlinFunction", "doFunctionTest"),
                testModel("idea/testData/safeDelete/deleteFunction/kotlinFunctionWithJava", "doFunctionTestWithJava"),
                testModel("idea/testData/safeDelete/deleteFunction/javaFunctionWithKotlin", "doJavaMethodTest"),
                testModel("idea/testData/safeDelete/deleteProperty/kotlinProperty", "doPropertyTest"),
                testModel("idea/testData/safeDelete/deleteProperty/kotlinPropertyWithJava", "doPropertyTestWithJava"),
                testModel("idea/testData/safeDelete/deleteProperty/javaPropertyWithKotlin", "doJavaPropertyTest"),
                testModel("idea/testData/safeDelete/deleteTypeParameter/kotlinTypeParameter", "doTypeParameterTest"),
                testModel("idea/testData/safeDelete/deleteTypeParameter/kotlinTypeParameterWithJava", "doTypeParameterTestWithJava"),
                testModel("idea/testData/safeDelete/deleteValueParameter/kotlinValueParameter", "doValueParameterTest"),
                testModel("idea/testData/safeDelete/deleteValueParameter/kotlinValueParameterWithJava", "doValueParameterTestWithJava")
        );

        generateTest(
                "idea/tests/",
                "ReferenceResolveTestGenerated",
                AbstractResolveBaseTest.class,
                testModel("idea/testData/resolve/references", true, "doTest")
        );

        generateTest(
                "idea/tests/",
                "ReferenceResolveWithLibTestGenerated",
                AbstractResolveWithLibTest.class,
                testModel("idea/testData/resolve/referenceWithLib", false, "doTest")
        );

        generateTest(
                "idea/tests/",
                "JetFindUsagesTestGenerated",
                AbstractJetFindUsagesTest.class,
                testModelWithPattern("idea/testData/findUsages/kotlin", "^(.+)\\.0\\.kt$", "doTest"),
                testModelWithPattern("idea/testData/findUsages/java", "^(.+)\\.0\\.java$", "doTest")
        );

        generateTest(
                "idea/tests",
                "CompletionWeigherTestGenerated",
                AbstractCompletionWeigherTest.class,
                testModelWithPattern("idea/testData/completion/weighers", "^([^\\.]+)\\.kt$", "doTest")
        );

        generateTest(
                "idea/tests/",
                "ConfigureProjectByChangingFileTestGenerated",
                AbstractConfigureProjectByChangingFileTest.class,
                new SimpleTestClassModel(new File("idea/testData/configuration/android-gradle"), true, Pattern.compile("(\\w+)_before\\.gradle$"), "doTestAndroidGradle"),
                new SimpleTestClassModel(new File("idea/testData/configuration/gradle"), true, Pattern.compile("(\\w+)_before\\.gradle$"), "doTestGradle"),
                testModelWithDirectories("idea/testData/configuration/maven", "doTestWithMaven")
        );

        generateTest(
                "idea/tests/",
                "JetFormatterTestGenerated",
                AbstractJetFormatterTest.class,
                testModelWithPattern("idea/testData/formatter", "^([^\\.]+)\\.kt$", "doTest")
        );

        generateTest(
                "idea/tests/",
                "DiagnosticMessageTestGenerated",
                AbstractDiagnosticMessageTest.class,
                testModel("idea/testData/diagnosticMessage")
        );

        generateTest(
                "idea/tests/",
                "OutOfBlockModificationTestGenerated",
                AbstractOutOfBlockModificationTest.class,
                testModel("idea/testData/codeInsight/outOfBlock")
        );

        generateTest(
                "idea/tests/",
                "DataFlowValueRenderingTestGenerated",
                AbstractDataFlowValueRenderingTest.class,
                testModel("idea/testData/dataFlowValueRendering")
        );

        generateTest(
                "idea/tests/",
                "RenameTestGenerated",
                AbstractRenameTest.class,
                new SingleClassTestModel(new File("idea/testData/refactoring/rename"), Pattern.compile("^(.+)\\.test$"), "doTest")
        );

        generateTest(
                "compiler/tests",
                "AnnotationParameterTestGenerated",
                AbstractAnnotationParameterTest.class,
                testModel("compiler/testData/resolveAnnotations/parameters")
        );

        generateTest(
                "compiler/tests",
                "EvaluateExpressionTestGenerated",
                AbstractEvaluateExpressionTest.class,
                testModel("compiler/testData/evaluate/constant", "doConstantTest"),
                testModel("compiler/testData/evaluate/isPure", "doIsPureTest")
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
        return new SimpleTestClassModel(new File(rootPath), false, Pattern.compile("^(.+)$"), methodName);
    }

    private static SimpleTestClassModel testModelWithPattern(
            @NotNull String rootPath,
            @NotNull String pattern,
            @NotNull String methodName
    ) {
        return new SimpleTestClassModel(new File(rootPath), true, Pattern.compile(pattern), methodName);
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