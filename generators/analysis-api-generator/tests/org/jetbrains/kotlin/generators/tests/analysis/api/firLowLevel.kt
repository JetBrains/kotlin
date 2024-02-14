/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirOutOfContentRootContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirSourceContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractScriptContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractScriptDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractSourceDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosticCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirBlackBoxCodegenBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirDiagnosticCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirReversedBlackBoxCodegenBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractErrorResistanceTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractOutOfContentRootWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptLazyDeclarationResolveScopeBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceLazyDeclarationResolveScopeBasedTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceWholeFileResolvePhaseTest
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.KT_OR_KTS
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

internal fun TestGroupSuite.generateFirLowLevelApiTests() {
    testGroup("analysis/low-level-api-fir/tests", "compiler/fir/raw-fir/psi2fir/testData") {
        testClass<AbstractFirSourceLazyBodiesCalculatorTest> {
            model("rawBuilder", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirOutOfContentRootLazyBodiesCalculatorTest> {
            model("rawBuilder", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirScriptLazyBodiesCalculatorTest> {
            model("rawBuilder", pattern = TestGeneratorUtil.KTS)
        }
    }

    testGroup("analysis/low-level-api-fir/tests", "analysis/low-level-api-fir/testData") {
        testClass<AbstractFirSourceLazyDeclarationResolveTest> {
            model("lazyResolve", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirOutOfContentRootLazyDeclarationResolveTest> {
            model("lazyResolve", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirScriptLazyDeclarationResolveTest> {
            model("lazyResolve", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceLazyTypeAnnotationsTest> {
            model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootLazyTypeAnnotationsTest> {
            model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptLazyTypeAnnotationsTest> {
            model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceLazyDeclarationResolveForTypeAnnotationsTest> {
            model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootLazyDeclarationResolveForTypeAnnotationsTest> {
            model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptLazyDeclarationResolveForTypeAnnotationsTest> {
            model("lazyResolveTypeAnnotations", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractStdLibSourcesLazyDeclarationResolveTest> {
            model("lazyResolveStdlibSources")
        }

        testClass<AbstractSourceLazyDeclarationResolveScopeBasedTest> {
            model("lazyResolveScopes", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootLazyDeclarationResolveScopeBasedTest> {
            model("lazyResolveScopes", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptLazyDeclarationResolveScopeBasedTest> {
            model("lazyResolveScopes", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractErrorResistanceTest> {
            model("errorResistance")
        }

        testClass<AbstractSourceInBlockModificationTest> {
            model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootInBlockModificationTest> {
            model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptInBlockModificationTest> {
            model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceDanglingFileInBlockModificationTest> {
            model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootDanglingFileInBlockModificationTest> {
            model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptDanglingFileInBlockModificationTest> {
            model("inBlockModification", recursive = false, pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractCodeFragmentInBlockModificationTest> {
            model("inBlockModification/codeFragments", recursive = false, pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        // Modifiable PSI tests must not be generated until KT-63650 is fixed.
//        testClass<AbstractDeclarationModificationServiceCallExpressionCalleeResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/callExpression",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }
//
//        testClass<AbstractDeclarationModificationServiceDotQualifiedExpressionReceiverResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/dotQualifiedExpression",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }
//
//        testClass<AbstractDeclarationModificationServiceDotQualifiedExpressionSelectorResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/dotQualifiedExpression",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }
//
//        testClass<AbstractDeclarationModificationServicePropertyDeclarationInitializerResilienceTest> {
//            model(
//                "declarationModificationService/psiResilience/propertyDeclaration",
//                recursive = false,
//                pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME,
//            )
//        }

        testClass<AbstractSourceFileStructureTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootFileStructureTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptFileStructureTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractFirSourceContextCollectionTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirOutOfContentRootContextCollectionTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptContextCollectionTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceDiagnosticTraversalCounterTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptDiagnosticTraversalCounterTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceWholeFileResolvePhaseTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootWholeFileResolvePhaseTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptWholeFileResolvePhaseTest> {
            model("fileStructure", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourcePartialRawFirBuilderTestCase> {
            model("partialRawBuilder", testMethod = "doRawFirTest")
        }

        testClass<AbstractOutOfContentRootPartialRawFirBuilderTestCase> {
            model("partialRawBuilder", testMethod = "doRawFirTest")
        }

        testClass<AbstractSourceGetOrBuildFirTest> {
            model("getOrBuildFir", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractOutOfContentRootGetOrBuildFirTest> {
            model("getOrBuildFir", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptGetOrBuildFirTest> {
            model("getOrBuildFir", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractLibraryGetOrBuildFirTest> {
            model("getOrBuildFirBinary")
        }

        testClass<AbstractStdLibBasedGetOrBuildFirTest> {
            model("getOrBuildFirForStdLib")
        }

        testClass<AbstractSourceFileBasedKotlinDeclarationProviderTest> {
            model("fileBasedDeclarationProvider", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptFileBasedKotlinDeclarationProviderTest> {
            model("fileBasedDeclarationProvider", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceNonLocalDeclarationAnchorTest> {
            model("nonLocalDeclarationAnchors", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptNonLocalDeclarationAnchorTest> {
            model("nonLocalDeclarationAnchors", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceClassIdTest> {
            model("classId", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptClassIdTest> {
            model("classId", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceClassIdConsistencyTest> {
            model("classId", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptClassIdConsistencyTest> {
            model("classId", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractCompilationPeerAnalysisTest> {
            model("compilationPeers")
        }

        testClass<AbstractContextCollectorSourceTest> {
            model("contextCollector", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractContextCollectorScriptTest> {
            model("contextCollector", pattern = TestGeneratorUtil.KTS)
        }
    }

    testGroup("analysis/low-level-api-fir/tests", "analysis/analysis-api/testData") {
        testClass<AbstractCodeFragmentCapturingTest> {
            model("components/compilerFacility/compilation/codeFragments/capturing", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    testGroup(
        "analysis/low-level-api-fir/tests",
        "compiler/fir/analysis-tests/testData",
    ) {
        fun TestGroup.TestClass.modelInit() {
            model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

        testClass<AbstractDiagnosticCompilerTestDataTest>(suiteTestClassName = "DiagnosticCompilerTestFirTestdataTestGenerated") {
            modelInit()
        }

        testClass<AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest>(suiteTestClassName = "LLFirPreresolvedReversedDiagnosticCompilerFirTestDataTestGenerated") {
            modelInit()
        }
    }

    testGroup(
        "analysis/low-level-api-fir/tests",
        "compiler/testData",
    ) {
        fun TestGroup.TestClass.modelInit() {
            model(
                "diagnostics/tests",
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                pattern = KT_OR_KTS,
            )
            model(
                "diagnostics/testsWithStdLib",
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                excludeDirs = listOf("native"),
                pattern = KT_OR_KTS,
            )
        }

        testClass<AbstractDiagnosticCompilerTestDataTest>(suiteTestClassName = "DiagnosticCompilerTestFE10TestdataTestGenerated") {
            modelInit()
        }

        testClass<AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest>(suiteTestClassName = "LLFirPreresolvedReversedDiagnosticCompilerFE10TestDataTestGenerated") {
            modelInit()
        }

        testClass<AbstractLLFirBlackBoxCodegenBasedTest> {
            model(
                "codegen/box",
                excludeDirs = listOf(
                    "script", // script is excluded until KT-60127 is implemented
                )
            )
        }

        testClass<AbstractLLFirReversedBlackBoxCodegenBasedTest> {
            model(
                "codegen/box",
                excludeDirs = listOf(
                    "script", // script is excluded until KT-60127 is implemented
                )
            )
        }

        testClass<AbstractLLFirBlackBoxCodegenBasedTest>(suiteTestClassName = "LLFirBlackBoxModernJdkCodegenBasedTestGenerated") {
            model("codegen/boxModernJdk")
        }

        testClass<AbstractLLFirReversedBlackBoxCodegenBasedTest>(suiteTestClassName = "LLFirReversedBlackBoxModernJdkCodegenBasedTestGenerated") {
            model("codegen/boxModernJdk")
        }
    }

    testGroup("analysis/low-level-api-fir/tests", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
        fun TestGroup.TestClass.modelInit() {
            model(
                "diagnostics",
                excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
            )
        }

        testClass<AbstractLLFirDiagnosticCompilerTestDataSpecTest>(suiteTestClassName = "FirIdeSpecTestGenerated") {
            modelInit()
        }

        testClass<AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataSpecTest>(suiteTestClassName = "PreFirIdeSpecTestGenerated") {
            modelInit()
        }
    }
}