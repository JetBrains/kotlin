/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.AbstractDataFlowValueRenderingTest
import org.jetbrains.kotlin.addImport.AbstractAddImportTest
import org.jetbrains.kotlin.addImportAlias.AbstractAddImportAliasTest
import org.jetbrains.kotlin.allopen.AbstractBytecodeListingTestForAllOpen
import org.jetbrains.kotlin.android.parcel.AbstractParcelBoxTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelBytecodeListingTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelIrBoxTest
import org.jetbrains.kotlin.android.parcel.AbstractParcelIrBytecodeListingTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidBytecodeShapeTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidIrBoxTest
import org.jetbrains.kotlin.android.synthetic.test.AbstractAndroidSyntheticPropertyDescriptorTest
import org.jetbrains.kotlin.asJava.classes.*
import org.jetbrains.kotlin.checkers.*
import org.jetbrains.kotlin.copyright.AbstractUpdateKotlinCopyrightTest
import org.jetbrains.kotlin.findUsages.*
import org.jetbrains.kotlin.fir.plugin.AbstractFirAllOpenDiagnosticTest
import org.jetbrains.kotlin.formatter.AbstractFormatterTest
import org.jetbrains.kotlin.formatter.AbstractTypingIndentationTestBase
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.impl.generateTestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS_WITHOUT_DOTS_IN_NAME
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME
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
import org.jetbrains.kotlin.idea.completion.AbstractHighLevelJvmBasicCompletionTest
import org.jetbrains.kotlin.idea.completion.test.*
import org.jetbrains.kotlin.idea.completion.test.handlers.*
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractBasicCompletionWeigherTest
import org.jetbrains.kotlin.idea.completion.test.weighers.AbstractSmartCompletionWeigherTest
import org.jetbrains.kotlin.idea.completion.wheigher.AbstractHighLevelWeigherTest
import org.jetbrains.kotlin.idea.configuration.AbstractGradleConfigureProjectByChangingFileTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralKotlinToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractLiteralTextToKotlinCopyPasteTest
import org.jetbrains.kotlin.idea.conversion.copy.AbstractTextJavaToKotlinCopyPasteConversionTest
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
import org.jetbrains.kotlin.idea.fir.AbstractKtDeclarationAndFirDeclarationEqualityChecker
import org.jetbrains.kotlin.idea.fir.low.level.api.AbstractFirLazyDeclarationResolveTest
import org.jetbrains.kotlin.idea.fir.low.level.api.AbstractFirLazyResolveTest
import org.jetbrains.kotlin.idea.fir.low.level.api.AbstractFirMultiModuleLazyResolveTest
import org.jetbrains.kotlin.idea.fir.low.level.api.AbstractFirMultiModuleResolveTest
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.AbstractFileStructureAndOutOfBlockModificationTrackerConsistencyTest
import org.jetbrains.kotlin.idea.fir.low.level.api.file.structure.AbstractFileStructureTest
import org.jetbrains.kotlin.idea.fir.low.level.api.sessions.AbstractSessionsInvalidationTest
import org.jetbrains.kotlin.idea.fir.low.level.api.trackers.AbstractProjectWideOutOfBlockKotlinModificationTrackerTest
import org.jetbrains.kotlin.idea.folding.AbstractKotlinFoldingTest
import org.jetbrains.kotlin.idea.frontend.api.components.AbstractExpectedExpressionTypeTest
import org.jetbrains.kotlin.idea.frontend.api.components.AbstractReturnExpressionTargetTest
import org.jetbrains.kotlin.idea.frontend.api.fir.AbstractResolveCallTest
import org.jetbrains.kotlin.idea.frontend.api.scopes.AbstractFileScopeTest
import org.jetbrains.kotlin.idea.frontend.api.scopes.AbstractMemberScopeByFqNameTest
import org.jetbrains.kotlin.idea.frontend.api.symbols.*
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
import org.jetbrains.kotlin.incremental.*
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterForWebDemoTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.j2k.AbstractJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.jps.build.*
import org.jetbrains.kotlin.jps.build.dependeciestxt.actualizeMppJpsIncTestCaseDirs
import org.jetbrains.kotlin.jps.incremental.AbstractJsProtoComparisonTest
import org.jetbrains.kotlin.jps.incremental.AbstractJvmProtoComparisonTest
import org.jetbrains.kotlin.jvm.abi.AbstractCompareJvmAbiTest
import org.jetbrains.kotlin.jvm.abi.AbstractCompileAgainstJvmAbiTest
import org.jetbrains.kotlin.jvm.abi.AbstractJvmAbiContentTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractArgumentParsingTest
import org.jetbrains.kotlin.kapt.cli.test.AbstractKaptToolIntegrationTest
import org.jetbrains.kotlin.kapt3.test.AbstractClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.AbstractIrClassFileToSourceStubConverterTest
import org.jetbrains.kotlin.kapt3.test.AbstractIrKotlinKaptContextTest
import org.jetbrains.kotlin.kapt3.test.AbstractKotlinKaptContextTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinConverterMultiFileTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinConverterSingleFileTest
import org.jetbrains.kotlin.nj2k.AbstractNewJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.nj2k.AbstractTextNewJavaToKotlinCopyPasteConversionTest
import org.jetbrains.kotlin.nj2k.inference.common.AbstractCommonConstraintCollectorTest
import org.jetbrains.kotlin.nj2k.inference.mutability.AbstractMutabilityInferenceTest
import org.jetbrains.kotlin.nj2k.inference.nullability.AbstractNullabilityInferenceTest
import org.jetbrains.kotlin.noarg.*
import org.jetbrains.kotlin.pacelize.ide.test.AbstractParcelizeCheckerTest
import org.jetbrains.kotlin.pacelize.ide.test.AbstractParcelizeQuickFixTest
import org.jetbrains.kotlin.parcelize.test.AbstractParcelizeBoxTest
import org.jetbrains.kotlin.parcelize.test.AbstractParcelizeBytecodeListingTest
import org.jetbrains.kotlin.parcelize.test.AbstractParcelizeIrBoxTest
import org.jetbrains.kotlin.parcelize.test.AbstractParcelizeIrBytecodeListingTest
import org.jetbrains.kotlin.psi.patternMatching.AbstractPsiUnifierTest
import org.jetbrains.kotlin.samWithReceiver.AbstractSamWithReceiverScriptTest
import org.jetbrains.kotlin.samWithReceiver.AbstractSamWithReceiverTest
import org.jetbrains.kotlin.search.AbstractAnnotatedMembersSearchTest
import org.jetbrains.kotlin.search.AbstractInheritorsSearchTest
import org.jetbrains.kotlin.shortenRefs.AbstractShortenRefsTest
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.tools.projectWizard.cli.AbstractProjectTemplateBuildFileGenerationTest
import org.jetbrains.kotlin.tools.projectWizard.cli.AbstractYamlBuildFileGenerationTest
import org.jetbrains.kotlin.tools.projectWizard.wizard.AbstractProjectTemplateNewWizardProjectImportTest
import org.jetbrains.kotlin.tools.projectWizard.wizard.AbstractYamlNewWizardProjectImportTest
import org.jetbrains.kotlinx.serialization.AbstractSerializationIrBytecodeListingTest
import org.jetbrains.kotlinx.serialization.AbstractSerializationPluginBytecodeListingTest
import org.jetbrains.kotlinx.serialization.AbstractSerializationPluginDiagnosticTest

fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")
    generateTestGroupSuite(args) {
        testGroup("plugins/parcelize/parcelize-compiler/tests", "plugins/parcelize/parcelize-compiler/testData") {
            testClass<AbstractParcelizeBoxTest> {
                model("box", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractParcelizeIrBoxTest> {
                model("box", targetBackend = TargetBackend.JVM_IR)
            }

            testClass<AbstractParcelizeBytecodeListingTest> {
                model("codegen", targetBackend = TargetBackend.JVM)
            }

            testClass<AbstractParcelizeIrBytecodeListingTest> {
                model("codegen", targetBackend = TargetBackend.JVM_IR)
            }
        }
    }
}
