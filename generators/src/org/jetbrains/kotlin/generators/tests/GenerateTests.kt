/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import junit.framework.TestCase
import org.jetbrains.kotlin.AbstractDataFlowValueRenderingTest
import org.jetbrains.kotlin.addImport.AbstractAddImportTest
import org.jetbrains.kotlin.android.AbstractAndroidCompletionTest
import org.jetbrains.kotlin.android.AbstractAndroidFindUsagesTest
import org.jetbrains.kotlin.android.AbstractAndroidGotoTest
import org.jetbrains.kotlin.android.AbstractAndroidRenameTest
import org.jetbrains.kotlin.annotation.AbstractAnnotationProcessorBoxTest
import org.jetbrains.kotlin.asJava.AbstractCompilerLightClassTest
import org.jetbrains.kotlin.cfg.AbstractControlFlowTest
import org.jetbrains.kotlin.cfg.AbstractDataFlowTest
import org.jetbrains.kotlin.cfg.AbstractPseudoValueTest
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.codegen.generated.AbstractBlackBoxCodegenTest
import org.jetbrains.kotlin.codegen.generated.AbstractBlackBoxInlineCodegenTest
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesTest
import org.jetbrains.kotlin.findUsages.AbstractKotlinFindUsagesWithLibraryTest
import org.jetbrains.kotlin.formatter.AbstractFormatterTest
import org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase
import org.jetbrains.kotlin.generators.tests.generator.*
import org.jetbrains.kotlin.generators.tests.generator.TestGenerator.TargetBackend
import org.jetbrains.kotlin.idea.AbstractExpressionSelectionTest
import org.jetbrains.kotlin.idea.AbstractSmartSelectionTest
import org.jetbrains.kotlin.idea.actions.AbstractGotoTestOrCodeActionTest
import org.jetbrains.kotlin.idea.caches.resolve.AbstractIdeCompiledLightClassTest
import org.jetbrains.kotlin.idea.caches.resolve.AbstractIdeLightClassTest
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractCodeInsightActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateHashCodeAndEqualsActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateTestSupportMethodActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateToStringActionTest
import org.jetbrains.kotlin.idea.codeInsight.moveUpDown.AbstractCodeMoverTest
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.AbstractSurroundWithTest
import org.jetbrains.kotlin.idea.codeInsight.unwrap.AbstractUnwrapRemoveTest
import org.jetbrains.kotlin.idea.completion.test.*
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractBasicCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionCharFilterTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractKeywordCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractSmartCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractBasicCompletionWeigherTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractSmartCompletionWeigherTest
import org.jetbrains.kotlin.idea.configuration.AbstractConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.coverage.AbstractKotlinCoverageOutputFilesTest
import org.jetbrains.kotlin.idea.debugger.AbstractBeforeExtractFunctionInsertionTest
import org.jetbrains.kotlin.idea.debugger.AbstractKotlinSteppingTest
import org.jetbrains.kotlin.idea.debugger.AbstractPositionManagerTest
import org.jetbrains.kotlin.idea.debugger.AbstractSmartStepIntoTest
import org.jetbrains.kotlin.idea.debugger.evaluate.*
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToLibrarySourceTest
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractClsStubBuilderTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJsDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJvmDecompiledTextTest
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractQuickDocProviderTest
import org.jetbrains.kotlin.idea.filters.AbstractKotlinExceptionFilterTest
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyTest
import org.jetbrains.kotlin.idea.highlighter.*
import org.jetbrains.kotlin.idea.imports.AbstractOptimizeImportsTest
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest
import org.jetbrains.kotlin.idea.intentions.AbstractMultiFileIntentionTest
import org.jetbrains.kotlin.idea.intentions.declarations.AbstractJoinLinesTest
import org.jetbrains.kotlin.idea.internal.AbstractBytecodeToolWindowTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocHighlightingTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocTypingTest
import org.jetbrains.kotlin.idea.navigation.AbstractGotoSuperTest
import org.jetbrains.kotlin.idea.navigation.AbstractKotlinGotoImplementationTest
import org.jetbrains.kotlin.idea.navigation.AbstractKotlinGotoTest
import org.jetbrains.kotlin.idea.parameterInfo.AbstractParameterInfoTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.refactoring.inline.AbstractInlineTest
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractExtractionTest
import org.jetbrains.kotlin.idea.refactoring.move.AbstractMoveTest
import org.jetbrains.kotlin.idea.refactoring.pullUp.AbstractPullUpTest
import org.jetbrains.kotlin.idea.refactoring.pushDown.AbstractPushDownTest
import org.jetbrains.kotlin.idea.refactoring.rename.AbstractRenameTest
import org.jetbrains.kotlin.idea.refactoring.safeDelete.AbstractSafeDeleteTest
import org.jetbrains.kotlin.idea.resolve.*
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinFileStructureTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiFileHighlightingTest
import org.jetbrains.kotlin.idea.stubs.AbstractResolveByStubTest
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.integration.AbstractAntTaskTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterForWebDemoTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.build.android.AbstractAndroidJpsTestCase
import org.jetbrains.kotlin.jps.incremental.AbstractProtoComparisonTest
import org.jetbrains.kotlin.js.test.semantics.*
import org.jetbrains.kotlin.jvm.compiler.*
import org.jetbrains.kotlin.jvm.runtime.AbstractJvmRuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.lang.resolve.android.test.AbstractAndroidBoxTest
import org.jetbrains.kotlin.lang.resolve.android.test.AbstractAndroidBytecodeShapeTest
import org.jetbrains.kotlin.lang.resolve.android.test.AbstractAndroidSyntheticPropertyDescriptorTest
import org.jetbrains.kotlin.load.java.AbstractJavaTypeSubstitutorTest
import org.jetbrains.kotlin.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.kotlin.parsing.AbstractParsingTest
import org.jetbrains.kotlin.psi.patternMatching.AbstractPsiUnifierTest
import org.jetbrains.kotlin.renderer.AbstractDescriptorRendererTest
import org.jetbrains.kotlin.renderer.AbstractFunctionDescriptorInExpressionRendererTest
import org.jetbrains.kotlin.repl.AbstractReplInterpreterTest
import org.jetbrains.kotlin.resolve.AbstractResolveTest
import org.jetbrains.kotlin.resolve.annotation.AbstractAnnotationParameterTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedCallsTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedConstructorDelegationCallsTests
import org.jetbrains.kotlin.resolve.constants.evaluate.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.resolve.constraintSystem.AbstractConstraintSystemTest
import org.jetbrains.kotlin.serialization.AbstractLocalClassProtoTest
import org.jetbrains.kotlin.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.kotlin.types.AbstractTypeBindingTest
import java.io.File
import java.util.*
import java.util.regex.Pattern

