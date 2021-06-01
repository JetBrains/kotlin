/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.idea

import org.jetbrains.kotlin.AbstractDataFlowValueRenderingTest
import org.jetbrains.kotlin.addImport.AbstractAddImportTest
import org.jetbrains.kotlin.addImportAlias.AbstractAddImportAliasTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightClassLoadingTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightClassSanityTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightFacadeClassTest
import org.jetbrains.kotlin.asJava.classes.AbstractUltraLightScriptLoadingTest
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.copyright.AbstractUpdateKotlinCopyrightTest
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesTest
import org.jetbrains.kotlin.findUsages.AbstractFindUsagesWithDisableComponentSearchTest
import org.jetbrains.kotlin.findUsages.AbstractKotlinFindUsagesWithLibraryTest
import org.jetbrains.kotlin.findUsages.AbstractKotlinFindUsagesWithStdlibTest
import org.jetbrains.kotlin.formatter.AbstractFormatterTest
import org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_WITHOUT_FIR_PREFIX
import org.jetbrains.kotlin.idea.AbstractExpressionSelectionTest
import org.jetbrains.kotlin.idea.AbstractSmartSelectionTest
import org.jetbrains.kotlin.idea.actions.AbstractGotoTestOrCodeActionTest
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.codeInsight.*
import org.jetbrains.kotlin.idea.codeInsight.codevision.AbstractKotlinCodeVisionProviderTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractCodeInsightActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateHashCodeAndEqualsActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateTestSupportMethodActionTest
import org.jetbrains.kotlin.idea.codeInsight.generate.AbstractGenerateToStringActionTest
import org.jetbrains.kotlin.idea.codeInsight.hints.AbstractKotlinReferenceTypeHintsProviderTest
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
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralKotlinToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralTextToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.coverage.AbstractKotlinCoverageOutputFilesTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentAutoImportTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentCompletionHandlerTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentCompletionTest
import org.jetbrains.kotlin.idea.debugger.evaluate.AbstractCodeFragmentHighlightingTest
import org.jetbrains.kotlin.idea.debugger.test.*
import org.jetbrains.kotlin.idea.debugger.test.sequence.exec.AbstractSequenceTraceTestCase
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateJavaToLibrarySourceTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToDecompiledLibraryTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToLibrarySourceTest
import org.jetbrains.kotlin.idea.decompiler.navigation.AbstractNavigateToLibrarySourceTestWithJS
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractClsStubBuilderTest
import org.jetbrains.kotlin.idea.decompiler.stubBuilder.AbstractLoadJavaClsStubTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractCommonDecompiledTextTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJsDecompiledTextFromJsMetadataTest
import org.jetbrains.kotlin.idea.decompiler.textBuilder.AbstractJvmDecompiledTextTest
import org.jetbrains.kotlin.idea.editor.AbstractEnterAfterUnmatchedBraceHandlerTest
import org.jetbrains.kotlin.idea.editor.AbstractMultiLineStringIndentTest
import org.jetbrains.kotlin.idea.editor.backspaceHandler.AbstractBackspaceHandlerTest
import org.jetbrains.kotlin.idea.editor.quickDoc.AbstractQuickDocProviderTest
import org.jetbrains.kotlin.idea.filters.AbstractKotlinExceptionFilterTest
import org.jetbrains.kotlin.idea.fir.asJava.classes.AbstractFirClassLoadingTest
import org.jetbrains.kotlin.idea.fir.asJava.classes.AbstractFirLightClassTest
import org.jetbrains.kotlin.idea.fir.asJava.classes.AbstractFirLightFacadeClassTest
import org.jetbrains.kotlin.idea.fir.checkers.AbstractFirKotlinHighlightingPassTest
import org.jetbrains.kotlin.idea.fir.completion.AbstractFirKeywordCompletionTest
import org.jetbrains.kotlin.idea.fir.completion.AbstractHighLevelJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.fir.completion.AbstractHighLevelMultiFileJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.fir.completion.test.handlers.AbstractFirKeywordCompletionHandlerTest
import org.jetbrains.kotlin.idea.fir.completion.test.handlers.AbstractHighLevelBasicCompletionHandlerTest
import org.jetbrains.kotlin.idea.fir.completion.wheigher.AbstractHighLevelWeigherTest
import org.jetbrains.kotlin.idea.fir.findUsages.AbstractFindUsagesFirTest
import org.jetbrains.kotlin.idea.fir.findUsages.AbstractFindUsagesWithDisableComponentSearchFirTest
import org.jetbrains.kotlin.idea.fir.findUsages.AbstractKotlinFindUsagesWithLibraryFirTest
import org.jetbrains.kotlin.idea.fir.findUsages.AbstractKotlinFindUsagesWithStdlibFirTest
import org.jetbrains.kotlin.idea.fir.frontend.api.components.*
import org.jetbrains.kotlin.idea.fir.frontend.api.fir.AbstractResolveCallTest
import org.jetbrains.kotlin.idea.fir.frontend.api.scopes.AbstractFileScopeTest
import org.jetbrains.kotlin.idea.fir.frontend.api.scopes.AbstractMemberScopeByFqNameTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractMemoryLeakInSymbolsTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractSymbolByFqNameTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractSymbolByPsiTest
import org.jetbrains.kotlin.idea.fir.frontend.api.symbols.AbstractSymbolByReferenceTest
import org.jetbrains.kotlin.idea.fir.highlighter.AbstractFirHighlightingTest
import org.jetbrains.kotlin.idea.fir.inspections.AbstractFe10BindingIntentionTest
import org.jetbrains.kotlin.idea.fir.inspections.AbstractHLInspectionTest
import org.jetbrains.kotlin.idea.fir.inspections.AbstractHLLocalInspectionTest
import org.jetbrains.kotlin.idea.fir.intentions.AbstractHLIntentionTest
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.AbstractSessionsInvalidationTest
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.AbstractProjectWideOutOfBlockKotlinModificationTrackerTest
import org.jetbrains.kotlin.idea.fir.quickfix.AbstractHighLevelQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.fir.quickfix.AbstractHighLevelQuickFixTest
import org.jetbrains.kotlin.idea.fir.resolve.AbstractFirReferenceResolveTest
import org.jetbrains.kotlin.idea.fir.shortenRefs.AbstractFirShortenRefsTest
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest
import org.jetbrains.kotlin.idea.fir.inspections.AbstractFe10BindingIntentionTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyTest
import org.jetbrains.kotlin.idea.hierarchy.AbstractHierarchyWithLibTest
import org.jetbrains.kotlin.idea.highlighter.*
import org.jetbrains.kotlin.idea.imports.AbstractJsOptimizeImportsTest
import org.jetbrains.kotlin.idea.imports.AbstractJvmOptimizeImportsTest
import org.jetbrains.kotlin.idea.index.AbstractKotlinTypeAliasByExpansionShortNameIndexTest
import org.jetbrains.kotlin.idea.inspections.AbstractLocalInspectionTest
import org.jetbrains.kotlin.idea.inspections.AbstractMultiFileLocalInspectionTest
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
import org.jetbrains.kotlin.idea.navigationToolbar.AbstractKotlinNavBarTest
import org.jetbrains.kotlin.idea.parameterInfo.AbstractParameterInfoTest
import org.jetbrains.kotlin.idea.perf.*
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiFileTest
import org.jetbrains.kotlin.idea.quickfix.AbstractQuickFixMultiModuleTest
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
import org.jetbrains.kotlin.idea.refactoring.safeDelete.AbstractMultiModuleSafeDeleteTest
import org.jetbrains.kotlin.idea.refactoring.safeDelete.AbstractSafeDeleteTest
import org.jetbrains.kotlin.idea.repl.AbstractIdeReplCompletionTest
import org.jetbrains.kotlin.idea.resolve.*
import org.jetbrains.kotlin.idea.scratch.AbstractScratchLineMarkersTest
import org.jetbrains.kotlin.idea.scratch.AbstractScratchRunActionTest
import org.jetbrains.kotlin.idea.script.*
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerLeafGroupingTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerMultiplatformTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerNullnessGroupingTest
import org.jetbrains.kotlin.idea.slicer.AbstractSlicerTreeTest
import org.jetbrains.kotlin.idea.structureView.AbstractKotlinFileStructureTest
import org.jetbrains.kotlin.idea.stubs.AbstractMultiFileHighlightingTest
import org.jetbrains.kotlin.idea.stubs.AbstractResolveByStubTest
import org.jetbrains.kotlin.idea.stubs.AbstractStubBuilderTest
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.build.dependeciestxt.actualizeMppJpsIncTestCaseDirs
import org.jetbrains.kotlin.jps.incremental.AbstractJsProtoComparisonTest
import org.jetbrains.kotlin.jps.incremental.AbstractJvmProtoComparisonTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.nj2k.AbstractTextNewJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.nj2k.inference.common.AbstractCommonConstraintCollectorTest
import org.jetbrains.kotlin.nj2k.inference.mutability.AbstractMutabilityInferenceTest
import org.jetbrains.kotlin.nj2k.inference.nullability.AbstractNullabilityInferenceTest
import org.jetbrains.kotlin.pacelize.ide.test.AbstractParcelizeCheckerTest
import org.jetbrains.kotlin.pacelize.ide.test.AbstractParcelizeQuickFixTest
import org.jetbrains.kotlin.psi.patternMatching.AbstractPsiUnifierTest
import org.jetbrains.kotlin.search.AbstractAnnotatedMembersSearchTest
import org.jetbrains.kotlin.search.AbstractInheritorsSearchTest
import org.jetbrains.kotlin.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTestSpec
import org.jetbrains.kotlin.tools.projectWizard.cli.AbstractProjectTemplateBuildFileGenerationTest
import org.jetbrains.kotlin.tools.projectWizard.cli.AbstractYamlBuildFileGenerationTest
import org.jetbrains.kotlin.tools.projectWizard.wizard.AbstractProjectTemplateNewWizardProjectImportTest
import org.jetbrains.kotlin.tools.projectWizard.wizard.AbstractYamlNewWizardProjectImportTest
import org.jetbrains.kotlinx.serialization.idea.AbstractSerializationPluginIdeDiagnosticTest
import org.jetbrains.kotlinx.serialization.idea.AbstractSerializationQuickFixTest
import org.jetbrains.uast.test.kotlin.*
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.generators.generateTestGroupSuiteWithJUnit5

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuite(args) {
        val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

        testGroup("idea/jvm-debugger/jvm-debugger-test/test", "idea/jvm-debugger/jvm-debugger-test/testData") {
            testClass<AbstractKotlinSteppingTest> {
                model(
                    "stepping/stepIntoAndSmartStepInto",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doStepIntoTest",
                    testClassName = "StepInto"
                )
                model(
                    "stepping/stepIntoAndSmartStepInto",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doSmartStepIntoTest",
                    testClassName = "SmartStepInto"
                )
                model(
                    "stepping/stepInto",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doStepIntoTest",
                    testClassName = "StepIntoOnly"
                )
                model("stepping/stepOut", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOutTest")
                model("stepping/stepOver", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOverTest")
                model("stepping/filters", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepIntoTest")
                model("stepping/custom", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doCustomTest")
            }

            testClass<AbstractIrKotlinSteppingTest> {
                model(
                    "stepping/stepIntoAndSmartStepInto",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doStepIntoTest",
                    testClassName = "StepInto"
                )
                model(
                    "stepping/stepIntoAndSmartStepInto",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doSmartStepIntoTest",
                    testClassName = "SmartStepInto"
                )
                model(
                    "stepping/stepInto",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doStepIntoTest",
                    testClassName = "StepIntoOnly"
                )
                model("stepping/stepOut", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOutTest")
                model("stepping/stepOver", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepOverTest")
                model("stepping/filters", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doStepIntoTest")
                model("stepping/custom", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doCustomTest")
            }

            testClass<AbstractKotlinEvaluateExpressionTest> {
                model("evaluation/singleBreakpoint", testMethod = "doSingleBreakpointTest", targetBackend = TargetBackend.JVM_WITH_OLD_EVALUATOR)
                model("evaluation/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest", targetBackend = TargetBackend.JVM_WITH_OLD_EVALUATOR)
            }

            testClass<AbstractIrKotlinEvaluateExpressionTest> {
                model("evaluation/singleBreakpoint", testMethod = "doSingleBreakpointTest", targetBackend = TargetBackend.JVM_IR_WITH_OLD_EVALUATOR)
                model("evaluation/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest", targetBackend = TargetBackend.JVM_IR_WITH_OLD_EVALUATOR)
            }

            testClass<AbstractKotlinEvaluateExpressionWithIRFragmentCompilerTest> {
                model("evaluation/singleBreakpoint", testMethod = "doSingleBreakpointTest", targetBackend = TargetBackend.JVM_WITH_IR_EVALUATOR)
                model("evaluation/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest", targetBackend = TargetBackend.JVM_WITH_IR_EVALUATOR)
            }

            testClass<AbstractIrKotlinEvaluateExpressionWithIRFragmentCompilerTest> {
                model("evaluation/singleBreakpoint", testMethod = "doSingleBreakpointTest", targetBackend = TargetBackend.JVM_IR_WITH_IR_EVALUATOR)
                model("evaluation/multipleBreakpoints", testMethod = "doMultipleBreakpointsTest", targetBackend = TargetBackend.JVM_IR_WITH_IR_EVALUATOR)
            }

            testClass<AbstractSelectExpressionForDebuggerTest> {
                model("selectExpression", recursive = false)
                model("selectExpression/disallowMethodCalls", testMethod = "doTestWoMethodCalls")
            }

            testClass<AbstractPositionManagerTest> {
                model("positionManager", recursive = false, extension = "kt", testClassName = "SingleFile")
                model("positionManager", recursive = false, extension = null, testClassName = "MultiFile")
            }

            testClass<AbstractSmartStepIntoTest> {
                model("smartStepInto")
            }

            testClass<AbstractBreakpointApplicabilityTest> {
                model("breakpointApplicability")
            }

            testClass<AbstractFileRankingTest> {
                model("fileRanking")
            }

            testClass<AbstractAsyncStackTraceTest> {
                model("asyncStackTrace")
            }

            testClass<AbstractCoroutineDumpTest> {
                model("coroutines")
            }

            testClass<AbstractSequenceTraceTestCase> {
                // TODO: implement mapping logic for terminal operations
                model("sequence/streams/sequence", excludeDirs = listOf("terminal"))
            }

            testClass<AbstractContinuationStackTraceTest> {
                model("continuation")
            }

            testClass<AbstractXCoroutinesStackTraceTest> {
                model("xcoroutines")
            }
        }

        testGroup("idea/tests", "idea/testData") {
            testClass<AbstractAdditionalResolveDescriptorRendererTest> {
                model("resolve/additionalLazyResolve")
            }

            testClass<AbstractPartialBodyResolveTest> {
                model("resolve/partialBodyResolve")
            }

            testClass<AbstractResolveModeComparisonTest> {
                model("resolve/resolveModeComparison")
            }

            testClass<AbstractKotlinHighlightingPassTest> {
                model("checker", recursive = false, excludedPattern = excludedFirTestdataPattern)
                model("checker/regression", excludedPattern = excludedFirTestdataPattern)
                model("checker/recovery", excludedPattern = excludedFirTestdataPattern)
                model("checker/rendering", excludedPattern = excludedFirTestdataPattern)
                model("checker/scripts", extension = "kts", excludedPattern = excludedFirTestdataPattern)
                model("checker/duplicateJvmSignature", excludedPattern = excludedFirTestdataPattern)
                model("checker/infos", testMethod = "doTestWithInfos", excludedPattern = excludedFirTestdataPattern)
                model("checker/diagnosticsMessage", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractKotlinHighlightWolfPassTest> {
                model("checker/wolf", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractJavaAgainstKotlinSourceCheckerTest> {
                model("kotlinAndJavaChecker/javaAgainstKotlin")
                model("kotlinAndJavaChecker/javaWithKotlin")
            }

            testClass<AbstractJavaAgainstKotlinSourceCheckerWithoutUltraLightTest> {
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
                model("navigation/gotoSuper", extension = "test", recursive = false)
            }

            testClass<AbstractGotoTypeDeclarationTest> {
                model("navigation/gotoTypeDeclaration", extension = "test")
            }

            testClass<AbstractGotoDeclarationTest> {
                model("navigation/gotoDeclaration", extension = "test")
            }

            testClass<AbstractParameterInfoTest> {
                model(
                    "parameterInfo",
                    pattern = "^([\\w\\-_]+)\\.kt$", recursive = true,
                    excludeDirs = listOf("withLib1/sharedLib", "withLib2/sharedLib", "withLib3/sharedLib")
                )
            }

            testClass<AbstractKotlinGotoTest> {
                model("navigation/gotoClass", testMethod = "doClassTest")
                model("navigation/gotoSymbol", testMethod = "doSymbolTest")
            }

            testClass<AbstractNavigateToLibrarySourceTest>() {
                model("decompiler/navigation/usercode")
            }

            testClass<AbstractNavigateJavaToLibrarySourceTest>() {
                model("decompiler/navigation/userJavaCode", pattern = "^(.+)\\.java$")
            }

            testClass<AbstractNavigateToLibrarySourceTestWithJS>() {
                model("decompiler/navigation/usercode", testClassName = "UsercodeWithJSModule")
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

            testClass<AbstractDslHighlighterTest> {
                model("dslHighlighter")
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
                model("codeInsight/surroundWith/ifElseExpression", testMethod = "doTestWithIfElseExpressionSurrounder")
                model("codeInsight/surroundWith/ifElseExpressionBraces", testMethod = "doTestWithIfElseExpressionBracesSurrounder")
                model("codeInsight/surroundWith/not", testMethod = "doTestWithNotSurrounder")
                model("codeInsight/surroundWith/parentheses", testMethod = "doTestWithParenthesesSurrounder")
                model("codeInsight/surroundWith/stringTemplate", testMethod = "doTestWithStringTemplateSurrounder")
                model("codeInsight/surroundWith/when", testMethod = "doTestWithWhenSurrounder")
                model("codeInsight/surroundWith/tryCatch", testMethod = "doTestWithTryCatchSurrounder")
                model("codeInsight/surroundWith/tryCatchExpression", testMethod = "doTestWithTryCatchExpressionSurrounder")
                model("codeInsight/surroundWith/tryCatchFinally", testMethod = "doTestWithTryCatchFinallySurrounder")
                model("codeInsight/surroundWith/tryCatchFinallyExpression", testMethod = "doTestWithTryCatchFinallyExpressionSurrounder")
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
                model("intentions", pattern = "^([\\w\\-_]+)\\.(kt|kts)$")
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
                model("inspectionsLocal", pattern = "^(inspections\\.test)$", singleClass = true)
            }

            testClass<AbstractLocalInspectionTest> {
                model("inspectionsLocal", pattern = "^([\\w\\-_]+)\\.(kt|kts)$")
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
                model("codeInsight/moveUpDown/classBodyDeclarations", pattern = KT_OR_KTS, testMethod = "doTestClassBodyDeclaration")
                model("codeInsight/moveUpDown/closingBraces", testMethod = "doTestExpression")
                model("codeInsight/moveUpDown/expressions", pattern = KT_OR_KTS, testMethod = "doTestExpression")
                model("codeInsight/moveUpDown/line", testMethod = "doTestLine")
                model("codeInsight/moveUpDown/parametersAndArguments", testMethod = "doTestExpression")
                model("codeInsight/moveUpDown/trailingComma", testMethod = "doTestExpressionWithTrailingComma")
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
                model("codeInsight/unwrapAndRemove/unwrapFunctionParameter", testMethod = "doTestFunctionParameterUnwrapper")
            }

            testClass<AbstractExpressionTypeTest> {
                model("codeInsight/expressionType")
            }

            testClass<AbstractRenderingKDocTest> {
                model("codeInsight/renderingKDoc")
            }

            testClass<AbstractBackspaceHandlerTest> {
                model("editor/backspaceHandler")
            }

            testClass<AbstractEnterAfterUnmatchedBraceHandlerTest> {
                model("editor/enterHandler/afterUnmatchedBrace")
            }

            testClass<AbstractMultiLineStringIndentTest> {
                model("editor/enterHandler/multilineString")
            }

            testClass<AbstractQuickDocProviderTest> {
                model("editor/quickDoc", pattern = """^([^_]+)\.(kt|java)$""")
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
                model(
                    "refactoring/safeDelete/deleteValueParameter/kotlinValueParameterWithJava",
                    testMethod = "doValueParameterTestWithJava"
                )
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
                model("findUsages/kotlin", pattern = """^(.+)\.0\.(kt|kts)$""")
                model("findUsages/java", pattern = """^(.+)\.0\.java$""")
                model("findUsages/propertyFiles", pattern = """^(.+)\.0\.properties$""")
            }

            testClass<AbstractFindUsagesWithDisableComponentSearchTest> {
                model("findUsages/kotlin/conventions/components", pattern = """^(.+)\.0\.(kt|kts)$""")
            }

            testClass<AbstractKotlinFindUsagesWithLibraryTest> {
                model("findUsages/libraryUsages", pattern = """^(.+)\.0\.kt$""")
            }

            testClass<AbstractKotlinFindUsagesWithStdlibTest> {
                model("findUsages/stdlibUsages", pattern = """^(.+)\.0\.kt$""")
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

            testClass<AbstractMultiModuleSafeDeleteTest> {
                model("refactoring/safeDeleteMultiModule", extension = "test", singleClass = true)
            }

            testClass<AbstractMultiFileIntentionTest> {
                model("multiFileIntentions", extension = "test", singleClass = true, filenameStartsLowerCase = true)
            }

            testClass<AbstractMultiFileLocalInspectionTest> {
                model("multiFileLocalInspections", extension = "test", singleClass = true, filenameStartsLowerCase = true)
            }

            testClass<AbstractMultiFileInspectionTest> {
                model("multiFileInspections", extension = "test", singleClass = true)
            }

            testClass<AbstractFormatterTest> {
                model("formatter", pattern = """^([^\.]+)\.after\.kt.*$""")
                model(
                    "formatter/trailingComma", pattern = """^([^\.]+)\.call\.after\.kt.*$""",
                    testMethod = "doTestCallSite", testClassName = "FormatterCallSite"
                )
                model(
                    "formatter", pattern = """^([^\.]+)\.after\.inv\.kt.*$""",
                    testMethod = "doTestInverted", testClassName = "FormatterInverted"
                )
                model(
                    "formatter/trailingComma", pattern = """^([^\.]+)\.call\.after\.inv\.kt.*$""",
                    testMethod = "doTestInvertedCallSite", testClassName = "FormatterInvertedCallSite"
                )
            }

            testClass<AbstractTypingIndentationTestBase> {
                model(
                    "indentationOnNewline", pattern = """^([^\.]+)\.after\.kt.*$""", testMethod = "doNewlineTest",
                    testClassName = "DirectSettings"
                )
                model(
                    "indentationOnNewline", pattern = """^([^\.]+)\.after\.inv\.kt.*$""", testMethod = "doNewlineTestWithInvert",
                    testClassName = "InvertedSettings"
                )
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
                model("codeInsight/outOfBlock", pattern = KT_OR_KTS)
            }

            testClass<AbstractChangeLocalityDetectorTest> {
                model("codeInsight/changeLocality", pattern = KT_OR_KTS)
            }

            testClass<AbstractDataFlowValueRenderingTest> {
                model("dataFlowValueRendering")
            }


            testClass<AbstractLiteralTextToKotlinCopyPasteTest> {
                model("copyPaste/plainTextLiteral", pattern = """^([^\.]+)\.txt$""")
            }

            testClass<AbstractLiteralKotlinToKotlinCopyPasteTest> {
                model("copyPaste/literal", pattern = """^([^\.]+)\.kt$""")
            }

            testClass<AbstractInsertImportOnPasteTest> {
                model(
                    "copyPaste/imports",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doTestCopy",
                    testClassName = "Copy",
                    recursive = false
                )
                model(
                    "copyPaste/imports",
                    pattern = KT_WITHOUT_DOTS_IN_NAME,
                    testMethod = "doTestCut",
                    testClassName = "Cut",
                    recursive = false
                )
            }

            testClass<AbstractMoveOnCutPasteTest> {
                model("copyPaste/moveDeclarations", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doTest")
            }

            testClass<AbstractUpdateKotlinCopyrightTest> {
                model("copyright", pattern = KT_OR_KTS, testMethod = "doTest")
            }

            testClass<AbstractHighlightExitPointsTest> {
                model("exitPoints")
            }

            testClass<AbstractLineMarkersTest> {
                model("codeInsight/lineMarker")
            }

            testClass<AbstractLineMarkersTestInLibrarySources> {
                model("codeInsightInLibrary/lineMarker", testMethod = "doTestWithLibrary")
            }

            testClass<AbstractMultiModuleLineMarkerTest> {
                model("multiModuleLineMarker", extension = null, recursive = false)
            }

            testClass<AbstractShortenRefsTest> {
                model("shortenRefs", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }
            testClass<AbstractAddImportTest> {
                model("addImport", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractAddImportAliasTest> {
                model("addImportAlias", pattern = KT_WITHOUT_DOTS_IN_NAME)
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

            testClass<AbstractCommonDecompiledTextTest> {
                model("decompiler/decompiledText", pattern = """^([^\.]+)$""")
            }

            testClass<AbstractJvmDecompiledTextTest> {
                model("decompiler/decompiledTextJvm", pattern = """^([^\.]+)$""")
            }

            testClass<AbstractCommonDecompiledTextFromJsMetadataTest> {
                model("decompiler/decompiledText", pattern = """^([^\.]+)$""", targetBackend = TargetBackend.JS)
            }

            testClass<AbstractJsDecompiledTextFromJsMetadataTest> {
                model("decompiler/decompiledTextJs", pattern = """^([^\.]+)$""", targetBackend = TargetBackend.JS)
            }

            testClass<AbstractClsStubBuilderTest> {
                model("decompiler/stubBuilder", extension = null, recursive = false)
            }

            testClass<AbstractJvmOptimizeImportsTest> {
                model("editor/optimizeImports/jvm", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
                model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }
            testClass<AbstractJsOptimizeImportsTest> {
                model("editor/optimizeImports/js", pattern = KT_WITHOUT_DOTS_IN_NAME)
                model("editor/optimizeImports/common", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractKotlinExceptionFilterTest> {
                model("debugger/exceptionFilter", pattern = """^([^\.]+)$""", recursive = false)
            }

            testClass<AbstractStubBuilderTest> {
                model("stubs", extension = "kt")
            }

            testClass<AbstractMultiFileHighlightingTest> {
                model("multiFileHighlighting", recursive = false)
            }

            testClass<AbstractMultiPlatformHighlightingTest> {
                model("multiModuleHighlighting/multiplatform/", recursive = false, extension = null)
            }

            testClass<AbstractMultiplatformAnalysisTest> {
                model("multiplatform", recursive = false, extension = null)
            }

            testClass<AbstractQuickFixMultiModuleTest> {
                model("multiModuleQuickFix", extension = null, deep = 1)
            }

            testClass<AbstractKotlinGotoImplementationMultiModuleTest> {
                model("navigation/implementations/multiModule", recursive = false, extension = null)
            }

            testClass<AbstractKotlinGotoRelatedSymbolMultiModuleTest> {
                model("navigation/relatedSymbols/multiModule", recursive = false, extension = null)
            }

            testClass<AbstractKotlinGotoSuperMultiModuleTest> {
                model("navigation/gotoSuper/multiModule", recursive = false, extension = null)
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
                model("refactoring/extractSuperclass", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME, testMethod = "doExtractSuperclassTest")
                model("refactoring/extractInterface", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME, testMethod = "doExtractInterfaceTest")
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

            testClass<AbstractKotlinCodeVisionProviderTest> {
                model("codeInsight/codeVision")
            }

            testClass<AbstractKotlinReferenceTypeHintsProviderTest> {
                model("codeInsight/hints/types")
            }

            testClass<AbstractScriptConfigurationHighlightingTest> {
                model("script/definition/highlighting", extension = null, recursive = false)
                model("script/definition/complex", extension = null, recursive = false, testMethod = "doComplexTest")
            }

            testClass<AbstractScriptConfigurationNavigationTest> {
                model("script/definition/navigation", extension = null, recursive = false)
            }

            testClass<AbstractScriptConfigurationCompletionTest> {
                model("script/definition/completion", extension = null, recursive = false)
            }

            testClass<AbstractScriptConfigurationInsertImportOnPasteTest> {
                model(
                    "script/definition/imports",
                    testMethod = "doTestCopy",
                    testClassName = "Copy",
                    extension = null,
                    recursive = false
                )
                model(
                    "script/definition/imports",
                    testMethod = "doTestCut",
                    testClassName = "Cut",
                    extension = null,
                    recursive = false
                )
            }

            testClass<AbstractScriptDefinitionsOrderTest> {
                model("script/definition/order", extension = null, recursive = false)
            }

            testClass<AbstractNameSuggestionProviderTest> {
                model("refactoring/nameSuggestionProvider")
            }

            testClass<AbstractSlicerTreeTest> {
                model("slicer", excludeDirs = listOf("mpp"))
            }

            testClass<AbstractSlicerLeafGroupingTest> {
                model("slicer/inflow", singleClass = true)
            }

            testClass<AbstractSlicerNullnessGroupingTest> {
                model("slicer/inflow", singleClass = true)
            }

            testClass<AbstractSlicerMultiplatformTest> {
                model("slicer/mpp", recursive = false, extension = null)
            }

            testClass<AbstractKotlinNavBarTest> {
                model("navigationToolbar", recursive = false)
            }
        }




        testGroup("idea/idea-frontend-fir/fir-low-level-api-ide-impl/tests", "idea/idea-frontend-fir/idea-fir-low-level-api/testdata") {
            testClass<AbstractProjectWideOutOfBlockKotlinModificationTrackerTest> {
                model("outOfBlockProjectWide")
            }
//
//            testClass<AbstractFileStructureAndOutOfBlockModificationTrackerConsistencyTest> {
//                model("outOfBlockProjectWide")
//            }

            testClass<AbstractSessionsInvalidationTest> {
                model("sessionInvalidation", recursive = false, extension = null)
            }
        }

        testGroup("idea/idea-fir/tests", "idea") {
            testClass<AbstractFirHighlightingTest> {
                model("testData/highlighter")
                model("idea-fir/testData/highlighterFir", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup("idea/idea-fir-performance-tests/tests", "idea") {
            testClass<AbstractFirHighlightingPerformanceTest> {
                model("testData/highlighter")
            }
        }

        testGroup("idea/idea-fir-performance-tests/tests", "idea/idea-completion/testData") {
            testClass<AbstractHighLevelPerformanceBasicCompletionHandlerTest> {
                model("handlers/basic", testMethod = "doPerfTest", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup("idea/idea-fir/tests", "idea/testData") {
            testClass<AbstractFirReferenceResolveTest> {
                model("resolve/references", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirKotlinHighlightingPassTest> {
                model("checker", recursive = false, excludedPattern = excludedFirTestdataPattern)
                model("checker/regression", excludedPattern = excludedFirTestdataPattern)
                model("checker/recovery", excludedPattern = excludedFirTestdataPattern)
                model("checker/rendering", excludedPattern = excludedFirTestdataPattern)
                model("checker/infos", excludedPattern = excludedFirTestdataPattern)
                model("checker/diagnosticsMessage", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractHighLevelQuickFixTest> {
                val pattern = "^([\\w\\-_]+)\\.kt$"
                model("quickfix/abstract", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/addExclExclCall", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/addInitializer", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/addPropertyAccessors", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/expressions", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/lateinit", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/modifiers", pattern = pattern, filenameStartsLowerCase = true, recursive = false)
                model("quickfix/nullables", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/override", pattern = pattern, filenameStartsLowerCase = true, recursive = false)
                model("quickfix/override/typeMismatchOnOverride", pattern = pattern, filenameStartsLowerCase = true, recursive = false)
                model("quickfix/replaceInfixOrOperatorCall", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/replaceWithDotCall", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/replaceWithSafeCall", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/variables/changeMutability", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/variables/removeValVarFromParameter", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/when", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/wrapWithSafeLetCall", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/typeMismatch/componentFunctionReturnTypeMismatch", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/typeMismatch/typeMismatchOnReturnedExpression", pattern = pattern, filenameStartsLowerCase = true)
                model("quickfix/toString", pattern = pattern, filenameStartsLowerCase = true)
            }

            testClass<AbstractHighLevelQuickFixMultiFileTest> {
                model("quickfix/autoImports", pattern = """^(\w+)\.((before\.Main\.\w+))$""", testMethod = "doTestWithExtraFile")
            }

            testClass<AbstractHLInspectionTest> {
                val pattern = "^(inspections\\.test)$"
                model("inspections/redundantUnitReturnType", pattern = pattern, singleClass = true)
            }


            testClass<AbstractHLIntentionTest> {
                val pattern = "^([\\w\\-_]+)\\.(kt|kts)$"
                model("intentions/addPropertyAccessors", pattern = pattern)
                model("intentions/specifyTypeExplicitly", pattern = pattern)
                model("intentions/importAllMembers", pattern = pattern)
                model("intentions/importMember", pattern = pattern)
            }

            testClass<AbstractFirShortenRefsTest> {
                model("shortenRefsFir", pattern = KT_WITHOUT_DOTS_IN_NAME, testMethod = "doTestWithMuting")
            }
        }

        testGroup("idea/idea-fir/tests", "idea") {
            testClass<AbstractHLLocalInspectionTest> {
                val pattern = "^([\\w\\-_]+)\\.(kt|kts)$"
                model("testData/inspectionsLocal/redundantVisibilityModifier", pattern = pattern)
                model("idea-fir/testData/inspectionsLocal", pattern = pattern)
            }
        }

        testGroup("idea/idea-fir/tests", "idea/idea-completion/testData") {
            testClass<AbstractHighLevelJvmBasicCompletionTest> {
                model("basic/common")
                model("basic/java")
                model("../../idea-fir/testData/completion/basic/common", testClassName = "CommonFir")
            }

            testClass<AbstractHighLevelBasicCompletionHandlerTest> {
                model("handlers/basic", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirKeywordCompletionHandlerTest> {
                model("handlers/keywords", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractHighLevelWeigherTest> {
                model("weighers/basic", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractHighLevelMultiFileJvmBasicCompletionTest> {
                model("basic/multifile", extension = null, recursive = false)
            }

            testClass<AbstractFirKeywordCompletionTest> {
                model("keywords", recursive = false, pattern = KT_WITHOUT_FIR_PREFIX)
                model(
                    "../../idea-fir/testData/completion/keywords",
                    testClassName = "KeywordsFir",
                    recursive = false,
                    pattern = KT_WITHOUT_FIR_PREFIX
                )
            }
        }

        testGroup("idea/idea-fir/tests", "idea/testData/findUsages") {

            testClass<AbstractFindUsagesFirTest> {
                model("kotlin", pattern = """^(.+)\.0\.(kt|kts)$""")
                model("java", pattern = """^(.+)\.0\.java$""")
                model("propertyFiles", pattern = """^(.+)\.0\.properties$""")
            }

            testClass<AbstractFindUsagesWithDisableComponentSearchFirTest> {
                model("kotlin/conventions/components", pattern = """^(.+)\.0\.(kt|kts)$""")
            }

            testClass<AbstractKotlinFindUsagesWithLibraryFirTest> {
                model("libraryUsages", pattern = """^(.+)\.0\.kt$""")
            }

            testClass<AbstractKotlinFindUsagesWithStdlibFirTest> {
                model("stdlibUsages", pattern = """^(.+)\.0\.kt$""")
            }
        }

        testGroup("idea/idea-fir-fe10-binding/tests", "idea") {
            testClass<AbstractFe10BindingIntentionTest> {
                val pattern = "^([\\w\\-_]+)\\.(kt|kts)$"
                model("testData/intentions/conventionNameCalls", pattern = pattern)
            }
        }

        testGroup("idea/scripting-support/test", "idea/scripting-support/testData") {
            testClass<AbstractScratchRunActionTest> {
                model(
                    "scratch",
                    extension = "kts",
                    testMethod = "doScratchCompilingTest",
                    testClassName = "ScratchCompiling",
                    recursive = false
                )
                model("scratch", extension = "kts", testMethod = "doScratchReplTest", testClassName = "ScratchRepl", recursive = false)
                model(
                    "scratch/multiFile",
                    extension = null,
                    testMethod = "doScratchMultiFileTest",
                    testClassName = "ScratchMultiFile",
                    recursive = false
                )

                model(
                    "worksheet",
                    extension = "ws.kts",
                    testMethod = "doWorksheetCompilingTest",
                    testClassName = "WorksheetCompiling",
                    recursive = false
                )
                model(
                    "worksheet",
                    extension = "ws.kts",
                    testMethod = "doWorksheetReplTest",
                    testClassName = "WorksheetRepl",
                    recursive = false
                )
                model(
                    "worksheet/multiFile",
                    extension = null,
                    testMethod = "doWorksheetMultiFileTest",
                    testClassName = "WorksheetMultiFile",
                    recursive = false
                )

                model(
                    "scratch/rightPanelOutput",
                    extension = "kts",
                    testMethod = "doRightPreviewPanelOutputTest",
                    testClassName = "ScratchRightPanelOutput",
                    recursive = false
                )
            }

            testClass<AbstractScratchLineMarkersTest> {
                model("scratch/lineMarker", testMethod = "doScratchTest", pattern = KT_OR_KTS)
            }

            testClass<AbstractScriptTemplatesFromDependenciesTest> {
                model("script/templatesFromDependencies", extension = null, recursive = false)
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

        testGroup("idea/idea-gradle/tests", "idea/testData") {
            testClass<AbstractGradleConfigureProjectByChangingFileTest> {
                model("configuration/gradle", extension = null, recursive = false, testMethod = "doTestGradle")
                model("configuration/gsk", extension = null, recursive = false, testMethod = "doTestGradle")
            }
        }

        testGroup("idea/tests", "compiler/testData") {
            testClass<AbstractResolveByStubTest> {
                model("loadJava/compiledKotlin")
            }

            testClass<AbstractLoadJavaClsStubTest> {
                model("loadJava/compiledKotlin", testMethod = "doTestCompiledKotlin")
            }

            testClass<AbstractIdeLightClassTest> {
                model("asJava/lightClasses", excludeDirs = listOf("delegation", "script"), pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractIdeLightClassForScriptTest> {
                model("asJava/script/ide", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractUltraLightClassSanityTest> {
                model("asJava/lightClasses", pattern = KT_OR_KTS)
            }
            testClass<AbstractUltraLightClassLoadingTest> {
                model("asJava/ultraLightClasses", pattern = KT_OR_KTS)
            }
            testClass<AbstractUltraLightScriptLoadingTest> {
                model("asJava/ultraLightScripts", pattern = KT_OR_KTS)
            }
            testClass<AbstractUltraLightFacadeClassTest> {
                model("asJava/ultraLightFacades", pattern = KT_OR_KTS)
            }

            testClass<AbstractIdeCompiledLightClassTest> {
                model(
                    "asJava/lightClasses",
                    excludeDirs = listOf("local", "compilationErrors", "ideRegression", "script"),
                    pattern = KT_WITHOUT_DOTS_IN_NAME
                )
            }
        }

        testGroup("idea/idea-fir/tests", "compiler/testData") {
            testClass<AbstractFirLightClassTest> {
                model("asJava/lightClasses", excludeDirs = listOf("delegation", "script"), pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractFirClassLoadingTest> {
                model("asJava/ultraLightClasses", pattern = KT_OR_KTS)
            }

            testClass<AbstractFirLightFacadeClassTest> {
                model("asJava/ultraLightFacades", pattern = KT_OR_KTS)
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
                model("weighers/basic", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
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
                model("keywords", recursive = false, pattern = KT_WITHOUT_FIR_PREFIX)
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
                model("handlers/charFilter", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractMultiFileJvmBasicCompletionTest> {
                model("basic/multifile", extension = null, recursive = false)
            }

            testClass<AbstractMultiFileSmartCompletionTest> {
                model("smartMultiFile", extension = null, recursive = false)
            }

            testClass<AbstractJvmBasicCompletionTest>("KDocCompletionTestGenerated") {
                model("kdoc")
            }

            testClass<AbstractJava8BasicCompletionTest> {
                model("basic/java8")
            }

            testClass<AbstractCompletionIncrementalResolveTest> {
                model("incrementalResolve")
            }

            testClass<AbstractMultiPlatformCompletionTest> {
                model("multiPlatform", recursive = false, extension = null)
            }
        }

        testGroup(
            "libraries/tools/new-project-wizard/new-project-wizard-cli/tests",
            "libraries/tools/new-project-wizard/new-project-wizard-cli/testData"
        ) {
            testClass<AbstractYamlBuildFileGenerationTest> {
                model("buildFileGeneration", recursive = false, extension = null)
            }
            testClass<AbstractProjectTemplateBuildFileGenerationTest> {
                model("projectTemplatesBuildFileGeneration", recursive = false, extension = null)
            }
        }

        testGroup(
            "idea/idea-new-project-wizard/tests",
            "libraries/tools/new-project-wizard/new-project-wizard-cli/testData"
        ) {
            fun TestGroup.TestClass.allBuildSystemTests(relativeRootPath: String) {
                for (testClass in listOf("GradleKts", "GradleGroovy", "Maven")) {
                    model(
                        relativeRootPath,
                        recursive = false,
                        extension = null,
                        testMethod = "doTest${testClass}",
                        testClassName = testClass
                    )
                }
            }
            testClass<AbstractYamlNewWizardProjectImportTest> {
                allBuildSystemTests("buildFileGeneration")
            }
            testClass<AbstractProjectTemplateNewWizardProjectImportTest> {
                allBuildSystemTests("projectTemplatesBuildFileGeneration")
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


        testGroup("nj2k/tests", "nj2k/testData") {
            testClass<AbstractNewJavaToKotlinConverterSingleFileTest> {
                model("newJ2k", pattern = """^([^\.]+)\.java$""")
            }
            testClass<AbstractCommonConstraintCollectorTest> {
                model("inference/common")
            }
            testClass<AbstractNullabilityInferenceTest> {
                model("inference/nullability")
            }
            testClass<AbstractMutabilityInferenceTest> {
                model("inference/mutability")
            }
            testClass<AbstractNewJavaToKotlinCopyPasteConversionTest> {
                model("copyPaste", pattern = """^([^\.]+)\.java$""")
            }
            testClass<AbstractTextNewJavaToKotlinCopyPasteConversionTest> {
                model("copyPastePlainText", pattern = """^([^\.]+)\.txt$""")
            }
            testClass<AbstractNewJavaToKotlinConverterMultiFileTest> {
                model("multiFile", extension = null, recursive = false)
            }
        }

        testGroup("jps-plugin/jps-tests/test", "jps-plugin/testData") {
            testClass<AbstractIncrementalJvmJpsTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model("incremental/multiModule/jvm", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model(
                    "incremental/multiModule/multiplatform/custom", extension = null, excludeParentDirs = true,
                    targetBackend = TargetBackend.JVM_IR
                )
                model("incremental/pureKotlin", extension = null, recursive = false, targetBackend = TargetBackend.JVM_IR)
                model("incremental/withJava", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model("incremental/inlineFunCallSite", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR)
                model(
                    "incremental/classHierarchyAffected", extension = null, excludeParentDirs = true, targetBackend = TargetBackend.JVM_IR
                )
            }

            actualizeMppJpsIncTestCaseDirs(testDataRoot, "incremental/multiModule/multiplatform/withGeneratedContent")

            testClass<AbstractIncrementalJsJpsTest> {
                model("incremental/multiModule/common", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractMultiplatformJpsTestWithGeneratedContent> {
                model(
                    "incremental/multiModule/multiplatform/withGeneratedContent", extension = null, excludeParentDirs = true,
                    testClassName = "MultiplatformMultiModule", recursive = true
                )
            }

            testClass<AbstractJvmLookupTrackerTest> {
                model("incremental/lookupTracker/jvm", extension = null, recursive = false)
            }
            testClass<AbstractJsLookupTrackerTest> {
                model("incremental/lookupTracker/js", extension = null, recursive = false)
            }
            testClass<AbstractJsKlibLookupTrackerTest> {
                // todo: investigate why lookups are different from non-klib js
                model("incremental/lookupTracker/jsKlib", extension = null, recursive = false)
            }

            testClass<AbstractIncrementalLazyCachesTest> {
                model("incremental/lazyKotlinCaches", extension = null, excludeParentDirs = true)
                model("incremental/changeIncrementalOption", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractIncrementalCacheVersionChangedTest> {
                model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractDataContainerVersionChangedTest> {
                model("incremental/cacheVersionChanged", extension = null, excludeParentDirs = true)
            }
        }

        testGroup("jps-plugin/jps-tests/test", "jps-plugin/testData") {
            fun TestGroup.TestClass.commonProtoComparisonTests() {
                model("comparison/classSignatureChange", extension = null, excludeParentDirs = true)
                model("comparison/classPrivateOnlyChange", extension = null, excludeParentDirs = true)
                model("comparison/classMembersOnlyChanged", extension = null, excludeParentDirs = true)
                model("comparison/packageMembers", extension = null, excludeParentDirs = true)
                model("comparison/unchanged", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractJvmProtoComparisonTest> {
                commonProtoComparisonTests()
                model("comparison/jvmOnly", extension = null, excludeParentDirs = true)
            }

            testClass<AbstractJsProtoComparisonTest> {
                commonProtoComparisonTests()
                model("comparison/jsOnly", extension = null, excludeParentDirs = true)
            }
        }

        testGroup("plugins/parcelize/parcelize-ide/tests", "plugins/parcelize/parcelize-ide/testData") {
            testClass<AbstractParcelizeQuickFixTest> {
                model("quickfix", pattern = "^([\\w\\-_]+)\\.kt$", filenameStartsLowerCase = true)
            }

            testClass<AbstractParcelizeCheckerTest> {
                model("checker", extension = "kt")
            }
        }

        testGroup(
            "plugins/kotlin-serialization/kotlin-serialization-ide/test",
            "plugins/kotlin-serialization/kotlin-serialization-ide/testData"
        ) {
            testClass<AbstractSerializationPluginIdeDiagnosticTest> {
                model("diagnostics")
            }
            testClass<AbstractSerializationQuickFixTest> {
                model("quickfix", pattern = "^([\\w\\-_]+)\\.kt$", filenameStartsLowerCase = true)
            }
        }

        testGroup("plugins/uast-kotlin-fir/tests", "plugins/uast-kotlin-fir/testData") {
            testClass<AbstractFirUastDeclarationTest> {
                model("declaration")
            }

            testClass<AbstractFirUastTypesTest> {
                model("type")
            }
        }

        testGroup("plugins/uast-kotlin-fir/tests", "plugins/uast-kotlin/testData") {
            testClass<AbstractFirLegacyUastDeclarationTest> {
                model("")
            }

            testClass<AbstractFirLegacyUastIdentifiersTest> {
                model("")
            }

            testClass<AbstractFirLegacyUastTypesTest> {
                model("")
            }

            testClass<AbstractFirLegacyUastValuesTest> {
                model("")
            }
        }

        testGroup("plugins/uast-kotlin-fir/tests", "plugins/uast-kotlin-fir/testData") {
            testClass<AbstractFE1UastDeclarationTest> {
                model("declaration")
            }

            testClass<AbstractFE1UastTypesTest> {
                model("type")
            }
        }

        testGroup("plugins/uast-kotlin-fir/tests", "plugins/uast-kotlin/testData") {
            testClass<AbstractFE1LegacyUastDeclarationTest> {
                model("")
            }

            testClass<AbstractFE1LegacyUastIdentifiersTest> {
                model("")
            }

            testClass<AbstractFE1LegacyUastTypesTest> {
                model("")
            }

            testClass<AbstractFE1LegacyUastValuesTest> {
                model("")
            }
        }

        testGroup("idea/performanceTests/test", "idea/testData") {
            testClass<AbstractPerformanceJavaToKotlinCopyPasteConversionTest> {
                model("copyPaste/conversion", testMethod = "doPerfTest", pattern = """^([^\.]+)\.java$""")
            }

            testClass<AbstractPerformanceNewJavaToKotlinCopyPasteConversionTest> {
                model("copyPaste/conversion", testMethod = "doPerfTest", pattern = """^([^\.]+)\.java$""")
            }

            testClass<AbstractPerformanceLiteralKotlinToKotlinCopyPasteTest> {
                model("copyPaste/literal", testMethod = "doPerfTest", pattern = """^([^\.]+)\.kt$""")
            }

            testClass<AbstractPerformanceHighlightingTest> {
                model("highlighter", testMethod = "doPerfTest")
            }

            testClass<AbstractPerformanceAddImportTest> {
                model("addImport", testMethod = "doPerfTest", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractPerformanceTypingIndentationTest> {
                model("indentationOnNewline", testMethod = "doPerfTest", pattern = KT_OR_KTS_WITHOUT_DOTS_IN_NAME)
            }
        }

        testGroup("idea/performanceTests/test", "idea/idea-completion/testData") {
            testClass<AbstractPerformanceCompletionIncrementalResolveTest> {
                model("incrementalResolve", testMethod = "doPerfTest")
            }

            testClass<AbstractPerformanceBasicCompletionHandlerTest> {
                model("handlers/basic", testMethod = "doPerfTest", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }

            testClass<AbstractPerformanceSmartCompletionHandlerTest> {
                model("handlers/smart", testMethod = "doPerfTest")
            }

            testClass<AbstractPerformanceKeywordCompletionHandlerTest> {
                model("handlers/keywords", testMethod = "doPerfTest")
            }

            testClass<AbstractPerformanceCompletionCharFilterTest> {
                model("handlers/charFilter", testMethod = "doPerfTest", pattern = KT_WITHOUT_DOTS_IN_NAME)
            }
        }
    }
}
