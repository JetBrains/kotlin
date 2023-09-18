/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirDiagnosticCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirOutOfContentRootContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirSourceContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractScriptContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractScriptDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractSourceDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosticCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractErrorResistanceTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractOutOfContentRootWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractScriptWholeFileResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceWholeFileResolvePhaseTest
import org.jetbrains.kotlin.generators.TestGroup
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

internal fun TestGroupSuite.generateFirLowLevelApiTests() {
    testGroup("analysis/low-level-api-fir/tests", "compiler/fir/raw-fir/psi2fir/testData") {
        testClass<AbstractFirSourceLazyBodiesCalculatorTest> {
            model("rawBuilder", testMethod = "doTest")
        }

        testClass<AbstractFirOutOfContentRootLazyBodiesCalculatorTest> {
            model("rawBuilder", testMethod = "doTest")
        }
    }

    testGroup("analysis/low-level-api-fir/tests", "analysis/low-level-api-fir/testData") {
        testClass<AbstractFirOnAirResolveTest> {
            model("onAirResolve")
        }

        testClass<AbstractFirSourceLazyDeclarationResolveTest> {
            model("lazyResolve", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirOutOfContentRootLazyDeclarationResolveTest> {
            model("lazyResolve", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractFirScriptLazyDeclarationResolveTest> {
            model("lazyResolve", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractStdLibSourcesLazyDeclarationResolveTest> {
            model("lazyResolveStdlibSources")
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

        testClass<AbstractCodeFragmentInBlockModificationTest> {
            model("inBlockModification/codeFragments", recursive = false, pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }

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

        testClass<AbstractSourceDependentCopyContextTest> {
            model("dependentCopy", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptDependentCopyContextTest> {
            model("dependentCopy", pattern = TestGeneratorUtil.KTS)
        }

        testClass<AbstractSourceDependentCopyFirTest> {
            model("dependentCopy", pattern = TestGeneratorUtil.KT)
        }

        testClass<AbstractScriptDependentCopyFirTest> {
            model("dependentCopy", pattern = TestGeneratorUtil.KTS)
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
                // MPP tests are not actual for Analysis Api (IDE) infrastructure because it doesn't use IR at all, unlike MPP
                excludeDirsRecursively = listOf("multiplatform"),
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
            )
            model(
                "diagnostics/testsWithStdLib",
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                excludeDirs = listOf("native")
            )
        }

        testClass<AbstractDiagnosticCompilerTestDataTest>(suiteTestClassName = "DiagnosticCompilerTestFE10TestdataTestGenerated") {
            modelInit()
        }

        testClass<AbstractLLFirPreresolvedReversedDiagnosticCompilerTestDataTest>(suiteTestClassName = "LLFirPreresolvedReversedDiagnosticCompilerFE10TestDataTestGenerated") {
            modelInit()
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