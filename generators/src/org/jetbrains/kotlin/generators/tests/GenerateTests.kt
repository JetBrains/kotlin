/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.AbstractDataFlowValueRenderingTest
import org.jetbrains.kotlin.addImport.AbstractAddImportTest
import org.jetbrains.kotlin.allopen.AbstractBytecodeListingTestForAllOpen
import org.jetbrains.kotlin.android.*
import org.jetbrains.kotlin.android.annotator.AbstractAndroidGutterIconTest
import org.jetbrains.kotlin.android.configure.AbstractConfigureProjectTest
import org.jetbrains.kotlin.android.folding.AbstractAndroidResourceFoldingTest
import org.jetbrains.kotlin.android.intention.AbstractAndroidIntentionTest
import org.jetbrains.kotlin.android.intention.AbstractAndroidResourceIntentionTest
import org.jetbrains.kotlin.android.lint.AbstractKotlinLintTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelBytecodeListingTest
import org.jetbrains.kotlin.android.quickfix.AbstractAndroidLintQuickfixTest
import org.jetbrains.kotlin.android.quickfix.AbstractAndroidQuickFixMultiFileTest
import org.jetbrains.kotlin.asJava.AbstractCompilerLightClassTest
import org.jetbrains.kotlin.cfg.AbstractControlFlowTest
import org.jetbrains.kotlin.cfg.AbstractDataFlowTest
import org.jetbrains.kotlin.cfg.AbstractDiagnosticsWithModifiedMockJdkTest
import org.jetbrains.kotlin.cfg.AbstractPseudoValueTest
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.cli.AbstractCliTest
import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.codegen.defaultConstructor.AbstractDefaultArgumentsReflectionTest
import org.jetbrains.kotlin.codegen.flags.AbstractWriteFlagsTest
import org.jetbrains.kotlin.codegen.ir.AbstractIrBlackBoxCodegenTest
import org.jetbrains.kotlin.codegen.ir.AbstractIrBlackBoxInlineCodegenTest
import org.jetbrains.kotlin.codegen.ir.AbstractIrCompileKotlinAgainstInlineKotlinTest
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesTest
import org.jetbrains.kotlin.findUsages.AbstractKotlinFindUsagesWithLibraryTest
import org.jetbrains.kotlin.formatter.AbstractFormatterTest
import org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase
import org.jetbrains.kotlin.generators.tests.generator.*
import org.jetbrains.kotlin.idea.AbstractExpressionSelectionTest
import org.jetbrains.kotlin.idea.AbstractKotlinTypeAliasByExpansionShortNameIndexTest
import org.jetbrains.kotlin.idea.AbstractSmartSelectionTest
import org.jetbrains.kotlin.idea.actions.AbstractGotoTestOrCodeActionTest
import org.jetbrains.kotlin.idea.caches.resolve.AbstractIdeCompiledLightClassTest
import org.jetbrains.kotlin.idea.caches.resolve.AbstractIdeLightClassTest
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractCodeInsightActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateHashCodeAndEqualsActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateTestSupportMethodActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateToStringActionTest
import org.jetbrains.kotlin.idea.codeInsight.moveUpDown.AbstractMoveLeftRightTest
import org.jetbrains.kotlin.idea.codeInsight.moveUpDown.AbstractMoveStatementTest
import org.jetbrains.kotlin.idea.codeInsight.postfix.AbstractPostfixTemplateProviderTest
import org.jetbrains.kotlin.idea.codeInsight.surroundWith.AbstractSurroundWithTest
import org.jetbrains.kotlin.idea.codeInsight.unwrap.AbstractUnwrapRemoveTest
import org.jetbrains.kotlin.idea.completion.test.*
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractBasicCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractCompletionCharFilterTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractKeywordCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.handlers.AbstractSmartCompletionHandlerTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractBasicCompletionWeigherTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractSmartCompletionWeigherTest
import org.jetbrains.kotlin.idea.configuration.AbstractGradleConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralKotlinToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralTextToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractTextJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.coverage.AbstractKotlinCoverageOutputFilesTest
import org.jetbrains.kotlin.idea.debugger.AbstractBeforeExtractFunctionInsertionTest
import org.jetbrains.kotlin.idea.debugger.AbstractKotlinSteppingTest
import org.jetbrains.kotlin.idea.debugger.AbstractPositionManagerTest
import org.jetbrains.kotlin.idea.debugger.AbstractSmartStepIntoTest
import org.jetbrains.kotlin.idea.debugger.evaluate.*
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToLibrarySourceTest
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractClsStubBuilderTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJsDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJvmDecompiledTextTest
import org.jetbrains.kotlin.idea.editor.AbstractMultiLineStringIndentTest
import org.jetbrains.kotlin.idea.editor.backspaceHandler.AbstractBackspaceHandlerTest
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractQuickDocProviderTest
import org.jetbrains.kotlin.idea.filters.AbstractKotlinExceptionFilterTest
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyWithLibTest
import org.jetbrains.kotlin.idea.highlighter.*
import org.jetbrains.kotlin.idea.imports.AbstractJsOptimizeImportsTest
import org.jetbrains.kotlin.idea.imports.AbstractJvmOptimizeImportsTest
import org.jetbrains.kotlin.idea.inspections.AbstractLocalInspectionTest
import org.jetbrains.kotlin.idea.intentions.AbstractConcatenatedStringGeneratorTest
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest
import org.jetbrains.kotlin.idea.intentions.AbstractIntentionTest2
import org.jetbrains.kotlin.idea.intentions.AbstractMultiFileIntentionTest
import org.jetbrains.kotlin.idea.intentions.declarations.AbstractJoinLinesTest
import org.jetbrains.kotlin.idea.internal.AbstractBytecodeToolWindowTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocHighlightingTest
import org.jetbrains.kotlin.idea.kdoc.AbstractKDocTypingTest
import org.jetbrains.kotlin.idea.maven.AbstractKotlinMavenInspectionTest
import org.jetbrains.kotlin.idea.maven.configuration.AbstractMavenConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.navigation.*
import org.jetbrains.kotlin.idea.parameterInfo.AbstractParameterInfoTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixTest
import org.jetbrains.kotlin.idea.refactoring.AbstractNameSuggestionProviderTest
import org.jetbrains.kotlin.idea.refactoring.copy.AbstractCopyTest
import org.jetbrains.kotlin.idea.refactoring.copy.AbstractMultiModuleCopyTest
import org.jetbrains.kotlin.idea.refactoring.inline.AbstractInlineTest
import org.jetbrains.kotlin.idea.refactoring.introduce.AbstractExtractionTest
import org.jetbrains.kotlin.idea.refactoring.move.AbstractMoveTest
import org.jetbrains.kotlin.idea.refactoring.move.AbstractMultiModuleMoveTest
import org.jetbrains.kotlin.idea.refactoring.pullUp.AbstractPullUpTest
import org.jetbrains.kotlin.idea.refactoring.pushDown.AbstractPushDownTest
import org.jetbrains.kotlin.idea.refactoring.rename.AbstractMultiModuleRenameTest
import org.jetbrains.kotlin.idea.refactoring.rename.AbstractRenameTest
import org.jetbrains.kotlin.idea.refactoring.safeDelete.AbstractSafeDeleteTest
import org.jetbrains.kotlin.idea.repl.AbstractIdeReplCompletionTest
import org.jetbrains.kotlin.idea.resolve.*
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationHighlightingTest
import org.jetbrains.kotlin.idea.script.AbstractScriptConfigurationNavigationTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerTest
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinFileStructureTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiFileHighlightingTest
import org.jetbrains.kotlin.idea.stubs.AbstractResolveByStubTest
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.integration.AbstractAntTaskTest
import org.jetbrains.kotlin.ir.AbstractIrCfgTestCase
import org.jetbrains.kotlin.ir.AbstractIrSourceRangesTestCase
import org.jetbrains.kotlin.ir.AbstractIrTextTestCase
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterForWebDemoTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.build.android.AbstractAndroidJpsTestCase
import org.jetbrains.kotlin.jps.incremental.AbstractProtoComparisonTest
import org.jetbrains.kotlin.js.test.AbstractDceTest
import org.jetbrains.kotlin.js.test.AbstractJsLineNumberTest
import org.jetbrains.kotlin.js.test.semantics.*
import org.jetbrains.kotlin.jvm.compiler.*
import org.jetbrains.kotlin.jvm.runtime.AbstractJvm8RuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.jvm.runtime.AbstractJvmRuntimeDescriptorLoaderTest
import org.jetbrains.kotlin.kapt3.test.AbstractClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.AbstractKotlinKaptContextTest
import org.jetbrains.kotlin.kdoc.AbstractKDocLexerTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBytecodeShapeTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidSyntheticPropertyDescriptorTest
import org.jetbrains.kotlin.annotation.AbstractAnnotationProcessorBoxTest
import org.jetbrains.kotlin.modules.xml.AbstractModuleXmlParserTest
import org.jetbrains.kotlin.multiplatform.AbstractMultiPlatformIntegrationTest
import org.jetbrains.kotlin.noarg.AbstractBlackBoxCodegenTestForNoArg
import org.jetbrains.kotlin.noarg.AbstractBytecodeListingTestForNoArg
import org.jetbrains.kotlin.parsing.AbstractParsingTest
import org.jetbrains.kotlin.psi.patternMatching.AbstractPsiUnifierTest
import org.jetbrains.kotlin.renderer.AbstractDescriptorRendererTest
import org.jetbrains.kotlin.renderer.AbstractFunctionDescriptorInExpressionRendererTest
import org.jetbrains.kotlin.repl.AbstractReplInterpreterTest
import org.jetbrains.kotlin.resolve.AbstractResolveTest
import org.jetbrains.kotlin.resolve.annotation.AbstractAnnotationParameterTest
import org.jetbrains.kotlin.resolve.calls.AbstractEnhancedSignaturesResolvedCallsTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedCallsTest
import org.jetbrains.kotlin.resolve.calls.AbstractResolvedConstructorDelegationCallsTests
import org.jetbrains.kotlin.resolve.constants.evaluate.AbstractCompileTimeConstantEvaluatorTest
import org.jetbrains.kotlin.resolve.constraintSystem.AbstractConstraintSystemTest
import org.jetbrains.kotlin.samWithReceiver.AbstractSamWithReceiverScriptTest
import org.jetbrains.kotlin.samWithReceiver.AbstractSamWithReceiverTest
import org.jetbrains.kotlin.search.AbstractAnnotatedMembersSearchTest
import org.jetbrains.kotlin.search.AbstractInheritorsSearchTest
import org.jetbrains.kotlin.serialization.AbstractLocalClassProtoTest
import org.jetbrains.kotlin.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.types.AbstractTypeBindingTest
import java.io.File
import java.lang.IllegalArgumentException
import java.util.*
import java.util.regex.Pattern