private val kotlinFileOrScript = "^(.+)\\.(kt|kts)\$"

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {

        testClass<AbstractDiagnosticsTest>() {
            model("diagnostics/tests")
            model("diagnostics/tests/script", extension = "kts")
            model("codegen/box/diagnostics")
        }

        testClass<AbstractDiagnosticsTestWithStdLib>() {
            model("diagnostics/testsWithStdLib")
        }

        testClass<AbstractDiagnosticsTestWithJsStdLib>() {
            model("diagnostics/testsWithJsStdLib")
        }

        testClass<AbstractDiagnosticsTestWithJsStdLibAndBackendCompilation>() {
            model("diagnostics/testsWithJsStdLibAndBackendCompilation")
        }

        testClass<AbstractForeignAnnotationsTest>() {
            model("foreignAnnotations/tests")
        }

        testClass<AbstractResolveTest>() {
            model("resolve", extension = "resolve")
        }

        testClass<AbstractResolvedCallsTest>() {
            model("resolvedCalls")
        }

        testClass<AbstractResolvedConstructorDelegationCallsTests>() {
            model("resolveConstructorDelegationCalls")
        }

        testClass<AbstractConstraintSystemTest>() {
            model("constraintSystem", extension = "constraints")
        }

        testClass<AbstractParsingTest>() {
            model("psi", testMethod = "doParsingTest", pattern = "^(.*)\\.kts?$")
            model("parseCodeFragment/expression", testMethod = "doExpressionCodeFragmentParsingTest", extension = "kt")
            model("parseCodeFragment/block", testMethod = "doBlockCodeFragmentParsingTest", extension = "kt")
        }

        GenerateRangesCodegenTestData.main(arrayOf<String>())

        testClass<AbstractBlackBoxCodegenTest>() {
            model("codegen/box")
        }

        testClass<AbstractBlackBoxInlineCodegenTest>("BlackBoxInlineCodegenTestGenerated") {
            model("codegen/boxInline", extension = "1.kt", testMethod = "doTestMultiFileWithInlineCheck")
        }

        testClass<AbstractCompileKotlinAgainstInlineKotlinTest>("CompileKotlinAgainstInlineKotlinTestGenerated") {
            model("codegen/boxInline", extension = "1.kt", testMethod = "doBoxTestWithInlineCheck")
        }

        testClass(AbstractBlackBoxMultifileClassCodegenTest::class.java, "BlackBoxMultifileClassKotlinTestGenerated") {
            model("codegen/boxMultifileClasses", extension = "1.kt", testMethod = "doTestMultifileClassAgainstSources")
        }

        testClass(AbstractCompileKotlinAgainstMultifileKotlinTest::class.java, "CompileKotlinAgainstMultifileKotlinTestGenerated") {
            model("codegen/boxMultifileClasses", extension = "1.kt", testMethod = "doBoxTest")
        }

        testClass<AbstractBlackBoxCodegenTest>("BlackBoxMultiFileCodegenTestGenerated") {
            model("codegen/boxMultiFile", extension = null, recursive = false, testMethod = "doTestMultiFile")
        }

        testClass<AbstractBlackBoxCodegenTest>("BlackBoxAgainstJavaCodegenTestGenerated") {
            model("codegen/boxAgainstJava", testMethod = "doTestAgainstJava")
        }

        testClass<AbstractBlackBoxCodegenTest>("BlackBoxWithJavaCodegenTestGenerated") {
            model("codegen/boxWithJava", testMethod = "doTestWithJava", extension = null, recursive = true, excludeParentDirs = true)
        }

        testClass<AbstractBlackBoxCodegenTest>("BlackBoxWithStdlibCodegenTestGenerated") {
            model("codegen/boxWithStdlib", testMethod = "doTestWithStdlib")
        }

        testClass<AbstractScriptCodegenTest>() {
            model("codegen/script", extension = "kts")
        }

        testClass(AbstractBytecodeTextTest::class.java) {
            model("codegen/bytecodeText")
        }

        testClass(AbstractBytecodeTextTest::class.java, "BytecodeTextMultifileTestGenerated") {
            model("codegen/bytecodeTextMultifile", extension = null, recursive = false, testMethod = "doTestMultiFile")
        }

        testClass<AbstractBytecodeListingTest>() {
            model("codegen/bytecodeListing")
        }

        testClass<AbstractTopLevelMembersInvocationTest>() {
            model("codegen/topLevelMemberInvocation", extension = null, recursive = false)
        }

        testClass<AbstractCheckLocalVariablesTableTest>() {
            model("checkLocalVariablesTable")
        }

        testClass<AbstractWriteFlagsTest>() {
            model("writeFlags")
        }

        testClass<AbstractDefaultArgumentsReflectionTest>() {
            model("codegen/defaultArguments/reflection")
        }

        testClass<AbstractLoadJavaTest>() {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            model("loadJava/compiledJavaAndKotlin", extension = "txt", testMethod = "doTestCompiledJavaAndKotlin")
            model("loadJava/compiledJavaIncludeObjectMethods", extension = "java", testMethod = "doTestCompiledJavaIncludeObjectMethods")
            model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
            model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
            model("loadJava/kotlinAgainstCompiledJavaWithKotlin", extension = "kt", testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin", recursive = false)
            model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass<AbstractLoadKotlinWithTypeTableTest>() {
            model("loadJava/compiledKotlin")
        }

        testClass<AbstractJvmRuntimeDescriptorLoaderTest>() {
            model("loadJava/compiledKotlin")
            model("loadJava/compiledJava", extension = "java", excludeDirs = listOf("sam", "kotlinSignature/propagation"))
        }

        testClass<AbstractCompileJavaAgainstKotlinTest>() {
            model("compileJavaAgainstKotlin")
        }

        testClass<AbstractCompileKotlinAgainstKotlinTest>() {
            model("compileKotlinAgainstKotlin", extension = "A.kt")
        }

        testClass<AbstractDescriptorRendererTest>() {
            model("renderer")
        }

        testClass<AbstractFunctionDescriptorInExpressionRendererTest>() {
            model("renderFunctionDescriptorInExpression")
        }

        testClass<AbstractModuleXmlParserTest>() {
            model("modules.xml", extension = "xml")
        }

        testClass<AbstractWriteSignatureTest>() {
            model("writeSignature")
        }

        testClass<AbstractCliTest>() {
            model("cli/jvm", extension = "args", testMethod = "doJvmTest", recursive = false)
            model("cli/js", extension = "args", testMethod = "doJsTest", recursive = false)
        }

        testClass<AbstractReplInterpreterTest>() {
            model("repl", extension = "repl")
        }

        testClass<AbstractAntTaskTest>() {
            model("integration/ant/jvm", extension = null, recursive = false, excludeParentDirs = true)
        }

        testClass<AbstractControlFlowTest>() {
            model("cfg")
        }

        testClass<AbstractDataFlowTest>() {
            model("cfg-variables")
        }

        testClass<AbstractPseudoValueTest>() {
            model("cfg")
            model("cfg-variables")
        }

        testClass<AbstractAnnotationParameterTest>() {
            model("resolveAnnotations/parameters")
        }

        testClass<AbstractCompileTimeConstantEvaluatorTest>() {
            model("evaluate/constant", testMethod = "doConstantTest")
            model("evaluate/isPure", testMethod = "doIsPureTest")
            model("evaluate/usesVariableAsConstant", testMethod = "doUsesVariableAsConstantTest")
        }

        testClass<AbstractCompilerLightClassTest>() {
            model("asJava/lightClasses")
        }

        testClass<AbstractTypeBindingTest>() {
            model("type/binding")
        }

        testClass<AbstractLineNumberTest>() {
            model("lineNumber", recursive = false)
            model("lineNumber/custom", testMethod = "doTestCustom")
        }

        testClass<AbstractLocalClassProtoTest>() {
            model("serialization/local")
        }
    }

    testGroup("compiler/java8-tests/tests", "compiler/testData") {
        testClass<AbstractBlackBoxCodegenTest>("BlackBoxWithJava8CodegenTestGenerated") {
            model("codegen/java8/boxWithJava", testMethod = "doTestWithJava", extension = null, recursive = true, excludeParentDirs = true)
        }
        testClass<AbstractDiagnosticsWithFullJdkTest>("DiagnosticsWithJava8TestGenerated") {
            model("diagnostics/testsWithJava8")
        }
    }


    testGroup("idea/tests", "idea/testData") {

        testClass<AbstractJavaTypeSubstitutorTest>() {
            model("typeSubstitution", extension = "java")
        }

        testClass<AbstractAdditionalResolveDescriptorRendererTest>() {
            model("resolve/additionalLazyResolve")
        }

        testClass<AbstractPartialBodyResolveTest>() {
            model("resolve/partialBodyResolve")
        }

        testClass<AbstractPsiCheckerTest>() {
            model("checker", recursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/scripts", extension = "kts")
            model("checker/duplicateJvmSignature")
            model("checker/infos", testMethod = "doTestWithInfos")
        }

        testClass<AbstractJavaAgainstKotlinSourceCheckerTest>() {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
            model("kotlinAndJavaChecker/javaWithKotlin")
        }

        testClass<AbstractJavaAgainstKotlinBinariesCheckerTest>() {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
        }

        testClass<AbstractPsiUnifierTest>() {
            model("unifier")
        }

        testClass<AbstractCodeFragmentHighlightingTest>() {
            model("checker/codeFragments", extension = "kt", recursive = false)
            model("checker/codeFragments/imports", testMethod = "doTestWithImport", extension = "kt")
        }

        testClass<AbstractCodeFragmentAutoImportTest>() {
            model("quickfix.special/codeFragmentAutoImport", extension = "kt", recursive = false)
        }

        testClass<AbstractJsCheckerTest>() {
            model("checker/js")
        }

        testClass<AbstractQuickFixTest>() {
            model("quickfix", pattern = "^([\\w\\-_]+)\\.kt$", filenameStartsLowerCase = true)
        }

        testClass<AbstractGotoSuperTest>() {
            model("navigation/gotoSuper", extension = "test")
        }

        testClass<AbstractParameterInfoTest>() {
            model("parameterInfo", recursive = true, excludeDirs = listOf("withLib/sharedLib"))
        }

        testClass<AbstractKotlinGotoTest>() {
            model("navigation/gotoClass", testMethod = "doClassTest")
            model("navigation/gotoSymbol", testMethod = "doSymbolTest")
        }

        testClass<AbstractNavigateToLibrarySourceTest>() {
            model("decompiler/navigation/usercode")
            model("decompiler/navigation/usercode", testClassName ="UsercodeWithJSModule", testMethod = "doWithJSModuleTest")
        }

        testClass<AbstractKotlinGotoImplementationTest>() {
            model("navigation/implementations", recursive = false)
        }

        testClass<AbstractGotoTestOrCodeActionTest>() {
            model("navigation/gotoTestOrCode", pattern = "^(.+)\\.main\\..+\$")
        }

        testClass<AbstractQuickFixMultiFileTest>() {
            model("quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""", testMethod = "doTestWithExtraFile")
        }

        testClass<AbstractHighlightingTest>() {
            model("highlighter")
        }

        testClass<AbstractUsageHighlightingTest>() {
            model("usageHighlighter")
        }

        testClass<AbstractKotlinFoldingTest>() {
            model("folding/noCollapse")
            model("folding/checkCollapse", testMethod = "doSettingsFoldingTest")
        }

        testClass<AbstractSurroundWithTest>() {
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

        testClass<AbstractJoinLinesTest>() {
            model("joinLines")
        }

        testClass<AbstractIntentionTest>() {
            model("intentions", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractInspectionTest>() {
            model("intentions", pattern = "^(inspections\\.test)$", singleClass = true)
            model("inspections", pattern = "^(inspections\\.test)$", singleClass = true)
        }

        testClass<AbstractHierarchyTest>() {
            model("hierarchy/class/type", extension = null, recursive = false, testMethod = "doTypeClassHierarchyTest")
            model("hierarchy/class/super", extension = null, recursive = false, testMethod = "doSuperClassHierarchyTest")
            model("hierarchy/class/sub", extension = null, recursive = false, testMethod = "doSubClassHierarchyTest")
            model("hierarchy/calls/callers", extension = null, recursive = false, testMethod = "doCallerHierarchyTest")
            model("hierarchy/calls/callees", extension = null, recursive = false, testMethod = "doCalleeHierarchyTest")
            model("hierarchy/overrides", extension = null, recursive = false, testMethod = "doOverrideHierarchyTest")
        }

        testClass<AbstractCodeMoverTest>() {
            model("codeInsight/moveUpDown/classBodyDeclarations", testMethod = "doTestClassBodyDeclaration")
            model("codeInsight/moveUpDown/closingBraces", testMethod = "doTestExpression")
            model("codeInsight/moveUpDown/expressions", testMethod = "doTestExpression")
        }

        testClass<AbstractInlineTest>() {
            model("refactoring/inline", pattern = "^(\\w+)\\.kt$")
        }

        testClass<AbstractUnwrapRemoveTest>() {
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

        testClass<AbstractQuickDocProviderTest>() {
            model("editor/quickDoc", pattern = """^([^_]+)\.[^\.]*$""")
        }

        testClass<AbstractSafeDeleteTest>() {
            model("refactoring/safeDelete/deleteClass/kotlinClass", testMethod = "doClassTest")
            model("refactoring/safeDelete/deleteClass/kotlinClassWithJava", testMethod = "doClassTestWithJava")
            model("refactoring/safeDelete/deleteObject/kotlinObject", testMethod = "doObjectTest")
            model("refactoring/safeDelete/deleteFunction/kotlinFunction", testMethod = "doFunctionTest")
            model("refactoring/safeDelete/deleteFunction/kotlinFunctionWithJava", testMethod = "doFunctionTestWithJava")
            model("refactoring/safeDelete/deleteFunction/javaFunctionWithKotlin", testMethod = "doJavaMethodTest")
            model("refactoring/safeDelete/deleteProperty/kotlinProperty", testMethod = "doPropertyTest")
            model("refactoring/safeDelete/deleteProperty/kotlinPropertyWithJava", testMethod = "doPropertyTestWithJava")
            model("refactoring/safeDelete/deleteProperty/javaPropertyWithKotlin", testMethod = "doJavaPropertyTest")
            model("refactoring/safeDelete/deleteTypeParameter/kotlinTypeParameter", testMethod = "doTypeParameterTest")
            model("refactoring/safeDelete/deleteTypeParameter/kotlinTypeParameterWithJava", testMethod = "doTypeParameterTestWithJava")
            model("refactoring/safeDelete/deleteValueParameter/kotlinValueParameter", testMethod = "doValueParameterTest")
            model("refactoring/safeDelete/deleteValueParameter/kotlinValueParameterWithJava", testMethod = "doValueParameterTestWithJava")
        }

        testClass<AbstractReferenceResolveTest>() {
            model("resolve/references", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractReferenceResolveInJavaTest>() {
            model("resolve/referenceInJava", extension = "java")
        }

        testClass<AbstractReferenceResolveWithLibTest>() {
            model("resolve/referenceWithLib", recursive = false)
        }

        testClass<AbstractReferenceResolveInLibrarySourcesTest>() {
            model("resolve/referenceInLib", recursive = false)
        }

        testClass<AbstractReferenceToJavaWithWrongFileStructureTest>() {
            model("resolve/referenceToJavaWithWrongFileStructure", recursive = false)
        }

        testClass<AbstractFindUsagesTest>() {
            model("findUsages/kotlin", pattern = """^(.+)\.0\.kt$""")
            model("findUsages/java", pattern = """^(.+)\.0\.java$""")
            model("findUsages/propertyFiles", pattern = """^(.+)\.0\.properties$""")
        }

        testClass<AbstractKotlinFindUsagesWithLibraryTest>() {
            model("findUsages/libraryUsages", pattern = """^(.+)\.0\.kt$""")
        }

        testClass<AbstractMoveTest>() {
            model("refactoring/move", extension = "test", singleClass = true)
        }

        testClass<AbstractMultiFileIntentionTest>() {
            model("multiFileIntentions", extension = "test", singleClass = true, filenameStartsLowerCase = true)
        }

        testClass<AbstractMultiFileInspectionTest>() {
            model("multiFileInspections", extension = "test", singleClass = true)
        }

        testClass<AbstractConfigureProjectByChangingFileTest>() {
            model("configuration/android-gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestAndroidGradle")
            model("configuration/gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestGradle")
            model("configuration/maven", extension = null, recursive = false, testMethod = "doTestWithMaven")
            model("configuration/js-maven", extension = null, recursive = false, testMethod = "doTestWithJSMaven")
        }

        testClass<AbstractFormatterTest>() {
            model("formatter", pattern = """^([^\.]+)\.after\.kt.*$""")
            model("formatter", pattern = """^([^\.]+)\.after\.inv\.kt.*$""",
                  testMethod = "doTestInverted", testClassName = "FormatterInverted")
        }

        testClass<AbstractTypingIndentationTestBase>() {
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.kt.*$""", testMethod = "doNewlineTest",
                  testClassName = "DirectSettings")
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.inv\.kt.*$""", testMethod = "doNewlineTestWithInvert",
                  testClassName = "InvertedSettings")
        }

        testClass<AbstractDiagnosticMessageTest>() {
            model("diagnosticMessage", recursive = false)
        }

        testClass<AbstractDiagnosticMessageJsTest>() {
            model("diagnosticMessage/js", recursive = false, targetBackend = TargetBackend.JS)
        }

        testClass<AbstractRenameTest>() {
            model("refactoring/rename", extension = "test", singleClass = true)
        }

        testClass<AbstractOutOfBlockModificationTest>() {
            model("codeInsight/outOfBlock")
        }

        testClass<AbstractDataFlowValueRenderingTest>() {
            model("dataFlowValueRendering")
        }

        testClass<AbstractJavaToKotlinCopyPasteConversionTest>() {
            model("copyPaste/conversion", pattern = """^([^\.]+)\.java$""")
        }

        testClass<AbstractInsertImportOnPasteTest>() {
            model("copyPaste/imports", pattern = """^([^\.]+)\.kt$""", testMethod = "doTestCopy", testClassName = "Copy", recursive = false)
            model("copyPaste/imports", pattern = """^([^\.]+)\.kt$""", testMethod = "doTestCut", testClassName = "Cut", recursive = false)
        }

        testClass<AbstractHighlightExitPointsTest>() {
            model("exitPoints")
        }

        testClass<AbstractLineMarkersTest>() {
            model("codeInsight/lineMarker")
        }

        testClass<AbstractShortenRefsTest>() {
            model("shortenRefs", pattern = """^([^\.]+)\.kt$""")
        }
        testClass<AbstractAddImportTest>() {
            model("addImport", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractSmartSelectionTest>() {
            model("smartSelection", testMethod = "doTestSmartSelection", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractKotlinFileStructureTest>() {
            model("structureView/fileStructure", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractExpressionSelectionTest>() {
            model("expressionSelection", testMethod = "doTestExpressionSelection", pattern = """^([^\.]+)\.kt$""")
        }

        testClass(AbstractCommonDecompiledTextTest::class.java) {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""")
        }

        testClass(AbstractJvmDecompiledTextTest::class.java) {
            model("decompiler/decompiledTextJvm", pattern = """^([^\.]+)$""")
        }

        testClass(AbstractCommonDecompiledTextFromJsMetadataTest::class.java) {
            model("decompiler/decompiledText", pattern = """^([^\.]+)$""", targetBackend = TargetBackend.JS)
        }

        testClass(AbstractJsDecompiledTextFromJsMetadataTest::class.java) {
            model("decompiler/decompiledTextJs", pattern = """^([^\.]+)$""", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractClsStubBuilderTest>() {
            model("decompiler/stubBuilder", extension = null, recursive = false)
        }

        testClass<AbstractOptimizeImportsTest>() {
            model("editor/optimizeImports", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractPositionManagerTest>() {
            model("debugger/positionManager", recursive = false, extension = "kt", testClassName = "SingleFile")
            model("debugger/positionManager", recursive = false, extension = null, testClassName = "MultiFile")
        }

        testClass<AbstractKotlinExceptionFilterTest>() {
            model("debugger/exceptionFilter", pattern = """^([^\.]+)$""", recursive = false)
        }

        testClass<AbstractSmartStepIntoTest>() {
            model("debugger/smartStepInto")
        }

        testClass<AbstractBeforeExtractFunctionInsertionTest>() {
            model("debugger/insertBeforeExtractFunction", extension = "kt")
        }

        testClass<AbstractKotlinSteppingTest>() {
            model("debugger/tinyApp/src/stepping/stepIntoAndSmartStepInto", testMethod = "doStepIntoTest", testClassName = "StepInto")
            model("debugger/tinyApp/src/stepping/stepIntoAndSmartStepInto", testMethod = "doSmartStepIntoTest", testClassName = "SmartStepInto")
            model("debugger/tinyApp/src/stepping/stepInto", testMethod = "doStepIntoTest", testClassName = "StepIntoOnly")
            model("debugger/tinyApp/src/stepping/stepOut", testMethod = "doStepOutTest")
            model("debugger/tinyApp/src/stepping/stepOver", testMethod = "doStepOverTest")
            model("debugger/tinyApp/src/stepping/filters", testMethod = "doStepIntoTest")
            model("debugger/tinyApp/src/stepping/custom", testMethod = "doCustomTest")
        }

        testClass<AbstractKotlinEvaluateExpressionTest>() {
            model("debugger/tinyApp/src/evaluate/singleBreakpoint", testMethod = "doSingleBreakpointTest")
            model("debugger/tinyApp/src/evaluate/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest")
        }

        testClass<AbstractStubBuilderTest>() {
            model("stubs", extension = "kt")
        }

        testClass<AbstractMultiFileHighlightingTest>() {
            model("multiFileHighlighting", recursive = false)
        }

        testClass<AbstractExtractionTest>() {
            model("refactoring/introduceVariable", pattern = kotlinFileOrScript, testMethod = "doIntroduceVariableTest")
            model("refactoring/extractFunction", pattern = kotlinFileOrScript, testMethod = "doExtractFunctionTest")
            model("refactoring/introduceProperty", pattern = kotlinFileOrScript, testMethod = "doIntroducePropertyTest")
            model("refactoring/introduceParameter", pattern = kotlinFileOrScript, testMethod = "doIntroduceSimpleParameterTest")
            model("refactoring/introduceLambdaParameter", pattern = kotlinFileOrScript, testMethod = "doIntroduceLambdaParameterTest")
            model("refactoring/introduceJavaParameter", extension = "java", testMethod = "doIntroduceJavaParameterTest")
        }

        testClass<AbstractPullUpTest>() {
            model("refactoring/pullUp/k2k", extension = "kt", singleClass = true, testClassName = "K2K", testMethod = "doKotlinTest")
            model("refactoring/pullUp/k2j", extension = "kt", singleClass = true, testClassName = "K2J", testMethod = "doKotlinTest")
            model("refactoring/pullUp/j2k", extension = "java", singleClass = true, testClassName = "J2K", testMethod = "doJavaTest")
        }

        testClass<AbstractPushDownTest>() {
            model("refactoring/pushDown", extension = "kt", singleClass = true)
        }

        testClass<AbstractSelectExpressionForDebuggerTest>() {
            model("debugger/selectExpression", recursive = false)
            model("debugger/selectExpression/disallowMethodCalls", testMethod = "doTestWoMethodCalls")
        }

        testClass<AbstractKotlinCoverageOutputFilesTest>() {
            model("coverage/outputFiles")
        }

        testClass<AbstractBytecodeToolWindowTest>() {
            model("internal/toolWindow", recursive = false, extension = null)
        }

        testClass<AbstractReferenceResolveTest>("org.jetbrains.kotlin.idea.kdoc.KdocResolveTestGenerated") {
            model("kdoc/resolve")
        }

        testClass<AbstractKDocHighlightingTest>() {
            model("kdoc/highlighting")
        }

        testClass<AbstractKDocTypingTest>() {
            model("kdoc/typing")
        }

        testClass<AbstractGenerateTestSupportMethodActionTest>() {
            model("codeInsight/generate/testFrameworkSupport")
        }

        testClass<AbstractGenerateHashCodeAndEqualsActionTest>() {
            model("codeInsight/generate/equalsWithHashCode")
        }

        testClass<AbstractCodeInsightActionTest>() {
            model("codeInsight/generate/secondaryConstructors")
        }

        testClass<AbstractGenerateToStringActionTest>() {
            model("codeInsight/generate/toString")
        }
    }

    testGroup("idea/tests", "compiler/testData") {
        testClass<AbstractResolveByStubTest>() {
            model("loadJava/compiledKotlin")
        }
    }

    testGroup("idea/tests", "compiler/testData") {
        testClass<AbstractIdeLightClassTest>() {
            model("asJava/lightClasses", excludeDirs = listOf("delegation"))
        }

        testClass<AbstractIdeCompiledLightClassTest> {
            model("asJava/lightClasses", pattern = """^([^\.]+)\.kt$""")
        }
    }

    testGroup("idea/idea-completion/tests", "idea/idea-completion/testData") {
        testClass<AbstractCompiledKotlinInJavaCompletionTest>() {
            model("injava", extension = "java")
        }

        testClass<AbstractKotlinSourceInJavaCompletionTest>() {
            model("injava", extension = "java")
        }

        testClass<AbstractBasicCompletionWeigherTest>() {
            model("weighers/basic", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractSmartCompletionWeigherTest>() {
            model("weighers/smart", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractJSBasicCompletionTest>() {
            model("basic/common")
            model("basic/js")
        }

        testClass<AbstractJvmBasicCompletionTest>() {
            model("basic/common")
            model("basic/java")
        }

        testClass<AbstractJvmSmartCompletionTest>() {
            model("smart")
        }

        testClass<AbstractKeywordCompletionTest>() {
            model("keywords", recursive = false)
        }

        testClass<AbstractJvmWithLibBasicCompletionTest>() {
            model("basic/withLib", recursive = false)
        }

        testClass<AbstractBasicCompletionHandlerTest>() {
            model("handlers/basic")
        }

        testClass<AbstractSmartCompletionHandlerTest>() {
            model("handlers/smart")
        }

        testClass<AbstractKeywordCompletionHandlerTest>() {
            model("handlers/keywords")
        }

        testClass<AbstractCompletionCharFilterTest>() {
            model("handlers/charFilter")
        }

        testClass<AbstractMultiFileJvmBasicCompletionTest>() {
            model("basic/multifile", extension = null, recursive = false)
        }

        testClass<AbstractMultiFileSmartCompletionTest>() {
            model("smartMultiFile", extension = null, recursive = false)
        }

        testClass<AbstractJvmBasicCompletionTest>("org.jetbrains.kotlin.idea.completion.test.KDocCompletionTestGenerated") {
            model("kdoc")
        }
    }

    //TODO: move these tests into idea-completion module
    testGroup("idea/tests", "idea/idea-completion/testData") {
        testClass<AbstractCodeFragmentCompletionHandlerTest>() {
            model("handlers/runtimeCast")
        }

        testClass<AbstractCodeFragmentCompletionTest>() {
            model("basic/codeFragments", extension = "kt")
        }
    }

    testGroup("j2k/tests", "j2k/testData") {
        testClass<AbstractJavaToKotlinConverterSingleFileTest>() {
            model("fileOrElement", extension = "java")
        }
    }
    testGroup("j2k/tests", "j2k/testData") {
        testClass<AbstractJavaToKotlinConverterMultiFileTest>() {
            model("multiFile", extension = null, recursive = false)
        }
    }
    testGroup("j2k/tests", "j2k/testData") {
        testClass<AbstractJavaToKotlinConverterForWebDemoTest>() {
            model("fileOrElement", extension = "java")
        }
    }

    testGroup("jps-plugin/test", "jps-plugin/testData") {
        testClass<AbstractIncrementalJpsTest>() {
            model("incremental/multiModule", extension = null, excludeParentDirs = true)
            model("incremental/pureKotlin", extension = null, excludeParentDirs = true)
            model("incremental/withJava", extension = null, excludeParentDirs = true)
            model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true)
        }
        testClass<AbstractLookupTrackerTest>() {
            model("incremental/lookupTracker", extension = null, recursive = false)
        }

        testClass(AbstractIncrementalLazyCachesTest::class.java) {
            model("incremental/lazyKotlinCaches", extension = null, excludeParentDirs = true)
        }

        testClass(AbstractIncrementalCacheVersionChangedTest::class.java) {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("jps-plugin/test", "jps-plugin/testData") {
        testClass<AbstractExperimentalIncrementalJpsTest>() {
            model("incremental/multiModule", extension = null, excludeParentDirs = true)
            model("incremental/pureKotlin", extension = null, excludeParentDirs = true)
            model("incremental/withJava", extension = null, excludeParentDirs = true)
            model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true)
            model("incremental/classHierarchyAffected", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractExperimentalIncrementalLazyCachesTest>() {
            model("incremental/lazyKotlinCaches", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractExperimentalIncrementalCacheVersionChangedTest>() {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractDataContainerVersionChangedTest>() {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractExperimentalChangeIncrementalOptionTest>() {
            model("incremental/changeIncrementalOption", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("jps-plugin/test", "jps-plugin/testData") {
        testClass<AbstractProtoComparisonTest>() {
            model("comparison/classSignatureChange", extension = null, excludeParentDirs = true)
            model("comparison/classPrivateOnlyChange", extension = null, excludeParentDirs = true)
            model("comparison/classMembersOnlyChanged", extension = null, excludeParentDirs = true)
            model("comparison/packageMembers", extension = null, excludeParentDirs = true)
            model("comparison/unchanged", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("plugins/android-compiler-plugin/tests", "plugins/android-compiler-plugin/testData") {
        testClass<AbstractAndroidSyntheticPropertyDescriptorTest>() {
            model("descriptors", recursive = false, extension = null)
        }

        testClass<AbstractAndroidBoxTest>() {
            model("codegen/android", recursive = false, extension = null, testMethod = "doCompileAgainstAndroidSdkTest")
            model("codegen/android", recursive = false, extension = null, testMethod = "doFakeInvocationTest", testClassName = "Invoke")
        }

        testClass<AbstractAndroidBytecodeShapeTest>() {
            model("codegen/bytecodeShape", recursive = false, extension = null)
        }
    }

    testGroup("plugins/annotation-collector/test", "plugins/annotation-collector/testData") {
        testClass<AbstractAnnotationProcessorBoxTest>() {
            model("collectToFile", recursive = false, extension = null)
        }
    }

    testGroup("plugins/android-idea-plugin/tests", "plugins/android-idea-plugin/testData") {
        testClass<AbstractAndroidCompletionTest>() {
            model("android/completion", recursive = false, extension = null)
        }

        testClass<AbstractAndroidGotoTest>() {
            model("android/goto", recursive = false, extension = null)
        }

        testClass<AbstractAndroidRenameTest>() {
            model("android/rename", recursive = false, extension = null)
        }

        testClass<AbstractAndroidFindUsagesTest>() {
            model("android/findUsages", recursive = false, extension = null)
        }
    }

    testGroup("plugins/android-jps-plugin/tests", "plugins/android-jps-plugin/testData") {
        testClass<AbstractAndroidJpsTestCase>() {
            model("android", recursive = false, extension = null)
        }
    }

    generateTestDataForReservedWords()

    testGroup("js/js.tests/test", "js/js.translator/testData") {
        testClass<AbstractReservedWordTest>() {
            model("reservedWords/cases")
        }

        testClass<AbstractDynamicTest>() {
            model("dynamic/cases")
        }

        testClass<AbstractMultiModuleTest>() {
            model("multiModule/cases", extension = null, recursive =false)
        }

        testClass<AbstractInlineJsTest>() {
            model("inline/cases")
        }

        testClass<AbstractInlineJsStdlibTest>() {
            model("inlineStdlib/cases")
        }

        testClass<AbstractInlineEvaluationOrderTest>() {
            model("inlineEvaluationOrder/cases")
        }

        testClass<AbstractInlineMultiModuleTest>() {
            model("inlineMultiModule/cases", extension = null, recursive =false)
        }

        testClass<AbstractLabelTest>() {
            model("labels/cases")
        }

        testClass<AbstractJsCodeTest>() {
            model("jsCode/cases")
        }

        testClass<AbstractInlineSizeReductionTest>() {
            model("inlineSizeReduction/cases")
        }

        testClass<AbstractReifiedTest>() {
            model("reified/cases")
        }
    }

    testGroup("js/js.tests/test", "compiler/testData") {
        testClass<AbstractBridgeTest>() {
            model("codegen/box/bridges", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractCompanionObjectTest>() {
            model("codegen/box/objectIntrinsics", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractFunctionExpressionTest>() {
            model("codegen/box/functions/functionExpression", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractSecondaryConstructorTest>() {
            model("codegen/box/secondaryConstructors", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractNestedTypesTest>() {
            model("codegen/box/classes/inner", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractClassesTest>() {
            model("codegen/box/classes/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractInnerNestedTest>() {
            model("codegen/box/innerNested/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractSuperTest>() {
            model("codegen/box/super/", targetBackend = TargetBackend.JS)
        }
    }
}

internal class TestGroup(val testsRoot: String, val testDataRoot: String) {
    inline fun <reified T: TestCase> testClass(
            suiteTestClass: String = getDefaultSuiteTestClass(T::class.java),
            noinline init: TestClass.() -> Unit
    ) {
        testClass(T::class.java, suiteTestClass, init)
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
        val suiteTestClassPackage = if (lastDot == -1) baseTestClass.`package`.name else suiteTestClass.substring(0, lastDot)

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
                targetBackend: TargetBackend = TargetBackend.ANY,
                excludeDirs: List<String> = listOf(),
                filenameStartsLowerCase: Boolean? = null
        ) {
            val rootFile = File(testDataRoot + "/" + relativeRootPath)
            val compiledPattern = Pattern.compile(pattern)
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(
                    if (singleClass) {
                        if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")
                        SingleClassTestModel(rootFile, compiledPattern, filenameStartsLowerCase, testMethod, className, targetBackend)
                    }
                    else {
                        SimpleTestClassModel(rootFile, recursive, excludeParentDirs,
                                             compiledPattern, filenameStartsLowerCase, testMethod, className, 
                                             targetBackend, excludeDirs)
                    }
            )
        }
    }

}

private fun testGroup(testsRoot: String, testDataRoot: String, init: TestGroup.() -> Unit) {
    TestGroup(testsRoot, testDataRoot).init()
}

private fun getDefaultSuiteTestClass(baseTestClass:Class<*>): String {
    val baseName = baseTestClass.simpleName
    if (!baseName.startsWith("Abstract")) {
        throw IllegalArgumentException("Doesn't start with \"Abstract\": $baseName")
    }
    return baseName.substring("Abstract".length) + "Generated"
}
