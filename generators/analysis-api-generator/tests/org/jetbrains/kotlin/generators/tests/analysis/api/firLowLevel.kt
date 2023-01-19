/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractOutOfContentRootFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractSourceFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractOutOfContentRootInnerDeclarationsResolvePhaseTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractSourceInnerDeclarationsResolvePhaseTest
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

    testGroup("analysis/low-level-api-fir/tests", "analysis/low-level-api-fir/testdata") {
        testClass<AbstractFirOnAirResolveTest> {
            model("onAirResolve")
        }

        testClass<AbstractFirSourceLazyDeclarationResolveTest> {
            model("lazyResolve")
        }

        testClass<AbstractFirOutOfContentRootLazyDeclarationResolveTest> {
            model("lazyResolve")
        }

        testClass<AbstractSourceFileStructureTest> {
            model("fileStructure")
        }

        testClass<AbstractOutOfContentRootFileStructureTest> {
            model("fileStructure")
        }

        testClass<AbstractFirSourceContextCollectionTest> {
            model("fileStructure")
        }

        testClass<AbstractFirOutOfContentRootContextCollectionTest> {
            model("fileStructure")
        }

        testClass<AbstractDiagnosticTraversalCounterTest> {
            model("diagnosticTraversalCounter")
        }

        testClass<AbstractSourceInnerDeclarationsResolvePhaseTest> {
            model("innerDeclarationsResolve")
        }

        testClass<AbstractOutOfContentRootInnerDeclarationsResolvePhaseTest> {
            model("innerDeclarationsResolve")
        }

        testClass<AbstractSourcePartialRawFirBuilderTestCase> {
            model("partialRawBuilder", testMethod = "doRawFirTest")
        }

        testClass<AbstractOutOfContentRootPartialRawFirBuilderTestCase> {
            model("partialRawBuilder", testMethod = "doRawFirTest")
        }

        testClass<AbstractSourceGetOrBuildFirTest> {
            model("getOrBuildFir")
        }

        testClass<AbstractOutOfContentRootGetOrBuildFirTest> {
            model("getOrBuildFir")
        }

        testClass<AbstractFileBasedKotlinDeclarationProviderTest> {
            model("fileBasedDeclarationProvider")
        }
    }

    testGroup(
        "analysis/low-level-api-fir/tests",
        "compiler/fir/analysis-tests/testData",
    ) {
        testClass<AbstractDiagnosisCompilerTestDataTest>(suiteTestClassName = "DiagnosisCompilerFirTestdataTestGenerated") {
            model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
        }
    }

    testGroup(
        "analysis/low-level-api-fir/tests",
        "compiler/testData",
    ) {
        testClass<AbstractDiagnosisCompilerTestDataTest>(suiteTestClassName = "DiagnosisCompilerTestFE10TestdataTestGenerated") {
            model(
                "diagnostics/tests",
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
            )
            model(
                "diagnostics/testsWithStdLib",
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
                excludeDirs = listOf("native")
            )
        }
    }


    testGroup("analysis/low-level-api-fir/tests", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
        testClass<AbstractDiagnosisCompilerTestDataSpecTest>(suiteTestClassName = "FirIdeSpecTest") {
            model(
                "diagnostics",
                excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN,
            )
        }
    }
}