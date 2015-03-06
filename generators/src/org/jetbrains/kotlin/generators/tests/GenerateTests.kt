/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.tests.generator.TestGenerator
import org.jetbrains.kotlin.generators.tests.generator.TestGenerator.TargetBackend
import java.util.ArrayList
import org.jetbrains.kotlin.generators.tests.generator.SimpleTestClassModel
import java.io.File
import java.util.regex.Pattern
import junit.framework.TestCase
import org.jetbrains.kotlin.checkers.AbstractJetDiagnosticsTest
import org.jetbrains.kotlin.resolve.AbstractResolveTest
import org.jetbrains.kotlin.parsing.AbstractJetParsingTest
import org.jetbrains.kotlin.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.codegen.AbstractBytecodeTextTest
import org.jetbrains.kotlin.codegen.AbstractTopLevelMembersInvocationTest
import org.jetbrains.kotlin.codegen.AbstractCheckLocalVariablesTableTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.jvm.compiler.AbstractLoadJavaTest
import org.jetbrains.kotlin.jvm.compiler.AbstractCompileJavaAgainstKotlinTest
import org.jetbrains.kotlin.jvm.compiler.AbstractCompileKotlinAgainstKotlinTest
import org.jetbrains.kotlin.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.kotlin.jvm.compiler.AbstractWriteSignatureTest
import org.jetbrains.kotlin.cli.AbstractKotlincExecutableTest
import org.jetbrains.kotlin.repl.AbstractReplInterpreterTest
import org.jetbrains.kotlin.cfg.AbstractControlFlowTest
import org.jetbrains.kotlin.checkers.AbstractJetPsiCheckerTest
import org.jetbrains.kotlin.checkers.AbstractJetJsCheckerTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.completion.AbstractJSBasicCompletionTest
import org.jetbrains.kotlin.completion.AbstractKeywordCompletionTest
import org.jetbrains.kotlin.completion.AbstractJvmSmartCompletionTest
import org.jetbrains.kotlin.completion.AbstractJvmBasicCompletionTest
import org.jetbrains.kotlin.completion.AbstractJvmWithLibBasicCompletionTest
import org.jetbrains.kotlin.idea.navigation.AbstractGotoSuperTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.AbstractSurroundWithTest
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest
import org.jetbrains.kotlin.idea.AbstractSmartSelectionTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyTest
import org.jetbrains.kotlin.idea.codeInsight.moveUpDown.AbstractCodeMoverTest
import org.jetbrains.kotlin.idea.refactoring.inline.AbstractInlineTest
import org.jetbrains.kotlin.idea.codeInsight.unwrap.AbstractUnwrapRemoveTest
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractJetQuickDocProviderTest
import org.jetbrains.kotlin.safeDelete.AbstractJetSafeDeleteTest
import org.jetbrains.kotlin.idea.resolve.AbstractReferenceResolveTest
import org.jetbrains.kotlin.idea.resolve.AbstractReferenceResolveWithLibTest
import org.jetbrains.kotlin.findUsages.AbstractJetFindUsagesTest
import org.jetbrains.kotlin.idea.configuration.AbstractConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.formatter.AbstractJetFormatterTest
import org.jetbrains.kotlin.idea.codeInsight.AbstractOutOfBlockModificationTest
import org.jetbrains.kotlin.completion.AbstractDataFlowValueRenderingTest
import org.jetbrains.kotlin.resolve.annotation.AbstractAnnotationParameterTest
import org.jetbrains.kotlin.resolve.constants.evaluate.AbstractEvaluateExpressionTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedCallsTest
import org.jetbrains.kotlin.idea.refactoring.rename.AbstractRenameTest
import org.jetbrains.kotlin.generators.tests.generator.SingleClassTestModel
import org.jetbrains.kotlin.generators.tests.generator.TestClassModel
import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.kotlin.completion.handlers.AbstractSmartCompletionHandlerTest
import org.jetbrains.kotlin.generators.tests.generator.TestGeneratorUtil
import org.jetbrains.kotlin.idea.resolve.AbstractAdditionalResolveDescriptorRendererTest
import org.jetbrains.kotlin.idea.resolve.AbstractReferenceResolveInLibrarySourcesTest
import org.jetbrains.kotlin.resolve.constraintSystem.AbstractConstraintSystemTest
import org.jetbrains.kotlin.completion.AbstractCompiledKotlinInJavaCompletionTest
import org.jetbrains.kotlin.completion.AbstractKotlinSourceInJavaCompletionTest
import org.jetbrains.kotlin.checkers.AbstractJetDiagnosticsTestWithStdLib
import org.jetbrains.kotlin.idea.codeInsight.AbstractInsertImportOnPasteTest
import org.jetbrains.kotlin.idea.codeInsight.AbstractLineMarkersTest
import org.jetbrains.kotlin.idea.resolve.AbstractReferenceToJavaWithWrongFileStructureTest
import org.jetbrains.kotlin.idea.navigation.AbstractKotlinGotoTest
import org.jetbrains.kotlin.idea.AbstractExpressionSelectionTest
import org.jetbrains.kotlin.idea.refactoring.move.AbstractJetMoveTest
import org.jetbrains.kotlin.cfg.AbstractDataFlowTest
import org.jetbrains.kotlin.idea.imports.AbstractOptimizeImportsTest
import org.jetbrains.kotlin.idea.debugger.AbstractSmartStepIntoTest
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.idea.codeInsight.AbstractJetInspectionTest
import org.jetbrains.kotlin.idea.debugger.AbstractKotlinSteppingTest
import org.jetbrains.kotlin.idea.debugger.AbstractJetPositionManagerTest
import org.jetbrains.kotlin.completion.AbstractMultiFileJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractJetExtractionTest
import org.jetbrains.kotlin.formatter.AbstractJetTypingIndentationTestBase
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractKotlinEvaluateExpressionTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractSelectExpressionForDebuggerTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentCompletionTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentHighlightingTest
import org.jetbrains.kotlin.idea.stubs.AbstractResolveByStubTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiFileHighlightingTest
import org.jetbrains.kotlin.cfg.AbstractPseudoValueTest
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinFileStructureTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.jps.build.AbstractIncrementalJpsTest
import org.jetbrains.kotlin.asJava.AbstractKotlinLightClassTest
import org.jetbrains.kotlin.load.java.AbstractJavaTypeSubstitutorTest
import org.jetbrains.kotlin.idea.intentions.declarations.AbstractJoinLinesTest
import org.jetbrains.kotlin.codegen.AbstractScriptCodegenTest
import org.jetbrains.kotlin.idea.parameterInfo.AbstractFunctionParameterInfoTest
import org.jetbrains.kotlin.psi.patternMatching.AbstractJetPsiUnifierTest
import org.jetbrains.kotlin.completion.weighers.AbstractBasicCompletionWeigherTest
import org.jetbrains.kotlin.completion.weighers.AbstractSmartCompletionWeigherTest
import org.jetbrains.kotlin.generators.tests.reservedWords.generateTestDataForReservedWords
import org.jetbrains.kotlin.idea.resolve.AbstractReferenceResolveInJavaTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterForWebDemoTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractDecompiledTextTest
import org.jetbrains.kotlin.completion.AbstractMultiFileSmartCompletionTest
import org.jetbrains.kotlin.completion.handlers.AbstractCompletionCharFilterTest
import org.jetbrains.kotlin.serialization.AbstractLocalClassProtoTest
import org.jetbrains.kotlin.idea.resolve.AbstractPartialBodyResolveTest
import org.jetbrains.kotlin.checkers.AbstractJetDiagnosticsTestWithJsStdLib
import org.jetbrains.kotlin.renderer.AbstractDescriptorRendererTest
import org.jetbrains.kotlin.types.AbstractJetTypeBindingTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentCompletionHandlerTest
import org.jetbrains.kotlin.idea.coverage.AbstractKotlinCoverageOutputFilesTest
import org.jetbrains.kotlin.completion.handlers.AbstractBasicCompletionHandlerTest
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractClsStubBuilderTest
import org.jetbrains.kotlin.codegen.AbstractLineNumberTest
import org.jetbrains.kotlin.completion.handlers.AbstractKeywordCompletionHandlerTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocHighlightingTest
import org.jetbrains.kotlin.addImport.AbstractAddImportTest
import org.jetbrains.kotlin.idea.highlighter.*
import org.jetbrains.kotlin.android.AbstractAndroidCompletionTest
import org.jetbrains.kotlin.android.AbstractAndroidGotoTest
import org.jetbrains.kotlin.jps.build.android.AbstractAndroidJpsTestCase
import org.jetbrains.kotlin.android.AbstractAndroidRenameTest
import org.jetbrains.kotlin.android.AbstractAndroidFindUsagesTest
import org.jetbrains.kotlin.lang.resolve.android.test.AbstractAndroidBytecodeShapeTest
import org.jetbrains.kotlin.lang.resolve.android.test.AbstractAndroidXml2KConversionTest
import org.jetbrains.kotlin.lang.resolve.android.test.AbstractAndroidBoxTest
import org.jetbrains.kotlin.android.AbstractParserResultEqualityTest
import org.jetbrains.kotlin.js.test.semantics.*

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {

        testClass(javaClass<AbstractJetDiagnosticsTest>()) {
            model("diagnostics/tests")
            model("diagnostics/tests/script", extension = "kts")
            model("codegen/box/diagnostics")
        }

        testClass(javaClass<AbstractJetDiagnosticsTestWithStdLib>()) {
            model("diagnostics/testsWithStdLib")
        }

        testClass(javaClass<AbstractJetDiagnosticsTestWithJsStdLib>()) {
            model("diagnostics/testsWithJsStdLib")
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
            model("psi", testMethod = "doParsingTest", pattern = "^(.*)\\.kts?$")
            model("parseCodeFragment/expression", testMethod = "doExpressionCodeFragmentParsingTest", extension = "kt")
            model("parseCodeFragment/block", testMethod = "doBlockCodeFragmentParsingTest", extension = "kt")
        }

        GenerateRangesCodegenTestData.main(array<String>())

        testClass(javaClass<AbstractBlackBoxCodegenTest>()) {
            model("codegen/box")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxInlineCodegenTestGenerated") {
            model("codegen/boxInline", extension = "1.kt", testMethod = "doTestMultiFileWithInlineCheck")
        }

        testClass(javaClass<AbstractCompileKotlinAgainstKotlinTest>(), "CompileKotlinAgainstInlineKotlinTestGenerated") {
            model("codegen/boxInline", extension = "1.kt", testMethod = "doBoxTestWithInlineCheck")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxMultiFileCodegenTestGenerated") {
            model("codegen/boxMultiFile", extension = null, recursive = false, testMethod = "doTestMultiFile")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxAgainstJavaCodegenTestGenerated") {
            model("codegen/boxAgainstJava", testMethod = "doTestAgainstJava")
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxWithJavaCodegenTestGenerated") {
            model("codegen/boxWithJava", testMethod = "doTestWithJava", extension = null, recursive = true, excludeParentDirs = true)
        }

        testClass(javaClass<AbstractBlackBoxCodegenTest>(), "BlackBoxWithStdlibCodegenTestGenerated") {
            model("codegen/boxWithStdlib", testMethod = "doTestWithStdlib")
        }

        testClass(javaClass<AbstractScriptCodegenTest>()) {
            model("codegen/script", extension = "kts")
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
            model("loadJava/compiledJavaIncludeObjectMethods", extension = "java", testMethod = "doTestCompiledJavaIncludeObjectMethods")
            model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
            model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
            model("loadJava/kotlinAgainstCompiledJavaWithKotlin", extension = "kt", testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin", recursive = false)
            model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass(javaClass<AbstractCompileJavaAgainstKotlinTest>()) {
            model("compileJavaAgainstKotlin")
        }

        testClass(javaClass<AbstractCompileKotlinAgainstKotlinTest>()) {
            model("compileKotlinAgainstKotlin", extension = "A.kt")
        }

        testClass(javaClass<AbstractDescriptorRendererTest>()) {
            model("renderer")
        }

        testClass(javaClass<AbstractModuleXmlParserTest>()) {
            model("modules.xml", extension = "xml")
        }

        testClass(javaClass<AbstractWriteSignatureTest>()) {
            model("writeSignature")
        }

        testClass(javaClass<AbstractKotlincExecutableTest>()) {
            model("cli/jvm", extension = "args", testMethod = "doJvmTest", recursive = false)
            model("cli/js", extension = "args", testMethod = "doJsTest", recursive = false)
        }

        testClass(javaClass<AbstractReplInterpreterTest>()) {
            model("repl", extension = "repl")
        }

        testClass(javaClass<AbstractControlFlowTest>()) {
            model("cfg")
        }

        testClass(javaClass<AbstractDataFlowTest>()) {
            model("cfg-variables")
        }

        testClass(javaClass<AbstractPseudoValueTest>()) {
            model("cfg")
            model("cfg-variables")
        }

        testClass(javaClass<AbstractAnnotationParameterTest>()) {
            model("resolveAnnotations/parameters")
        }

        testClass(javaClass<AbstractEvaluateExpressionTest>()) {
            model("evaluate/constant", testMethod = "doConstantTest")
            model("evaluate/isPure", testMethod = "doIsPureTest")
            model("evaluate/usesVariableAsConstant", testMethod = "doUsesVariableAsConstantTest")
        }

        testClass(javaClass<AbstractKotlinLightClassTest>()) {
            model("asJava/lightClasses")
        }

        testClass(javaClass<AbstractJetTypeBindingTest>()) {
            model("type/binding")
        }

        testClass(javaClass<AbstractLineNumberTest>()) {
            model("lineNumber", recursive = false)
            model("lineNumber/custom", testMethod = "doTestCustom")
        }

        testClass(javaClass<AbstractLocalClassProtoTest>()) {
            model("serialization/local")
        }
    }

    testGroup("idea/tests", "idea/testData") {

        testClass(javaClass<AbstractJavaTypeSubstitutorTest>()) {
            model("typeSubstitution", extension = "java")
        }

        testClass(javaClass<AbstractAdditionalResolveDescriptorRendererTest>()) {
            model("resolve/additionalLazyResolve")
        }

        testClass(javaClass<AbstractPartialBodyResolveTest>()) {
            model("resolve/partialBodyResolve")
        }

        testClass(javaClass<AbstractJetPsiCheckerTest>()) {
            model("checker", recursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/duplicateJvmSignature")
            model("checker/infos", testMethod = "doTestWithInfos")
        }

        testClass(javaClass<AbstractJetPsiUnifierTest>()) {
            model("unifier")
        }

        testClass(javaClass<AbstractCodeFragmentHighlightingTest>()) {
            model("checker/codeFragments", extension = "kt", recursive = false)
            model("checker/codeFragments/imports", testMethod = "doTestWithImport", extension = "kt")
        }

        testClass(javaClass<AbstractJetJsCheckerTest>()) {
            model("checker/js")
        }

        testClass(javaClass<AbstractQuickFixTest>()) {
            model("quickfix", pattern = "^before(\\w+)\\.kt$")
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

        testClass(javaClass<AbstractBasicCompletionHandlerTest>()) {
            model("completion/handlers/basic")
        }

        testClass(javaClass<AbstractSmartCompletionHandlerTest>()) {
            model("completion/handlers/smart")
        }

        testClass(javaClass<AbstractKeywordCompletionHandlerTest>()) {
            model("completion/handlers/keywords")
        }

        testClass(javaClass<AbstractCompletionCharFilterTest>()) {
            model("completion/handlers/charFilter")
        }

        testClass(javaClass<AbstractCodeFragmentCompletionHandlerTest>()) {
            model("completion/handlers/runtimeCast")
        }

        testClass(javaClass<AbstractCodeFragmentCompletionTest>()) {
            model("completion/basic/codeFragments", extension = "kt")
        }

        testClass(javaClass<AbstractMultiFileJvmBasicCompletionTest>()) {
            model("completion/basic/multifile", extension = null, recursive = false)
        }
        testClass(javaClass<AbstractMultiFileSmartCompletionTest>()) {
            model("completion/smartMultiFile", extension = null, recursive = false)
        }

        testClass(javaClass<AbstractGotoSuperTest>()) {
            model("navigation/gotoSuper", extension = "test")
        }

        testClass(javaClass<AbstractFunctionParameterInfoTest>()) {
            model("parameterInfo/functionParameterInfo")
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

        testClass(javaClass<AbstractJoinLinesTest>()) {
            model("joinLines")
        }

        testClass(javaClass<AbstractIntentionTest>()) {
            model("intentions")
        }

        testClass(javaClass<AbstractJetInspectionTest>()) {
            model("intentions", pattern = "^(inspections\\.test)$", singleClass = true)
            model("inspections", pattern = "^(inspections\\.test)$", singleClass = true)
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

        testClass(javaClass<AbstractReferenceResolveInJavaTest>()) {
            model("resolve/referenceInJava", extension = "java")
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

        testClass(javaClass<AbstractBasicCompletionWeigherTest>()) {
            model("completion/weighers/basic", pattern = """^([^\.]+)\.kt$""")
        }
        testClass(javaClass<AbstractSmartCompletionWeigherTest>()) {
            model("completion/weighers/smart", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractConfigureProjectByChangingFileTest>()) {
            model("configuration/android-gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestAndroidGradle")
            model("configuration/gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestGradle")
            model("configuration/maven", extension = null, recursive = false, testMethod = "doTestWithMaven")
            model("configuration/js-maven", extension = null, recursive = false, testMethod = "doTestWithJSMaven")
        }

        testClass(javaClass<AbstractJetFormatterTest>()) {
            model("formatter", pattern = """^([^\.]+)\.after\.kt.*$""")
            model("formatter", pattern = """^([^\.]+)\.after\.inv\.kt.*$""",
                  testMethod = "doTestInverted", testClassName = "FormatterInverted")
        }

        testClass(javaClass<AbstractJetTypingIndentationTestBase>()) {
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.kt.*$""", testMethod = "doNewlineTest",
                  testClassName = "DirectSettings")
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.inv\.kt.*$""", testMethod = "doNewlineTestWithInvert",
                  testClassName = "InvertedSettings")
        }

        testClass(javaClass<AbstractDiagnosticMessageTest>()) {
            model("diagnosticMessage", recursive = false)
        }

        testClass(javaClass<AbstractDiagnosticMessageJsTest>()) {
            model("diagnosticMessage/js", recursive = false, targetBackend = TargetBackend.JS)
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

        testClass(javaClass<AbstractLineMarkersTest>()) {
            model("codeInsight/lineMarker")
        }

        testClass(javaClass<AbstractShortenRefsTest>()) {
            model("shortenRefs", pattern = """^([^\.]+)\.kt$""")
        }
        testClass(javaClass<AbstractAddImportTest>()) {
            model("addImport", pattern = """^([^\.]+)\.kt$""")
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

        testClass(javaClass<AbstractKotlinFileStructureTest>()) {
            model("structureView/fileStructure", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractExpressionSelectionTest>()) {
            model("expressionSelection", testMethod = "doTestExpressionSelection", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractDecompiledTextTest>()) {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""")
        }

        testClass(javaClass<AbstractClsStubBuilderTest>()) {
            model("decompiler/stubBuilder", extension = null, recursive = false)
        }

        testClass(javaClass<AbstractOptimizeImportsTest>()) {
            model("editor/optimizeImports", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(javaClass<AbstractJetPositionManagerTest>()) {
            model("debugger/positionManager", recursive = false, extension = "kt", testClassName = "SingleFile")
            model("debugger/positionManager", recursive = false, extension = null, testClassName = "MultiFile")
        }

        testClass(javaClass<AbstractSmartStepIntoTest>()) {
            model("debugger/smartStepInto")
        }

        testClass(javaClass<AbstractKotlinSteppingTest>()) {
            model("debugger/tinyApp/src/stepInto/stepIntoAndSmartStepInto", testMethod = "doStepIntoTest", testClassName = "StepInto")
            model("debugger/tinyApp/src/stepInto/stepIntoAndSmartStepInto", testMethod = "doSmartStepIntoTest", testClassName = "SmartStepInto")
            model("debugger/tinyApp/src/stepInto/stepInto", testMethod = "doStepIntoTest", testClassName = "StepIntoOnly")
            model("debugger/tinyApp/src/filters", testMethod = "doStepIntoTest")
        }

        testClass(javaClass<AbstractKotlinEvaluateExpressionTest>()) {
            model("debugger/tinyApp/src/evaluate/singleBreakpoint", testMethod = "doSingleBreakpointTest")
            model("debugger/tinyApp/src/evaluate/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest")
        }

        testClass(javaClass<AbstractStubBuilderTest>()) {
            model("stubs", extension = "kt")
        }

        testClass(javaClass<AbstractMultiFileHighlightingTest>()) {
            model("multiFileHighlighting", recursive = false)
        }

        testClass(javaClass<AbstractJetExtractionTest>()) {
            model("refactoring/introduceVariable", extension = "kt", testMethod = "doIntroduceVariableTest")
            model("refactoring/extractFunction", extension = "kt", testMethod = "doExtractFunctionTest")
            model("refactoring/introduceProperty", extension = "kt", testMethod = "doIntroducePropertyTest")
        }

        testClass(javaClass<AbstractSelectExpressionForDebuggerTest>()) {
            model("debugger/selectExpression", recursive = false)
            model("debugger/selectExpression/disallowMethodCalls", testMethod = "doTestWoMethodCalls")
        }

        testClass(javaClass<AbstractKotlinCoverageOutputFilesTest>()) {
            model("coverage/outputFiles")
        }

        testClass(javaClass<AbstractReferenceResolveTest>(), "org.jetbrains.kotlin.idea.kdoc.KdocResolveTestGenerated") {
            model("kdoc/resolve")
        }
        
        testClass(javaClass<AbstractKDocHighlightingTest>()) {
            model("kdoc/highlighting")
        }

        testClass<AbstractJvmBasicCompletionTest>("org.jetbrains.kotlin.idea.kdoc.KDocCompletionTestGenerated") {
            model("kdoc/completion")
        }
    }

    testGroup("idea/tests", "compiler/testData") {
        testClass(javaClass<AbstractResolveByStubTest>()) {
            model("loadJava/compiledKotlin")
        }
    }

    testGroup("j2k/tests", "j2k/testData") {
        testClass(javaClass<AbstractJavaToKotlinConverterSingleFileTest>()) {
            model("fileOrElement", extension = "java")
        }
    }
    testGroup("j2k/tests", "j2k/testData") {
        testClass(javaClass<AbstractJavaToKotlinConverterMultiFileTest>()) {
            model("multiFile", extension = null)
        }
    }
    testGroup("j2k/tests", "j2k/testData") {
        testClass(javaClass<AbstractJavaToKotlinConverterForWebDemoTest>()) {
            model("fileOrElement", extension = "java")
        }
    }

    testGroup("jps-plugin/test", "jps-plugin/testData") {
        testClass(javaClass<AbstractIncrementalJpsTest>()) {
            model("incremental/multiModule", extension = null, excludeParentDirs = true)
            model("incremental/pureKotlin", extension = null, excludeParentDirs = true)
            model("incremental/withJava", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("plugins/android-compiler-plugin/tests", "plugins/android-compiler-plugin/testData") {
        testClass(javaClass<AbstractAndroidXml2KConversionTest>()) {
            model("android/converter/simple", recursive = false, extension = null)
            model("android/converter/exceptions", recursive = false, extension = null, testMethod = "doNoManifestTest")
        }

        testClass(javaClass<AbstractAndroidBoxTest>()) {
            model("codegen/android", recursive = false, extension = null, testMethod = "doCompileAgainstAndroidSdkTest")
            model("codegen/android", recursive = false, extension = null, testMethod = "doFakeInvocationTest", testClassName = "Invoke")
        }

        testClass(javaClass<AbstractAndroidBytecodeShapeTest>()) {
            model("codegen/bytecodeShape", recursive = false, extension = null)
        }
    }

    testGroup("plugins/android-idea-plugin/tests", "plugins/android-idea-plugin/testData") {
        testClass(javaClass<AbstractParserResultEqualityTest>()) {
            model("android/parserResultEquality", recursive = false, extension = null)
        }

        testClass(javaClass<AbstractAndroidCompletionTest>()) {
            model("android/completion", recursive = false, extension = null)
        }

        testClass(javaClass<AbstractAndroidGotoTest>()) {
            model("android/goto", recursive = false, extension = null)
        }

        testClass(javaClass<AbstractAndroidRenameTest>()) {
            model("android/rename", recursive = false, extension = null)
        }

        testClass(javaClass<AbstractAndroidFindUsagesTest>()) {
            model("android/findUsages", recursive = false, extension = null)
        }
    }

    testGroup("plugins/android-jps-plugin/tests", "plugins/android-jps-plugin/testData") {
        testClass(javaClass<AbstractAndroidJpsTestCase>()) {
            model("android", recursive = false, extension = null)
        }
    }

    generateTestDataForReservedWords()

    testGroup("js/js.tests/test", "js/js.translator/testData") {
        testClass(javaClass<AbstractReservedWordTest>()) {
            model("reservedWords/cases")
        }

        testClass(javaClass<AbstractDynamicTest>()) {
            model("dynamic/cases")
        }

        testClass(javaClass<AbstractMultiModuleTest>()) {
            model("multiModule/cases", extension = null, recursive=false)
        }
    }

    testGroup("js/js.tests/test", "compiler/testData") {
        testClass(javaClass<AbstractBridgeTest>()) {
            model("codegen/box/bridges", targetBackend = TargetBackend.JS)
        }

        testClass(javaClass<AbstractDefaultObjectTest>()) {
            model("codegen/box/objectIntrinsics", targetBackend = TargetBackend.JS)
        }
    }
}

private class TestGroup(val testsRoot: String, val testDataRoot: String) {
    inline fun <reified T: TestCase> testClass(
            suiteTestClass: String = getDefaultSuiteTestClass(javaClass<T>()),
            [noinline] init: TestClass.() -> Unit
    ) {
        testClass(javaClass<T>(), suiteTestClass, init)
    }

    fun testClass(
            baseTestClass: Class<out TestCase>,
            suiteTestClass: String = getDefaultSuiteTestClass(baseTestClass),
            init: TestClass.() -> Unit
    ) {
        val testClass = TestClass()
        testClass.init()

        val lastDot = suiteTestClass.lastIndexOf('.')
        val suiteTestClassName = if (lastDot == -1) suiteTestClass else suiteTestClass.substring(lastDot+1)
        val suiteTestClassPackage = if (lastDot == -1) baseTestClass.getPackage().getName() else suiteTestClass.substring(0, lastDot)

        TestGenerator(
                testsRoot,
                suiteTestClassPackage,
                suiteTestClassName,
                baseTestClass,
                testClass.testModels
        ).generateAndSave()
    }

    inner class TestClass {
        val testModels = ArrayList<TestClassModel>()

        fun model(
                relativeRootPath: String,
                recursive: Boolean = true,
                excludeParentDirs: Boolean = false,
                extension: String? = "kt", // null string means dir (name without dot)
                pattern: String = if (extension == null) """^([^\.]+)$""" else "^(.+)\\.$extension\$",
                testMethod: String = "doTest",
                singleClass: Boolean = false,
                testClassName: String? = null,
                targetBackend: TargetBackend = TargetBackend.ANY
        ) {
            val rootFile = File(testDataRoot + "/" + relativeRootPath)
            val compiledPattern = Pattern.compile(pattern)
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(if (singleClass)
                               SingleClassTestModel(rootFile, compiledPattern, testMethod, className, targetBackend)
                           else
                               SimpleTestClassModel(rootFile, recursive, excludeParentDirs, compiledPattern, testMethod, className, targetBackend))
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
    return baseName.substring("Abstract".length()) + "Generated"
}