@Language("RegExp") private val KT_OR_KTS = """^(.+)\.(kt|kts)$"""
@Language("RegExp") private val KT_WITHOUT_DOTS_IN_NAME = """^([^.]+)\.kt$"""

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("compiler/tests", "compiler/testData") {

        testClass<AbstractDiagnosticsTest> {
            model("diagnostics/tests")
            model("diagnostics/tests/script", extension = "kts")
            model("codegen/box/diagnostics")
        }

        testClass<AbstractDiagnosticsTestWithStdLib> {
            model("diagnostics/testsWithStdLib")
        }

        testClass<AbstractDiagnosticsTestWithJsStdLib> {
            model("diagnostics/testsWithJsStdLib")
        }

        testClass<AbstractDiagnosticsTestWithJsStdLibAndBackendCompilation> {
            model("diagnostics/testsWithJsStdLibAndBackendCompilation")
        }

        testClass<AbstractDiagnosticsWithModifiedMockJdkTest> {
            model("diagnostics/testWithModifiedMockJdk")
        }

        testClass<AbstractDiagnosticsWithJdk9Test> {
            model("diagnostics/testsWithJava9")
        }

        testClass<AbstractMultiPlatformIntegrationTest> {
            model("multiplatform", extension = null, recursive = true, excludeParentDirs = true)
        }

        testClass<AbstractForeignAnnotationsTest> {
            model("foreignAnnotations/tests")
        }

        testClass<AbstractResolveTest> {
            model("resolve", extension = "resolve")
        }

        testClass<AbstractResolvedCallsTest> {
            model("resolvedCalls", excludeDirs = listOf("enhancedSignatures"))
        }

        testClass<AbstractResolvedConstructorDelegationCallsTests> {
            model("resolveConstructorDelegationCalls")
        }

        testClass<AbstractConstraintSystemTest> {
            model("constraintSystem", extension = "constraints")
        }

        testClass<AbstractParsingTest> {
            model("psi", testMethod = "doParsingTest", pattern = "^(.*)\\.kts?$")
            model("parseCodeFragment/expression", testMethod = "doExpressionCodeFragmentParsingTest", extension = "kt")
            model("parseCodeFragment/block", testMethod = "doBlockCodeFragmentParsingTest", extension = "kt")
        }

        GenerateRangesCodegenTestData.main(arrayOf<String>())

        testClass<AbstractBlackBoxCodegenTest> {
            model("codegen/box", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractLightAnalysisModeTest> {
            model("codegen/box", targetBackend = TargetBackend.JVM, skipIgnored = true)
        }

        testClass<AbstractKapt3BuilderModeBytecodeShapeTest> {
            model("codegen/kapt", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractIrBlackBoxCodegenTest>("IrOnlyBoxCodegenTestGenerated") {
            model("ir/box", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractBlackBoxInlineCodegenTest>("BlackBoxInlineCodegenTestGenerated") {
            model("codegen/boxInline")
        }

        testClass<AbstractCompileKotlinAgainstInlineKotlinTest>("CompileKotlinAgainstInlineKotlinTestGenerated") {
            model("codegen/boxInline")
        }

        testClass<AbstractBlackBoxAgainstJavaCodegenTest> {
            model("codegen/boxAgainstJava")
        }

        testClass<AbstractScriptCodegenTest> {
            model("codegen/script", extension = "kts")
        }

        testClass<AbstractBytecodeTextTest> {
            model("codegen/bytecodeText")
        }

        testClass<AbstractIrTextTestCase> {
            model("ir/irText")
        }

        testClass<AbstractIrCfgTestCase> {
            model("ir/irCfg")
        }

        testClass<AbstractIrSourceRangesTestCase> {
            model("ir/sourceRanges")
        }

        testClass<AbstractBytecodeListingTest> {
            model("codegen/bytecodeListing")
        }

        testClass<AbstractTopLevelMembersInvocationTest> {
            model("codegen/topLevelMemberInvocation", extension = null, recursive = false)
        }

        testClass<AbstractCheckLocalVariablesTableTest> {
            model("checkLocalVariablesTable")
        }

        testClass<AbstractWriteFlagsTest> {
            model("writeFlags")
        }

        testClass<AbstractDefaultArgumentsReflectionTest> {
            model("codegen/defaultArguments/reflection")
        }

        testClass<AbstractDumpDeclarationsTest> {
            model("codegen/dumpDeclarations")
        }

        testClass<AbstractLoadJavaTest> {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            model("loadJava/compiledJavaAndKotlin", extension = "txt", testMethod = "doTestCompiledJavaAndKotlin")
            model("loadJava/compiledJavaIncludeObjectMethods", extension = "java", testMethod = "doTestCompiledJavaIncludeObjectMethods")
            model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            model("loadJava/compiledKotlinWithStdlib", testMethod = "doTestCompiledKotlinWithStdlib")
            model("loadJava/javaAgainstKotlin", extension = "txt", testMethod = "doTestJavaAgainstKotlin")
            model("loadJava/kotlinAgainstCompiledJavaWithKotlin", extension = "kt", testMethod = "doTestKotlinAgainstCompiledJavaWithKotlin", recursive = false)
            model("loadJava/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass<AbstractLoadKotlinWithTypeTableTest> {
            model("loadJava/compiledKotlin")
        }

        testClass<AbstractJvmRuntimeDescriptorLoaderTest> {
            model("loadJava/compiledKotlin")
            model("loadJava/compiledJava", extension = "java", excludeDirs = listOf("sam", "kotlinSignature/propagation"))
        }

        testClass<AbstractLoadJavaWithFastClassReadingTest> {
            model("loadJava/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
        }

        testClass<AbstractCompileJavaAgainstKotlinTest> {
            model("compileJavaAgainstKotlin", testClassName = "WithoutJavac", testMethod = "doTestWithoutJavac")
            model("compileJavaAgainstKotlin", testClassName = "WithJavac", testMethod = "doTestWithJavac")
        }

        testClass<AbstractCompileKotlinAgainstJavaTest> {
            model("compileKotlinAgainstJava")
        }

        testClass<AbstractCompileKotlinAgainstKotlinTest> {
            model("compileKotlinAgainstKotlin")
        }

        testClass<AbstractDescriptorRendererTest> {
            model("renderer")
        }

        testClass<AbstractFunctionDescriptorInExpressionRendererTest> {
            model("renderFunctionDescriptorInExpression")
        }

        testClass<AbstractModuleXmlParserTest> {
            model("modules.xml", extension = "xml")
        }

        testClass<AbstractWriteSignatureTest> {
            model("writeSignature")
        }

        testClass<AbstractCliTest> {
            model("cli/jvm", extension = "args", testMethod = "doJvmTest", recursive = false)
            model("cli/js", extension = "args", testMethod = "doJsTest", recursive = false)
            model("cli/js-dce", extension = "args", testMethod = "doJsDceTest", recursive = false)
        }

        testClass<AbstractReplInterpreterTest> {
            model("repl", extension = "repl")
        }

        testClass<AbstractAntTaskTest> {
            model("integration/ant/jvm", extension = null, recursive = false, excludeParentDirs = true)
        }

        testClass<AbstractControlFlowTest> {
            model("cfg")
        }

        testClass<AbstractDataFlowTest> {
            model("cfg-variables")
        }

        testClass<AbstractPseudoValueTest> {
            model("cfg")
            model("cfg-variables")
        }

        testClass<AbstractAnnotationParameterTest> {
            model("resolveAnnotations/parameters")
        }

        testClass<AbstractCompileTimeConstantEvaluatorTest> {
            model("evaluate/constant", testMethod = "doConstantTest")
            model("evaluate/isPure", testMethod = "doIsPureTest")
            model("evaluate/usesVariableAsConstant", testMethod = "doUsesVariableAsConstantTest")
        }

        testClass<AbstractCompilerLightClassTest> {
            model("asJava/lightClasses", excludeDirs = listOf("local", "ideRegression"))
        }

        testClass<AbstractTypeBindingTest> {
            model("type/binding")
        }

        testClass<AbstractLineNumberTest> {
            model("lineNumber", recursive = false)
            model("lineNumber/custom", testMethod = "doTestCustom")
        }

        testClass<AbstractLocalClassProtoTest> {
            model("serialization/local")
        }

        testClass<AbstractKDocLexerTest> {
            model("kdoc/lexer")
        }
    }

    testGroup("compiler/tests-ir-jvm/tests", "compiler/testData") {
        testClass<AbstractIrBlackBoxCodegenTest> {
            model("codegen/box", targetBackend = TargetBackend.JVM)
        }

        testClass<AbstractIrBlackBoxInlineCodegenTest> {
            model("codegen/boxInline")
        }

        testClass<AbstractIrCompileKotlinAgainstInlineKotlinTest> {
            model("codegen/boxInline")
        }
    }

    testGroup("compiler/tests-java8/tests", "compiler/testData") {
        testClass<AbstractBlackBoxCodegenTest>("BlackBoxWithJava8CodegenTestGenerated") {
            model("codegen/java8/box")
        }

        testClass<AbstractDiagnosticsWithFullJdkTest>("DiagnosticsWithJava8TestGenerated") {
            model("diagnostics/testsWithJava8")
        }

        testClass<AbstractForeignJava8AnnotationsTest> {
            model("foreignAnnotationsJava8/tests")
        }

        testClass<AbstractLoadJava8Test> {
            model("loadJava8/compiledJava", extension = "java", testMethod = "doTestCompiledJava")
            model("loadJava8/sourceJava", extension = "java", testMethod = "doTestSourceJava")
        }

        testClass<AbstractEnhancedSignaturesResolvedCallsTest> {
            model("resolvedCalls/enhancedSignatures")
        }

        testClass<AbstractJvm8RuntimeDescriptorLoaderTest> {
            model("loadJava8/compiledJava", extension = "java", excludeDirs = listOf("sam", "kotlinSignature/propagation"))
        }

        testClass<AbstractCompileKotlinAgainstKotlinTest> {
            model("codegen/java8/compileKotlinAgainstKotlin")
        }

        testClass<AbstractJava8WriteSignatureTest> {
            model("codegen/java8/writeSignature")
        }

        testClass<AbstractWriteFlagsTest> {
            model("codegen/java8/writeFlags")
        }

        testClass<AbstractBytecodeTextTest>("BytecodeTextJava8TestGenerated") {
            model("codegen/java8/bytecodeText")
        }
    }

    testGroup("idea/tests", "idea/testData") {
        testClass<AbstractAdditionalResolveDescriptorRendererTest> {
            model("resolve/additionalLazyResolve")
        }

        testClass<AbstractPartialBodyResolveTest> {
            model("resolve/partialBodyResolve")
        }

        testClass<AbstractPsiCheckerTest> {
            model("checker", recursive = false)
            model("checker/regression")
            model("checker/recovery")
            model("checker/rendering")
            model("checker/scripts", extension = "kts")
            model("checker/duplicateJvmSignature")
            model("checker/infos", testMethod = "doTestWithInfos")
            model("checker/diagnosticsMessage")
        }

        testClass<AbstractJavaAgainstKotlinSourceCheckerTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
            model("kotlinAndJavaChecker/javaWithKotlin")
        }

        testClass<AbstractJavaAgainstKotlinBinariesCheckerTest> {
            model("kotlinAndJavaChecker/javaAgainstKotlin")
        }

        testClass<AbstractPsiUnifierTest> {
            model("unifier")
        }

        testClass<AbstractCodeFragmentHighlightingTest> {
            model("checker/codeFragments", extension = "kt", recursive = false)
            model("checker/codeFragments/imports", testMethod = "doTestWithImport", extension = "kt")
        }

        testClass<AbstractCodeFragmentAutoImportTest> {
            model("quickfix.special/codeFragmentAutoImport", extension = "kt", recursive = false)
        }

        testClass<AbstractJsCheckerTest> {
            model("checker/js")
        }

        testClass<AbstractQuickFixTest> {
            model("quickfix", pattern = "^([\\w\\-_]+)\\.kt$", filenameStartsLowerCase = true)
        }

        testClass<AbstractGotoSuperTest> {
            model("navigation/gotoSuper", extension = "test")
        }

        testClass<AbstractGotoTypeDeclarationTest> {
            model("navigation/gotoTypeDeclaration", extension = "test")
        }

        testClass<AbstractGotoDeclarationTest> {
            model("navigation/gotoDeclaration", extension = "test")
        }

        testClass<AbstractParameterInfoTest> {
            model("parameterInfo", recursive = true, excludeDirs = listOf("withLib1/sharedLib", "withLib2/sharedLib", "withLib3/sharedLib"))
        }

        testClass<AbstractKotlinGotoTest> {
            model("navigation/gotoClass", testMethod = "doClassTest")
            model("navigation/gotoSymbol", testMethod = "doSymbolTest")
        }

        testClass<AbstractNavigateToLibrarySourceTest> {
            model("decompiler/navigation/usercode")
            model("decompiler/navigation/usercode", testClassName ="UsercodeWithJSModule", testMethod = "doWithJSModuleTest")
        }

        testClass<AbstractNavigateToDecompiledLibraryTest> {
            model("decompiler/navigation/usercode")
        }

        testClass<AbstractKotlinGotoImplementationTest> {
            model("navigation/implementations", recursive = false)
        }

        testClass<AbstractGotoTestOrCodeActionTest> {
            model("navigation/gotoTestOrCode", pattern = "^(.+)\\.main\\..+\$")
        }

        testClass<AbstractInheritorsSearchTest> {
            model("search/inheritance")
        }

        testClass<AbstractAnnotatedMembersSearchTest> {
            model("search/annotations")
        }

        testClass<AbstractQuickFixMultiFileTest> {
            model("quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""", testMethod = "doTestWithExtraFile")
        }

        testClass<AbstractKotlinTypeAliasByExpansionShortNameIndexTest> {
            model("typealiasExpansionIndex")
        }

        testClass<AbstractHighlightingTest> {
            model("highlighter")
        }

        testClass<AbstractUsageHighlightingTest> {
            model("usageHighlighter")
        }

        testClass<AbstractKotlinFoldingTest> {
            model("folding/noCollapse")
            model("folding/checkCollapse", testMethod = "doSettingsFoldingTest")
        }

        testClass<AbstractSurroundWithTest> {
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
            model("codeInsight/surroundWith/withIfExpression", testMethod = "doTestWithSurroundWithIfExpression")
            model("codeInsight/surroundWith/withIfElseExpression", testMethod = "doTestWithSurroundWithIfElseExpression")
        }

        testClass<AbstractJoinLinesTest> {
            model("joinLines")
        }

        testClass<AbstractBreadcrumbsTest> {
            model("codeInsight/breadcrumbs")
        }

        testClass<AbstractIntentionTest> {
            model("intentions", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractIntentionTest2> {
            model("intentions/loopToCallChain", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractConcatenatedStringGeneratorTest> {
            model("concatenatedStringGenerator", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractInspectionTest> {
            model("intentions", pattern = "^(inspections\\.test)$", singleClass = true)
            model("inspections", pattern = "^(inspections\\.test)$", singleClass = true)
        }

        testClass<AbstractLocalInspectionTest> {
            model("inspectionsLocal", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractHierarchyTest> {
            model("hierarchy/class/type", extension = null, recursive = false, testMethod = "doTypeClassHierarchyTest")
            model("hierarchy/class/super", extension = null, recursive = false, testMethod = "doSuperClassHierarchyTest")
            model("hierarchy/class/sub", extension = null, recursive = false, testMethod = "doSubClassHierarchyTest")
            model("hierarchy/calls/callers", extension = null, recursive = false, testMethod = "doCallerHierarchyTest")
            model("hierarchy/calls/callersJava", extension = null, recursive = false, testMethod = "doCallerJavaHierarchyTest")
            model("hierarchy/calls/callees", extension = null, recursive = false, testMethod = "doCalleeHierarchyTest")
            model("hierarchy/overrides", extension = null, recursive = false, testMethod = "doOverrideHierarchyTest")
        }

        testClass<AbstractHierarchyWithLibTest> {
            model("hierarchy/withLib", extension = null, recursive = false)
        }

        testClass<AbstractMoveStatementTest> {
            model("codeInsight/moveUpDown/classBodyDeclarations", testMethod = "doTestClassBodyDeclaration")
            model("codeInsight/moveUpDown/closingBraces", testMethod = "doTestExpression")
            model("codeInsight/moveUpDown/expressions", testMethod = "doTestExpression")
            model("codeInsight/moveUpDown/parametersAndArguments", testMethod = "doTestExpression")
        }

        testClass<AbstractMoveLeftRightTest> {
            model("codeInsight/moveLeftRight")
        }

        testClass<AbstractInlineTest> {
            model("refactoring/inline", pattern = "^(\\w+)\\.kt$")
        }

        testClass<AbstractUnwrapRemoveTest> {
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

        testClass<AbstractExpressionTypeTest> {
            model("codeInsight/expressionType")
        }

        testClass<AbstractBackspaceHandlerTest> {
            model("editor/backspaceHandler")
        }

        testClass<AbstractMultiLineStringIndentTest> {
            model("editor/enterHandler/multilineString")
        }

        testClass<AbstractQuickDocProviderTest> {
            model("editor/quickDoc", pattern = """^([^_]+)\.[^\.]*$""")
        }

        testClass<AbstractSafeDeleteTest> {
            model("refactoring/safeDelete/deleteClass/kotlinClass", testMethod = "doClassTest")
            model("refactoring/safeDelete/deleteClass/kotlinClassWithJava", testMethod = "doClassTestWithJava")
            model("refactoring/safeDelete/deleteClass/javaClassWithKotlin", extension = "java", testMethod = "doJavaClassTest")
            model("refactoring/safeDelete/deleteObject/kotlinObject", testMethod = "doObjectTest")
            model("refactoring/safeDelete/deleteFunction/kotlinFunction", testMethod = "doFunctionTest")
            model("refactoring/safeDelete/deleteFunction/kotlinFunctionWithJava", testMethod = "doFunctionTestWithJava")
            model("refactoring/safeDelete/deleteFunction/javaFunctionWithKotlin", testMethod = "doJavaMethodTest")
            model("refactoring/safeDelete/deleteProperty/kotlinProperty", testMethod = "doPropertyTest")
            model("refactoring/safeDelete/deleteProperty/kotlinPropertyWithJava", testMethod = "doPropertyTestWithJava")
            model("refactoring/safeDelete/deleteProperty/javaPropertyWithKotlin", testMethod = "doJavaPropertyTest")
            model("refactoring/safeDelete/deleteTypeAlias/kotlinTypeAlias", testMethod = "doTypeAliasTest")
            model("refactoring/safeDelete/deleteTypeParameter/kotlinTypeParameter", testMethod = "doTypeParameterTest")
            model("refactoring/safeDelete/deleteTypeParameter/kotlinTypeParameterWithJava", testMethod = "doTypeParameterTestWithJava")
            model("refactoring/safeDelete/deleteValueParameter/kotlinValueParameter", testMethod = "doValueParameterTest")
            model("refactoring/safeDelete/deleteValueParameter/kotlinValueParameterWithJava", testMethod = "doValueParameterTestWithJava")
        }

        testClass<AbstractReferenceResolveTest> {
            model("resolve/references", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractReferenceResolveInJavaTest> {
            model("resolve/referenceInJava/binaryAndSource", extension = "java")
            model("resolve/referenceInJava/sourceOnly", extension = "java")
        }

        testClass<AbstractReferenceToCompiledKotlinResolveInJavaTest> {
            model("resolve/referenceInJava/binaryAndSource", extension = "java")
        }

        testClass<AbstractReferenceResolveWithLibTest> {
            model("resolve/referenceWithLib", recursive = false)
        }

        testClass<AbstractReferenceResolveInLibrarySourcesTest> {
            model("resolve/referenceInLib", recursive = false)
        }

        testClass<AbstractReferenceToJavaWithWrongFileStructureTest> {
            model("resolve/referenceToJavaWithWrongFileStructure", recursive = false)
        }

        testClass<AbstractFindUsagesTest> {
            model("findUsages/kotlin", pattern = """^(.+)\.0\.kt$""")
            model("findUsages/java", pattern = """^(.+)\.0\.java$""")
            model("findUsages/propertyFiles", pattern = """^(.+)\.0\.properties$""")
        }

        testClass<AbstractKotlinFindUsagesWithLibraryTest> {
            model("findUsages/libraryUsages", pattern = """^(.+)\.0\.kt$""")
        }

        testClass<AbstractMoveTest> {
            model("refactoring/move", extension = "test", singleClass = true)
        }

        testClass<AbstractCopyTest> {
            model("refactoring/copy", extension = "test", singleClass = true)
        }

        testClass<AbstractMultiModuleMoveTest> {
            model("refactoring/moveMultiModule", extension = "test", singleClass = true)
        }

        testClass<AbstractMultiModuleCopyTest> {
            model("refactoring/copyMultiModule", extension = "test", singleClass = true)
        }

        testClass<AbstractMultiFileIntentionTest> {
            model("multiFileIntentions", extension = "test", singleClass = true, filenameStartsLowerCase = true)
        }

        testClass<AbstractMultiFileInspectionTest> {
            model("multiFileInspections", extension = "test", singleClass = true)
        }

        testClass<AbstractGradleConfigureProjectByChangingFileTest> {
            model("configuration/gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestGradle")
            model("configuration/gsk", pattern = """(\w+)_before\.gradle.kts$""", testMethod = "doTestGradle")
        }

        testClass<AbstractFormatterTest> {
            model("formatter", pattern = """^([^\.]+)\.after\.kt.*$""")
            model("formatter", pattern = """^([^\.]+)\.after\.inv\.kt.*$""",
                  testMethod = "doTestInverted", testClassName = "FormatterInverted")
        }

        testClass<AbstractTypingIndentationTestBase> {
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.kt.*$""", testMethod = "doNewlineTest",
                  testClassName = "DirectSettings")
            model("indentationOnNewline", pattern = """^([^\.]+)\.after\.inv\.kt.*$""", testMethod = "doNewlineTestWithInvert",
                  testClassName = "InvertedSettings")
        }

        testClass<AbstractDiagnosticMessageTest> {
            model("diagnosticMessage", recursive = false)
        }

        testClass<AbstractDiagnosticMessageJsTest> {
            model("diagnosticMessage/js", recursive = false, targetBackend = TargetBackend.JS)
        }

        testClass<AbstractRenameTest> {
            model("refactoring/rename", extension = "test", singleClass = true)
        }

        testClass<AbstractMultiModuleRenameTest> {
            model("refactoring/renameMultiModule", extension = "test", singleClass = true)
        }

        testClass<AbstractOutOfBlockModificationTest> {
            model("codeInsight/outOfBlock")
        }

        testClass<AbstractDataFlowValueRenderingTest> {
            model("dataFlowValueRendering")
        }

        testClass<AbstractJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/conversion", pattern = """^([^\.]+)\.java$""")
        }

        testClass<AbstractTextJavaToKotlinCopyPasteConversionTest> {
            model("copyPaste/plainTextConversion", pattern = """^([^\.]+)\.txt$""")
        }

        testClass<AbstractLiteralTextToKotlinCopyPasteTest> {
            model("copyPaste/plainTextLiteral", pattern = """^([^\.]+)\.txt$""")
        }

        testClass<AbstractLiteralKotlinToKotlinCopyPasteTest> {
            model("copyPaste/literal", pattern = """^([^\.]+)\.kt$""")
        }

        testClass<AbstractInsertImportOnPasteTest> {
            model("copyPaste/imports", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doTestCopy", testClassName = "Copy", recursive = false)
            model("copyPaste/imports", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doTestCut", testClassName = "Cut", recursive = false)
        }

        testClass<AbstractMoveOnCutPasteTest> {
            model("copyPaste/moveDeclarations", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doTest")
        }

        testClass<AbstractHighlightExitPointsTest> {
            model("exitPoints")
        }

        testClass<AbstractLineMarkersTest> {
            model("codeInsight/lineMarker")
        }

        testClass<AbstractShortenRefsTest> {
            model("shortenRefs", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }
        testClass<AbstractAddImportTest> {
            model("addImport", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractSmartSelectionTest> {
            model("smartSelection", testMethod = "doTestSmartSelection", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractKotlinFileStructureTest> {
            model("structureView/fileStructure", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractExpressionSelectionTest> {
            model("expressionSelection", testMethod = "doTestExpressionSelection", pattern = KT_WITHOUT_DOTS_IN_NAME)
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

        testClass<AbstractClsStubBuilderTest> {
            model("decompiler/stubBuilder", extension = null, recursive = false)
        }

        testClass<AbstractJvmOptimizeImportsTest> {
            model("editor/optimizeImports/jvm", pattern = KT_WITHOUT_DOTS_IN_NAME)
            model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }
        testClass<AbstractJsOptimizeImportsTest> {
            model("editor/optimizeImports/js", pattern = KT_WITHOUT_DOTS_IN_NAME)
            model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractPositionManagerTest> {
            model("debugger/positionManager", recursive = false, extension = "kt", testClassName = "SingleFile")
            model("debugger/positionManager", recursive = false, extension = null, testClassName = "MultiFile")
        }

        testClass<AbstractKotlinExceptionFilterTest> {
            model("debugger/exceptionFilter", pattern = """^([^\.]+)$""", recursive = false)
        }

        testClass<AbstractSmartStepIntoTest> {
            model("debugger/smartStepInto")
        }

        testClass<AbstractBeforeExtractFunctionInsertionTest> {
            model("debugger/insertBeforeExtractFunction", extension = "kt")
        }

        testClass<AbstractKotlinSteppingTest> {
            model("debugger/tinyApp/src/stepping/stepIntoAndSmartStepInto", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepIntoTest", testClassName = "StepInto")
            model("debugger/tinyApp/src/stepping/stepIntoAndSmartStepInto", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doSmartStepIntoTest", testClassName = "SmartStepInto")
            model("debugger/tinyApp/src/stepping/stepInto", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepIntoTest", testClassName = "StepIntoOnly")
            model("debugger/tinyApp/src/stepping/stepOut", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOutTest")
            model("debugger/tinyApp/src/stepping/stepOver", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOverTest")
            model("debugger/tinyApp/src/stepping/stepOverForce", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOverForceTest")
            model("debugger/tinyApp/src/stepping/filters", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepIntoTest")
            model("debugger/tinyApp/src/stepping/custom", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doCustomTest")
        }

        testClass<AbstractKotlinEvaluateExpressionTest> {
            model("debugger/tinyApp/src/evaluate/singleBreakpoint", testMethod = "doSingleBreakpointTest")
            model("debugger/tinyApp/src/evaluate/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest")
        }

        testClass<AbstractStubBuilderTest> {
            model("stubs", extension = "kt")
        }

        testClass<AbstractMultiFileHighlightingTest> {
            model("multiFileHighlighting", recursive = false)
        }

        testClass<AbstractExtractionTest> {
            model("refactoring/introduceVariable", pattern = KT_OR_KTS, testMethod = "doIntroduceVariableTest")
            model("refactoring/extractFunction", pattern = KT_OR_KTS, testMethod = "doExtractFunctionTest")
            model("refactoring/introduceProperty", pattern = KT_OR_KTS, testMethod = "doIntroducePropertyTest")
            model("refactoring/introduceParameter", pattern = KT_OR_KTS, testMethod = "doIntroduceSimpleParameterTest")
            model("refactoring/introduceLambdaParameter", pattern = KT_OR_KTS, testMethod = "doIntroduceLambdaParameterTest")
            model("refactoring/introduceJavaParameter", extension = "java", testMethod = "doIntroduceJavaParameterTest")
            model("refactoring/introduceTypeParameter", pattern = KT_OR_KTS, testMethod = "doIntroduceTypeParameterTest")
            model("refactoring/introduceTypeAlias", pattern = KT_OR_KTS, testMethod = "doIntroduceTypeAliasTest")
            model("refactoring/extractSuperclass", pattern = KT_OR_KTS, testMethod = "doExtractSuperclassTest")
            model("refactoring/extractInterface", pattern = KT_OR_KTS, testMethod = "doExtractInterfaceTest")
        }

        testClass<AbstractPullUpTest> {
            model("refactoring/pullUp/k2k", extension = "kt", singleClass = true, testClassName = "K2K", testMethod = "doKotlinTest")
            model("refactoring/pullUp/k2j", extension = "kt", singleClass = true, testClassName = "K2J", testMethod = "doKotlinTest")
            model("refactoring/pullUp/j2k", extension = "java", singleClass = true, testClassName = "J2K", testMethod = "doJavaTest")
        }

        testClass<AbstractPushDownTest> {
            model("refactoring/pushDown/k2k", extension = "kt", singleClass = true, testClassName = "K2K", testMethod = "doKotlinTest")
            model("refactoring/pushDown/k2j", extension = "kt", singleClass = true, testClassName = "K2J", testMethod = "doKotlinTest")
            model("refactoring/pushDown/j2k", extension = "java", singleClass = true, testClassName = "J2K", testMethod = "doJavaTest")
        }

        testClass<AbstractSelectExpressionForDebuggerTest> {
            model("debugger/selectExpression", recursive = false)
            model("debugger/selectExpression/disallowMethodCalls", testMethod = "doTestWoMethodCalls")
        }

        testClass<AbstractKotlinCoverageOutputFilesTest> {
            model("coverage/outputFiles")
        }

        testClass<AbstractBytecodeToolWindowTest> {
            model("internal/toolWindow", recursive = false, extension = null)
        }

        testClass<AbstractReferenceResolveTest>("org.jetbrains.kotlin.idea.kdoc.KdocResolveTestGenerated") {
            model("kdoc/resolve")
        }

        testClass<AbstractKDocHighlightingTest> {
            model("kdoc/highlighting")
        }

        testClass<AbstractKDocTypingTest> {
            model("kdoc/typing")
        }

        testClass<AbstractGenerateTestSupportMethodActionTest> {
            model("codeInsight/generate/testFrameworkSupport")
        }

        testClass<AbstractGenerateHashCodeAndEqualsActionTest> {
            model("codeInsight/generate/equalsWithHashCode")
        }

        testClass<AbstractCodeInsightActionTest> {
            model("codeInsight/generate/secondaryConstructors")
        }

        testClass<AbstractGenerateToStringActionTest> {
            model("codeInsight/generate/toString")
        }

        testClass<AbstractIdeReplCompletionTest> {
            model("repl/completion")
        }

        testClass<AbstractPostfixTemplateProviderTest> {
            model("codeInsight/postfix")
        }

        testClass<AbstractScriptConfigurationHighlightingTest> {
            model("script/definition/highlighting", extension = null, recursive = false)
        }

        testClass<AbstractScriptConfigurationNavigationTest> {
            model("script/definition/navigation", extension = null, recursive = false)
        }

        testClass<AbstractNameSuggestionProviderTest> {
            model("refactoring/nameSuggestionProvider")
        }

        testClass<AbstractSlicerTest> {
            model("slicer", singleClass = true)
        }
    }

    testGroup("idea/idea-maven/test", "idea/idea-maven/testData") {
        testClass<AbstractMavenConfigureProjectByChangingFileTest> {
            model("configurator/jvm", extension = null, recursive = false, testMethod = "doTestWithMaven")
            model("configurator/js", extension = null, recursive = false, testMethod = "doTestWithJSMaven")
        }

        testClass<AbstractKotlinMavenInspectionTest> {
            model("maven-inspections", pattern = "^([\\w\\-]+).xml$", singleClass = true)
        }
    }

    testGroup("idea/tests", "compiler/testData") {
        testClass<AbstractResolveByStubTest> {
            model("loadJava/compiledKotlin")
        }
    }

    testGroup("idea/tests", "compiler/testData") {
        testClass<AbstractIdeLightClassTest> {
            model("asJava/lightClasses", excludeDirs = listOf("delegation"), pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractIdeCompiledLightClassTest> {
            model("asJava/lightClasses", excludeDirs = listOf("local", "compilationErrors", "ideRegression"), pattern = KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    testGroup("idea/idea-completion/tests", "idea/idea-completion/testData") {
        testClass<AbstractCompiledKotlinInJavaCompletionTest> {
            model("injava", extension = "java", recursive = false)
        }

        testClass<AbstractKotlinSourceInJavaCompletionTest> {
            model("injava", extension = "java", recursive = false)
        }

        testClass<AbstractKotlinStdLibInJavaCompletionTest> {
            model("injava/stdlib", extension = "java", recursive = false)
        }

        testClass<AbstractBasicCompletionWeigherTest> {
            model("weighers/basic", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractSmartCompletionWeigherTest> {
            model("weighers/smart", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractJSBasicCompletionTest> {
            model("basic/common")
            model("basic/js")
        }

        testClass<AbstractJvmBasicCompletionTest> {
            model("basic/common")
            model("basic/java")
        }

        testClass<AbstractJvmSmartCompletionTest> {
            model("smart")
        }

        testClass<AbstractKeywordCompletionTest> {
            model("keywords", recursive = false)
        }

        testClass<AbstractJvmWithLibBasicCompletionTest> {
            model("basic/withLib", recursive = false)
        }

        testClass<AbstractBasicCompletionHandlerTest> {
            model("handlers/basic", pattern = KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractSmartCompletionHandlerTest> {
            model("handlers/smart")
        }

        testClass<AbstractKeywordCompletionHandlerTest> {
            model("handlers/keywords")
        }

        testClass<AbstractCompletionCharFilterTest> {
            model("handlers/charFilter")
        }

        testClass<AbstractMultiFileJvmBasicCompletionTest> {
            model("basic/multifile", extension = null, recursive = false)
        }

        testClass<AbstractMultiFileSmartCompletionTest> {
            model("smartMultiFile", extension = null, recursive = false)
        }

        testClass<AbstractJvmBasicCompletionTest>("org.jetbrains.kotlin.idea.completion.test.KDocCompletionTestGenerated") {
            model("kdoc")
        }

        testClass<AbstractJava8BasicCompletionTest> {
            model("basic/java8")
        }

        testClass<AbstractCompletionIncrementalResolveTest>() {
            model("incrementalResolve")
        }
    }

    //TODO: move these tests into idea-completion module
    testGroup("idea/tests", "idea/idea-completion/testData") {
        testClass<AbstractCodeFragmentCompletionHandlerTest> {
            model("handlers/runtimeCast")
        }

        testClass<AbstractCodeFragmentCompletionTest> {
            model("basic/codeFragments", extension = "kt")
        }
    }

    testGroup("j2k/tests", "j2k/testData") {
        testClass<AbstractJavaToKotlinConverterSingleFileTest> {
            model("fileOrElement", extension = "java")
        }
    }
    testGroup("j2k/tests", "j2k/testData") {
        testClass<AbstractJavaToKotlinConverterMultiFileTest> {
            model("multiFile", extension = null, recursive = false)
        }
    }
    testGroup("j2k/tests", "j2k/testData") {
        testClass<AbstractJavaToKotlinConverterForWebDemoTest> {
            model("fileOrElement", extension = "java")
        }
    }

    testGroup("jps-plugin/jps-tests/test", "jps-plugin/testData") {
        testClass<AbstractIncrementalJpsTest> {
            model("incremental/multiModule", extension = null, excludeParentDirs = true)
            model("incremental/pureKotlin", extension = null, recursive = false)
            model("incremental/withJava", extension = null, excludeParentDirs = true)
            model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true)
            model("incremental/classHierarchyAffected", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractLookupTrackerTest> {
            model("incremental/lookupTracker", extension = null, recursive = false)
        }

        testClass(AbstractIncrementalLazyCachesTest::class.java) {
            model("incremental/lazyKotlinCaches", extension = null, excludeParentDirs = true)
            model("incremental/changeIncrementalOption", extension = null, excludeParentDirs = true)
        }

        testClass(AbstractIncrementalCacheVersionChangedTest::class.java) {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }

        testClass<AbstractDataContainerVersionChangedTest> {
            model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("jps-plugin/jps-tests/test", "jps-plugin/testData") {
        testClass<AbstractProtoComparisonTest> {
            model("comparison/classSignatureChange", extension = null, excludeParentDirs = true)
            model("comparison/classPrivateOnlyChange", extension = null, excludeParentDirs = true)
            model("comparison/classMembersOnlyChanged", extension = null, excludeParentDirs = true)
            model("comparison/packageMembers", extension = null, excludeParentDirs = true)
            model("comparison/unchanged", extension = null, excludeParentDirs = true)
        }
    }

    testGroup("plugins/plugins-tests/tests",  "plugins/android-extensions/android-extensions-compiler/testData") {
        testClass<AbstractAndroidSyntheticPropertyDescriptorTest> {
            model("descriptors", recursive = false, extension = null)
        }

        testClass<AbstractAndroidBoxTest> {
            model("codegen/android", recursive = false, extension = null, testMethod = "doCompileAgainstAndroidSdkTest")
            model("codegen/android", recursive = false, extension = null, testMethod = "doFakeInvocationTest", testClassName = "Invoke")
        }

        testClass<AbstractAndroidBytecodeShapeTest> {
            model("codegen/bytecodeShape", recursive = false, extension = null)
        }

        testClass<AbstractParcelBytecodeListingTest> {
            model("parcel/codegen")
        }
    }

    testGroup("plugins/plugins-tests/tests", "plugins/annotation-collector/testData") {
        testClass<AbstractAnnotationProcessorBoxTest> {
            model("collectToFile", recursive = false, extension = null)
        }
    }

    testGroup("plugins/kapt3/test", "plugins/kapt3/testData") {
        testClass<AbstractClassFileToSourceStubConverterTest> {
            model("converter")
        }

        testClass<AbstractKotlinKaptContextTest> {
            model("kotlinRunner")
        }
    }

    testGroup("plugins/plugins-tests/tests", "plugins/allopen/allopen-cli/testData") {
        testClass<AbstractBytecodeListingTestForAllOpen> {
            model("bytecodeListing", extension = "kt")
        }
    }

    testGroup("plugins/plugins-tests/tests", "plugins/noarg/noarg-cli/testData") {
        testClass<AbstractBytecodeListingTestForNoArg> {
            model("bytecodeListing", extension = "kt")
        }

        testClass<AbstractBlackBoxCodegenTestForNoArg> {
            model("box", targetBackend = TargetBackend.JVM)
        }
    }

    testGroup("plugins/plugins-tests/tests", "plugins/sam-with-receiver/sam-with-receiver-cli/testData") {
        testClass<AbstractSamWithReceiverTest> {
            model("diagnostics")
        }
        testClass<AbstractSamWithReceiverScriptTest> {
            model("script", extension = "kts")
        }
    }

    testGroup("plugins/android-extensions/android-extensions-idea/tests", "plugins/android-extensions/android-extensions-idea/testData") {
        testClass<AbstractAndroidCompletionTest> {
            model("android/completion", recursive = false, extension = null)
        }

        testClass<AbstractAndroidGotoTest> {
            model("android/goto", recursive = false, extension = null)
        }

        testClass<AbstractAndroidRenameTest> {
            model("android/rename", recursive = false, extension = null)
        }

        testClass<AbstractAndroidLayoutRenameTest> {
            model("android/renameLayout", recursive = false, extension = null)
        }

        testClass<AbstractAndroidFindUsagesTest> {
            model("android/findUsages", recursive = false, extension = null)
        }

        testClass<AbstractAndroidUsageHighlightingTest> {
            model("android/usageHighlighting", recursive = false, extension = null)
        }

        testClass<AbstractAndroidExtractionTest> {
            model("android/extraction", recursive = false, extension = null)
        }

        testClass<AbstractParcelCheckerTest> {
            model("android/parcel/checker", excludeParentDirs = true)
        }
    }

    testGroup("idea/idea-android/tests", "idea/testData") {
        testClass<AbstractConfigureProjectTest> {
            model("configuration/android-gradle", pattern = """(\w+)_before\.gradle$""", testMethod = "doTestAndroidGradle")
            model("configuration/android-gsk", pattern = """(\w+)_before\.gradle.kts$""", testMethod = "doTestAndroidGradle")
        }

        testClass<AbstractAndroidIntentionTest> {
            model("android/intention", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractAndroidResourceIntentionTest> {
            model("android/resourceIntention", extension = "test", singleClass = true)
        }

        testClass<AbstractAndroidQuickFixMultiFileTest> {
            model("android/quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""", testMethod = "doTestWithExtraFile")
        }

        testClass<AbstractKotlinLintTest> {
            model("android/lint", excludeParentDirs = true)
        }

        testClass<AbstractAndroidLintQuickfixTest> {
            model("android/lintQuickfix", pattern = "^([\\w\\-_]+)\\.kt$")
        }

        testClass<AbstractAndroidResourceFoldingTest> {
            model("android/folding")
        }

        testClass<AbstractAndroidGutterIconTest> {
            model("android/gutterIcon")
        }
    }

    testGroup("plugins/plugins-tests/tests", "plugins/android-extensions/android-extensions-jps/testData") {
        testClass<AbstractAndroidJpsTestCase> {
            model("android", recursive = false, extension = null)
        }
    }

    // TODO: repair these tests
    //generateTestDataForReservedWords()

    testGroup("js/js.tests/test", "js/js.translator/testData") {
        testClass<AbstractBoxJsTest> {
            model("box/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractSourceMapGenerationSmokeTest> {
            model("sourcemap/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractOutputPrefixPostfixTest> {
            model("outputPrefixPostfix/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractDceTest> {
            model("dce/", pattern = "(.+)\\.js", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractJsLineNumberTest> {
            model("lineNumbers/", pattern = "^([^_](.+))\\.kt$", targetBackend = TargetBackend.JS)
        }
    }

    testGroup("js/js.tests/test", "compiler/testData") {
        testClass<AbstractJsCodegenBoxTest> {
            model("codegen/box", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractNonLocalReturnsTest> {
            model("codegen/boxInline/nonLocalReturns/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractPropertyAccessorsInlineTests> {
            model("codegen/boxInline/property/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractNoInlineTests> {
            model("codegen/boxInline/noInline/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractCallableReferenceInlineTests> {
            model("codegen/boxInline/callableReference/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractEnumValuesInlineTests> {
            model("codegen/boxInline/enum/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractInlineDefaultValuesTests> {
            model("codegen/boxInline/defaultValues/", targetBackend = TargetBackend.JS)
        }

        testClass<AbstractJsTypedArraysBoxTest> {
            model("codegen/box/arrays", targetBackend = TargetBackend.JS)
        }
    }
}

class TestGroup(val testsRoot: String, val testDataRoot: String) {
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
                filenameStartsLowerCase: Boolean? = null,
                skipIgnored: Boolean = false
        ) {
            val rootFile = File(testDataRoot + "/" + relativeRootPath)
            val compiledPattern = Pattern.compile(pattern)
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            testModels.add(
                    if (singleClass) {
                        if (excludeDirs.isNotEmpty()) error("excludeDirs is unsupported for SingleClassTestModel yet")
                        SingleClassTestModel(rootFile, compiledPattern, filenameStartsLowerCase, testMethod, className, targetBackend,
                                             skipIgnored)
                    }
                    else {
                        SimpleTestClassModel(rootFile, recursive, excludeParentDirs,
                                             compiledPattern, filenameStartsLowerCase, testMethod, className,
                                             targetBackend, excludeDirs, skipIgnored)
                    }
            )
        }
    }

}

fun testGroup(testsRoot: String, testDataRoot: String, init: TestGroup.() -> Unit) {
    TestGroup(testsRoot, testDataRoot).init()
}

fun getDefaultSuiteTestClass(baseTestClass:Class<*>): String {
    val baseName = baseTestClass.simpleName
    if (!baseName.startsWith("Abstract")) {
        throw IllegalArgumentException("Doesn't start with \"Abstract\": $baseName")
    }
    return baseName.substring("Abstract".length) + "Generated"
}
