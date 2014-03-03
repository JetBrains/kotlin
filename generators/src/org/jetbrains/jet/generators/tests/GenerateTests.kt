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

package org.jetbrains.jet.generators.tests

import org.jetbrains.jet.generators.tests.generator.TestGenerator
import java.util.ArrayList
import org.jetbrains.jet.generators.tests.generator.SimpleTestClassModel
import java.io.File
import java.util.regex.Pattern
import junit.framework.TestCase
import org.jetbrains.jet.checkers.AbstractJetDiagnosticsTest
import org.jetbrains.jet.resolve.AbstractResolveTest
import org.jetbrains.jet.parsing.AbstractJetParsingTest
import org.jetbrains.jet.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.jet.codegen.AbstractBytecodeTextTest
import org.jetbrains.jet.codegen.AbstractTopLevelMembersInvocationTest
import org.jetbrains.jet.codegen.AbstractCheckLocalVariablesTableTest
import org.jetbrains.jet.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.jet.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.jet.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.jet.jvm.compiler.AbstractCompileJavaAgainstKotlinTest
import org.jetbrains.jet.jvm.compiler.AbstractCompileKotlinAgainstKotlinTest
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveDescriptorRendererTest
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveTest
import org.jetbrains.jet.lang.resolve.lazy.AbstractLazyResolveRecursiveComparingTest
import org.jetbrains.jet.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.jet.descriptors.serialization.AbstractDescriptorSerializationTest
import org.jetbrains.jet.jvm.compiler.AbstractWriteSignatureTest
import org.jetbrains.jet.cli.AbstractKotlincExecutableTest
import org.jetbrains.jet.cfg.AbstractControlFlowTest
import org.jetbrains.jet.psi.AbstractJetPsiMatcherTest
import org.jetbrains.jet.checkers.AbstractJetPsiCheckerTest
import org.jetbrains.jet.checkers.AbstractJetJsCheckerTest
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixTest
import org.jetbrains.jet.completion.AbstractJSBasicCompletionTest
import org.jetbrains.jet.completion.AbstractKeywordCompletionTest
import org.jetbrains.jet.completion.AbstractJvmSmartCompletionTest
import org.jetbrains.jet.completion.AbstractJvmBasicCompletionTest
import org.jetbrains.jet.completion.AbstractJvmWithLibBasicCompletionTest
import org.jetbrains.jet.plugin.navigation.AbstractGotoSuperTest
import org.jetbrains.jet.plugin.quickfix.AbstractQuickFixMultiFileTest
import org.jetbrains.jet.plugin.highlighter.AbstractHighlightingTest
import org.jetbrains.jet.plugin.folding.AbstractKotlinFoldingTest
import org.jetbrains.jet.plugin.codeInsight.surroundWith.AbstractSurroundWithTest
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationTest
import org.jetbrains.jet.plugin.AbstractSmartSelectionTest
import org.jetbrains.jet.plugin.hierarchy.AbstractHierarchyTest
import org.jetbrains.jet.plugin.codeInsight.moveUpDown.AbstractCodeMoverTest
import org.jetbrains.jet.plugin.refactoring.inline.AbstractInlineTest
import org.jetbrains.jet.plugin.codeInsight.unwrap.AbstractUnwrapRemoveTest
import org.jetbrains.jet.editor.quickDoc.AbstractJetQuickDocProviderTest
import org.jetbrains.jet.safeDelete.AbstractJetSafeDeleteTest
import org.jetbrains.jet.resolve.AbstractReferenceResolveTest
import org.jetbrains.jet.resolve.AbstractReferenceResolveWithLibTest
import org.jetbrains.jet.completion.weighers.AbstractCompletionWeigherTest
import org.jetbrains.jet.findUsages.AbstractJetFindUsagesTest
import org.jetbrains.jet.plugin.configuration.AbstractConfigureProjectByChangingFileTest
import org.jetbrains.jet.formatter.AbstractJetFormatterTest
import org.jetbrains.jet.plugin.highlighter.AbstractDiagnosticMessageTest
import org.jetbrains.jet.plugin.codeInsight.AbstractOutOfBlockModificationTest
import org.jetbrains.jet.completion.AbstractDataFlowValueRenderingTest
import org.jetbrains.jet.resolve.annotation.AbstractAnnotationParameterTest
import org.jetbrains.jet.evaluate.AbstractEvaluateExpressionTest
import org.jetbrains.jet.resolve.calls.AbstractResolvedCallsTest
import org.jetbrains.jet.plugin.refactoring.rename.AbstractRenameTest
import org.jetbrains.jet.generators.tests.generator.SingleClassTestModel
import org.jetbrains.jet.generators.tests.generator.TestClassModel
import org.jetbrains.jet.j2k.test.AbstractJavaToKotlinConverterPluginTest
import org.jetbrains.jet.j2k.test.AbstractJavaToKotlinConverterBasicTest
import org.jetbrains.jet.plugin.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.jet.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.jet.completion.handlers.AbstractSmartCompletionHandlerTest
import org.jetbrains.jet.generators.tests.generator.TestGeneratorUtil
import org.jetbrains.jet.resolve.AbstractAdditionalLazyResolveDescriptorRendererTest
import org.jetbrains.jet.resolve.AbstractReferenceResolveInLibrarySourcesTest
import org.jetbrains.jet.resolve.constraintSystem.AbstractConstraintSystemTest
import org.jetbrains.jet.completion.AbstractCompiledKotlinInJavaCompletionTest
import org.jetbrains.jet.completion.AbstractKotlinSourceInJavaCompletionTest
import org.jetbrains.jet.plugin.intentions.AbstractIntentionTest
import org.jetbrains.jet.checkers.AbstractJetDiagnosticsTestWithStdLib
import org.jetbrains.jet.plugin.codeInsight.AbstractInsertImportOnPasteTest
import org.jetbrains.jet.resolve.AbstractReferenceToJavaWithWrongFileStructureTest
import org.jetbrains.jet.plugin.navigation.AbstractKotlinGotoTest
import org.jetbrains.jet.plugin.AbstractExpressionSelectionTest
import org.jetbrains.jet.plugin.refactoring.move.AbstractJetMoveTest
import org.jetbrains.jet.cfg.AbstractDataFlowTest
import org.jetbrains.jet.plugin.libraries.AbstractDecompiledTextTest
import org.jetbrains.jet.plugin.imports.AbstractOptimizeImportsTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {

        testClass(javaClass<AbstractJetDiagnosticsTest>()) {
            model("diagnostics/tests")
            model("diagnostics/tests/script", extension = "ktscript")
            model("codegen/box/functions/tailRecursion")
        }

        testClass(javaClass<AbstractJetDiagnosticsTestWithStdLib>()) {
            model("diagnostics/testsWithStdLib")
        }

        testClass(javaClass<AbstractResolveTest>()) {
            model("resolve", extension = "resolve")
        }

        testClass(javaClass<AbstractResolvedCallsTest>()) {
            model("resolvedCalls")
        }

        testClass(javaClass<AbstractConstraintSystemTest>()) {
            model("constraintSystem", extension = "bounds")
        }

        testClass(javaClass<AbstractJetParsingTest>()) {
            model("psi", testMethod = "doParsingTest")
        }

        GenerateRangesCodegenTestData.main(array<String>())

        testClass(javaClass<AbstractBlackBoxCodegenTest>()) {
            model("codegen/box")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxInlineCodegenTestGenerated") {
            model("codegen/boxInline", extension = null, recursive = false, testMethod = "doTestMultiFile")
        }

        testClass(javaClass<AbstractCompileKotlinAgainstKotlinTest>(), "CompileKotlinAgainstInlineKotlinTestGenerated") {
            model("codegen/boxInline", extension = null, recursive = false, testMethod = "doBoxTest")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxMultiFileCodegenTestGenerated") {
            model("codegen/boxMultiFile", extension = null, recursive = false, testMethod = "doTestMultiFile")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxWithJavaCodegenTestGenerated") {
            model("codegen/boxWithJava", testMethod = "doTestWithJava")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxWithStdlibCodegenTestGenerated") {
            model("codegen/boxWithStdlib", testMethod = "doTestWithStdlib")
        }

        testClass(javaClass<AbstractBytecodeTextTest>()) {
            model("codegen/bytecodeText")
        }

        testClass(javaClass<AbstractTopLevelMembersInvocationTest>()) {
            model("codegen/topLevelMemberInvocation", extension = null, recursive = false)
        }

        testClass(javaClass<AbstractCheckLocalVariablesTableTest>()) {
            model("checkLocalVariablesTable")
        }

        testClass(javaClass<AbstractWriteFlagsTest>()) {
            model("writeFlags")
        }

        testClass(javaClass<AbstractDefaultArgumentsReflectionTest>()) {
            model("codegen/defaultArguments/reflection")
        }

        testClass(javaClass<AbstractLoadJavaTest>()) {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            model("loadJava/compiledJavaAndKotlin", extension = "txt", testMethod = "doTestCompiledJavaAndKotlin")
            model("loadJava/compiledJavaCompareWithKotlin", extension = "java", testMethod = "doTestCompiledJavaCompareWithKotlin")
            model("loadJava/compiledJavaIncludeObjectMethods", extension = "java", testMethod = "doTestCompiledJavaIncludeObjectMethods")
            model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
            model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass(javaClass<AbstractCompileJavaAgainstKotlinTest>()) {
            model("compileJavaAgainstKotlin")
        }

        testClass(javaClass<AbstractCompileKotlinAgainstKotlinTest>()) {
            model("compileKotlinAgainstKotlin", extension = "A.kt")
        }

        testClass(javaClass<AbstractLazyResolveDescriptorRendererTest>()) {
            model("renderer")
        }

        testClass(javaClass<AbstractLazyResolveTest>()) {
            model("resolve/imports", recursive = false, extension = "resolve")
        }

        testClass(javaClass<AbstractLazyResolveRecursiveComparingTest>()) {
            model("loadJava/compiledKotlin", testMethod = "doTestCheckingPrimaryConstructorsAndAccessors")
            model("loadJava/compiledJavaCompareWithKotlin", testMethod = "doTestNotCheckingPrimaryConstructors")
            model("lazyResolve/recursiveComparator", testMethod = "doTestCheckingPrimaryConstructors")
        }

        testClass(javaClass<AbstractModuleXmlParserTest>()) {
            model("modules.xml", extension = "xml")
        }

        testClass(javaClass<AbstractDescriptorSerializationTest>()) {
            model("loadJava/compiledKotlin/class")
            model("loadJava/compiledKotlin/classFun")
            model("loadJava/compiledKotlin/classObject")
            model("loadJava/compiledKotlin/constructor")
            model("loadJava/compiledKotlin/fun")
            model("loadJava/compiledKotlin/prop")
            model("loadJava/compiledKotlin/type")
            model("loadJava/compiledKotlin/visibility")
        }

        testClass(javaClass<AbstractWriteSignatureTest>()) {
            model("writeSignature")
        }

        testClass(javaClass<AbstractKotlincExecutableTest>()) {
            model("cli/jvm", extension = "args", testMethod = "doJvmTest")
            model("cli/js", extension = "args", testMethod = "doJsTest")
        }

        testClass(javaClass<AbstractControlFlowTest>()) {
            model("cfg")
        }

        testClass(javaClass<AbstractDataFlowTest>()) {
            model("cfg-variables")
        }

        testClass(javaClass<AbstractAnnotationParameterTest>()) {
            model("resolveAnnotations/parameters")
        }

        testClass(javaClass<AbstractEvaluateExpressionTest>()) {
            model("evaluate/constant", testMethod = "doConstantTest")
            model("evaluate/isPure", testMethod = "doIsPureTest")
        }
    }

    testGroup("idea/tests", "idea/testData") {
        testClass(javaClass<AbstractAdditionalLazyResolveDescriptorRendererTest>()) {
            model("resolve/additionalLazyResolve", testMethod = "doTest")
        }

        testClass(javaClass<AbstractJetPsiMatcherTest>()) {
            model("jetPsiMatcher/expressions", testMethod = "doTestExpressions")
            model("jetPsiMatcher/types", testMethod = "doTestTypes")
        }

        testClass(javaClass<AbstractJetPsiCheckerTest>()) {
            model("checker", recursive = false)
            model("checker/regression")
            model("checker/rendering")
            model("checker/infos", testMethod = "doTestWithInfos")
        }

        testClass(javaClass<AbstractJetJsCheckerTest>()) {
            model("checker/js")
        }

        testClass(javaClass<AbstractQuickFixTest>()) {
            model("quickfix", pattern = "^before(\\w+)\\.kt$")
        }

        testClass(javaClass<AbstractIntentionTest>(), "ConvertToExpressionBodyTestGenerated") {
            model("intentions/convertToExpressionBody", pattern = "^before(\\w+)\\.kt$")
        }

        testClass(javaClass<AbstractIntentionTest>(), "ConvertToBlockBodyTestGenerated") {
            model("intentions/convertToBlockBody", pattern = "^before(\\w+)\\.kt$")
        }

        testClass(javaClass<AbstractJSBasicCompletionTest>()) {
            model("completion/basic/common")
            model("completion/basic/js")
        }

        testClass(javaClass<AbstractJvmBasicCompletionTest>()) {
            model("completion/basic/common")
            model("completion/basic/java")
        }

        testClass(javaClass<AbstractJvmSmartCompletionTest>()) {
            model("completion/smart")
        }

        testClass(javaClass<AbstractKeywordCompletionTest>()) {
            model("completion/keywords", recursive = false)
        }

        testClass(javaClass<AbstractJvmWithLibBasicCompletionTest>()) {
            model("completion/basic/custom", recursive = false)
        }

        testClass(javaClass<AbstractSmartCompletionHandlerTest>()) {
            model("completion/handlers/smart")
        }

        testClass(javaClass<AbstractGotoSuperTest>()) {
            model("navigation/gotoSuper", extension = "test")
        }

        testClass(javaClass<AbstractKotlinGotoTest>()) {
            model("navigation/gotoClass", testMethod = "doClassTest")
            model("navigation/gotoSymbol", testMethod = "doSymbolTest")
        }

        testClass(javaClass<AbstractQuickFixMultiFileTest>()) {
            model("quickfix", pattern = """^(\w+)\.before\.Main\.kt$""", testMethod = "doTestWithExtraFile")
        }

        testClass(javaClass<AbstractHighlightingTest>()) {
            model("highlighter")
        }

        testClass(javaClass<AbstractKotlinFoldingTest>()) {
            model("folding/noCollapse")
            model("folding/checkCollapse", testMethod = "doSettingsFoldingTest")
        }

        testClass(javaClass<AbstractSurroundWithTest>()) {
            model("codeInsight/surroundWith/if", testMethod = "doTestWithIfSurrounder")
            model("codeInsight/surroundWith/ifElse", testMethod = "doTestWithIfElseSurrounder")
            model("codeInsight/surroundWith/not", testMethod = "doTestWithNotSurrounder")
            model("codeInsight/surroundWith/parentheses", testMethod = "doTestWithParenthesesSurrounder")
            model("codeInsight/surroundWith/stringTemplate", testMethod = "doTestWithStringTemplateSurrounder")
            model("codeInsight/surroundWith/when", testMethod = "doTestWithWhenSurrounder")
            model("codeInsight/surroundWith/tryCatch", testMethod = "doTestWithTryCatchSurrounder")
            model("codeInsight/surroundWith/tryCatchFinally", testMethod = "doTestWithTryCatchFinallySurrounder")
            model("codeInsight/surroundWith/tryFinally", testMethod = "doTestWithTryFinallySurrounder")
            model("codeInsight/surroundWith/functionLiteral", testMethod = "doTestWithFunctionLiteralSurrounder")
        }

        testClass(javaClass<AbstractCodeTransformationTest>()) {
            model("intentions/branched/elvisToIfThen", testMethod = "doTestElvisToIfThen")
            model("intentions/branched/ifThenToElvis", testMethod = "doTestIfThenToElvis")
            model("intentions/branched/folding/ifToAssignment", testMethod = "doTestFoldIfToAssignment")
            model("intentions/branched/folding/ifToReturn", testMethod = "doTestFoldIfToReturn")
            model("intentions/branched/folding/ifToReturnAsymmetrically", testMethod = "doTestFoldIfToReturnAsymmetrically")
            model("intentions/branched/folding/whenToAssignment", testMethod = "doTestFoldWhenToAssignment")
            model("intentions/branched/folding/whenToReturn", testMethod = "doTestFoldWhenToReturn")
            model("intentions/branched/unfolding/assignmentToIf", testMethod = "doTestUnfoldAssignmentToIf")
            model("intentions/branched/unfolding/assignmentToWhen", testMethod = "doTestUnfoldAssignmentToWhen")
            model("intentions/branched/unfolding/propertyToIf", testMethod = "doTestUnfoldPropertyToIf")
            model("intentions/branched/unfolding/propertyToWhen", testMethod = "doTestUnfoldPropertyToWhen")
            model("intentions/branched/unfolding/returnToIf", testMethod = "doTestUnfoldReturnToIf")
            model("intentions/branched/unfolding/returnToWhen", testMethod = "doTestUnfoldReturnToWhen")
            model("intentions/branched/ifWhen/ifToWhen", testMethod = "doTestIfToWhen")
            model("intentions/branched/ifWhen/whenToIf", testMethod = "doTestWhenToIf")
            model("intentions/branched/when/flatten", testMethod = "doTestFlattenWhen")
            model("intentions/branched/when/merge", testMethod = "doTestMergeWhen")
            model("intentions/branched/when/introduceSubject", testMethod = "doTestIntroduceWhenSubject")
            model("intentions/branched/when/eliminateSubject", testMethod = "doTestEliminateWhenSubject")
            model("intentions/declarations/split", testMethod = "doTestSplitProperty")
            model("intentions/declarations/join", testMethod = "doTestJoinProperty")
            model("intentions/declarations/convertMemberToExtension", testMethod = "doTestConvertMemberToExtension")
            model("intentions/reconstructedType", testMethod = "doTestReconstructType")
            model("intentions/removeUnnecessaryParentheses", testMethod = "doTestRemoveUnnecessaryParentheses")
            model("intentions/replaceWithDotQualifiedMethodCall", testMethod = "doTestReplaceWithDotQualifiedMethodCall")
            model("intentions/replaceWithInfixFunctionCall", testMethod = "doTestReplaceWithInfixFunctionCall")
            model("intentions/removeCurlyBracesFromTemplate", testMethod = "doTestRemoveCurlyFromTemplate")
            model("intentions/insertCurlyBracestsToTemplate", testMethod = "doTestInsertCurlyToTemplate")
            model("intentions/moveLambdaInsideParentheses", testMethod = "doTestMoveLambdaInsideParentheses")
            model("intentions/moveLambdaOutsideParentheses", testMethod = "doTestMoveLambdaOutsideParentheses")
            model("intentions/replaceExplicitFunctionLiteralParamWithIt", testMethod = "doTestReplaceExplicitFunctionLiteralParamWithIt")
            model("intentions/replaceItWithExplicitFunctionLiteralParam", testMethod = "doTestReplaceItWithExplicitFunctionLiteralParam")
            model("intentions/removeBraces", testMethod = "doTestRemoveBraces")
            model("intentions/addBraces", testMethod = "doTestAddBraces")
            model("intentions/attributeCallReplacements/replaceGetIntention", testMethod = "doTestReplaceGetIntention")
            model("intentions/attributeCallReplacements/replaceContainsIntention", testMethod = "doTestReplaceContainsIntention")
            model("intentions/attributeCallReplacements/replaceBinaryInfixIntention", testMethod = "doTestReplaceBinaryInfixIntention")
            model("intentions/attributeCallReplacements/replaceUnaryPrefixIntention", testMethod = "doTestReplaceUnaryPrefixIntention")
            model("intentions/attributeCallReplacements/replaceInvokeIntention", testMethod = "doTestReplaceInvokeIntention")
        }

        testClass(javaClass<AbstractHierarchyTest>()) {
            model("hierarchy/class/type", extension = null, recursive = false, testMethod = "doTypeClassHierarchyTest")
            model("hierarchy/class/super", extension = null, recursive = false, testMethod = "doSuperClassHierarchyTest")
            model("hierarchy/class/sub", extension = null, recursive = false, testMethod = "doSubClassHierarchyTest")
            model("hierarchy/calls/callers", extension = null, recursive = false, testMethod = "doCallerHierarchyTest")
            model("hierarchy/calls/callees", extension = null, recursive = false, testMethod = "doCalleeHierarchyTest")
            model("hierarchy/overrides", extension = null, recursive = false, testMethod = "doOverrideHierarchyTest")
        }

        testClass(javaClass<AbstractCodeMoverTest>()) {
            model("codeInsight/moveUpDown/classBodyDeclarations", testMethod = "doTestClassBodyDeclaration")
            model("codeInsight/moveUpDown/closingBraces", testMethod = "doTestExpression")
            model("codeInsight/moveUpDown/expressions", testMethod = "doTestExpression")
        }

        testClass(javaClass<AbstractInlineTest>()) {
            model("refactoring/inline")
        }

        testClass(javaClass<AbstractUnwrapRemoveTest>()) {
            model("codeInsight/unwrapAndRemove/removeExpression", testMethod = "doTestExpressionRemover")
            model("codeInsight/unwrapAndRemove/unwrapThen", testMethod = "doTestThenUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapElse", testMethod = "doTestElseUnwrapper")
            model("codeInsight/unwrapAndRemove/removeElse", testMethod = "doTestElseRemover")
            model("codeInsight/unwrapAndRemove/unwrapLoop", testMethod = "doTestLoopUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapTry", testMethod = "doTestTryUnwrapper")
            model("codeInsight/unwrapAndRemove/unwrapCatch", testMethod = "doTestCatchUnwrapper")
            model("codeInsight/unwrapAndRemove/removeCatch", testMethod = "doTestCatchRemover")
            model("codeInsight/unwrapAndRemove/unwrapFinally", testMethod = "doTestFinallyUnwrapper")
            model("codeInsight/unwrapAndRemove/removeFinally", testMethod = "doTestFinallyRemover")
            model("codeInsight/unwrapAndRemove/unwrapLambda", testMethod = "doTestLambdaUnwrapper")
        }

        testClass(javaClass<AbstractJetQuickDocProviderTest>()) {
            model("editor/quickDoc", pattern = """^([^_]+)\.[^\.]*$""")
        }

        testClass(javaClass<AbstractJetSafeDeleteTest>()) {
            model("safeDelete/deleteClass/kotlinClass", testMethod = "doClassTest")
            model("safeDelete/deleteObject/kotlinObject", testMethod = "doObjectTest")
            model("safeDelete/deleteFunction/kotlinFunction", testMethod = "doFunctionTest")
            model("safeDelete/deleteFunction/kotlinFunctionWithJava", testMethod = "doFunctionTestWithJava")
            model("safeDelete/deleteFunction/javaFunctionWithKotlin", testMethod = "doJavaMethodTest")
            model("safeDelete/deleteProperty/kotlinProperty", testMethod = "doPropertyTest")
            model("safeDelete/deleteProperty/kotlinPropertyWithJava", testMethod = "doPropertyTestWithJava")
            model("safeDelete/deleteProperty/javaPropertyWithKotlin", testMethod = "doJavaPropertyTest")
            model("safeDelete/deleteTypeParameter/kotlinTypeParameter", testMethod = "doTypeParameterTest")
            model("safeDelete/deleteTypeParameter/kotlinTypeParameterWithJava", testMethod = "doTypeParameterTestWithJava")
            model("safeDelete/deleteValueParameter/kotlinValueParameter", testMethod = "doValueParameterTest")
            model("safeDelete/deleteValueParameter/kotlinValueParameterWithJava", testMethod = "doValueParameterTestWithJava")
        }

        testClass(javaClass<AbstractReferenceResolveTest>()) {
            model("resolve/references", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractReferenceResolveWithLibTest>()) {
            model("resolve/referenceWithLib", recursive = false)
        }

        testClass(javaClass<AbstractReferenceResolveInLibrarySourcesTest>()) {
            model("resolve/referenceInLib", recursive = false)
        }

        testClass(javaClass<AbstractReferenceToJavaWithWrongFileStructureTest>()) {
            model("resolve/referenceToJavaWithWrongFileStructure", recursive = false)
        }

        testClass(javaClass<AbstractJetFindUsagesTest>()) {
            model("findUsages/kotlin", pattern = """^(.+)\.0\.kt$""")
            model("findUsages/java", pattern = """^(.+)\.0\.java$""")
        }

        testClass(javaClass<AbstractJetMoveTest>()) {
            model("refactoring/move", extension = "test", singleClass = true)
        }

        testClass(javaClass<AbstractCompletionWeigherTest>()) {
            model("completion/weighers", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractConfigureProjectByChangingFileTest>()) {
            model("configuration/android-gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestAndroidGradle")
            model("configuration/gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestGradle")
            model("configuration/maven", extension = null, recursive = false, testMethod = "doTestWithMaven")
        }

        testClass(javaClass<AbstractJetFormatterTest>()) {
            model("formatter", pattern = """^([^\.]+)\.after.kt$""")
            model("formatter", pattern = """^([^\.]+)\.after.inv.kt$""",
                  testMethod = "doTestInverted", testClassName = "FormatterInverted")
        }

        testClass(javaClass<AbstractDiagnosticMessageTest>()) {
            model("diagnosticMessage")
        }

        testClass(javaClass<AbstractRenameTest>()) {
            model("refactoring/rename", extension = "test", singleClass = true)
        }

        testClass(javaClass<AbstractOutOfBlockModificationTest>()) {
            model("codeInsight/outOfBlock")
        }

        testClass(javaClass<AbstractDataFlowValueRenderingTest>()) {
            model("dataFlowValueRendering")
        }

        testClass(javaClass<AbstractJavaToKotlinCopyPasteConversionTest>()) {
            model("copyPaste/conversion", extension = "java")
        }

        testClass(javaClass<AbstractInsertImportOnPasteTest>()) {
            model("copyPaste/imports", pattern = """^([^\.]+)\.kt$""", testMethod = "doTestCopy", testClassName = "Copy", recursive = false)
            model("copyPaste/imports", pattern = """^([^\.]+)\.kt$""", testMethod = "doTestCut", testClassName = "Cut", recursive = false)
        }

        testClass(javaClass<AbstractShortenRefsTest>()) {
            model("shortenRefs", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractCompiledKotlinInJavaCompletionTest>()) {
            model("completion/injava", extension = "java")
        }

        testClass(javaClass<AbstractKotlinSourceInJavaCompletionTest>()) {
            model("completion/injava", extension = "java")
        }

        testClass(javaClass<AbstractSmartSelectionTest>()) {
            model("smartSelection", testMethod = "doTestSmartSelection", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractExpressionSelectionTest>()) {
            model("expressionSelection", testMethod = "doTestExpressionSelection", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractDecompiledTextTest>()) {
            model("libraries/decompiledText", pattern = """^([^\.]+)$""")
        }

        testClass(javaClass<AbstractOptimizeImportsTest>()) {
            model("editor/optimizeImports", extension = null, recursive = false)
        }
    }

    testGroup("j2k/tests/test", "j2k/tests/testData") {
        testClass(javaClass<AbstractJavaToKotlinConverterPluginTest>()) {
            model("ast", extension = "java")
        }

        testClass(javaClass<AbstractJavaToKotlinConverterBasicTest>()) {
            model("ast", extension = "java")
        }
    }
}

private class TestGroup(val testsRoot: String, val testDataRoot: String) {
    fun testClass(
            baseTestClass: Class<out TestCase>,
            suiteTestClass: String = getDefaultSuiteTestClass(baseTestClass),
            init: TestClass.() -> Unit) {

        val testClass = TestClass()
        testClass.init()

        TestGenerator(
                testsRoot,
                baseTestClass.getPackage()!!.getName()!!,
                suiteTestClass,
                baseTestClass,
                testClass.testModels,
                "org.jetbrains.jet.generators.tests.TestsPackage"
        ).generateAndSave()
    }

    inner class TestClass() {

        val testModels = ArrayList<TestClassModel>()

        fun model(
                relativeRootPath: String,
                recursive: Boolean = true,
                extension: String? = "kt", // null string means dir (name without dot)
                pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
                testMethod: String = "doTest",
                singleClass: Boolean = false,
                testClassName: String? = null
        ) {
            val rootFile = File(testDataRoot + "/" + relativeRootPath)
            val compiledPattern = Pattern.compile(pattern)
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(if (singleClass)
                               SingleClassTestModel(rootFile, compiledPattern, testMethod, className)
                           else
                               SimpleTestClassModel(rootFile, recursive, compiledPattern, testMethod, className))
        }
    }

}

private fun testGroup(testsRoot: String, testDataRoot: String, init: TestGroup.() -> Unit) {
    TestGroup(testsRoot, testDataRoot).init()
}

private fun getDefaultSuiteTestClass(baseTestClass:Class<*>): String {
    val baseName = baseTestClass.getSimpleName()
    if (!baseName.startsWith("Abstract")) {
        throw IllegalArgumentException("Doesn't start with \"Abstract\": $baseName")
    }
    return baseName.substring("Abstract".length) + "Generated"
}
