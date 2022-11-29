/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api

import org.jetbrains.kotlin.analysis.low.level.api.fir.*
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractDiagnosticTraversalCounterTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.AbstractFirContextCollectionTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataSpecTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.diagnostic.compiler.based.AbstractDiagnosisCompilerTestDataTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.file.structure.AbstractFileStructureTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.resolve.AbstractInnerDeclarationsResolvePhaseTest
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration
import org.jetbrains.kotlin.spec.utils.tasks.detectDirsWithTestsMapFileOnly
import org.jetbrains.kotlin.test.runners.AbstractFirDiagnosticTestSpec

private const val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

internal fun TestGroupSuite.generateFirLowLevelApiTests() {
    testGroup("analysis/low-level-api-fir/tests", "compiler/fir/raw-fir/psi2fir/testData") {
        testClass<AbstractFirLazyBodiesCalculatorTest> {
            model("rawBuilder", testMethod = "doTest")
        }
    }

    testGroup("analysis/low-level-api-fir/tests", "analysis/low-level-api-fir/testdata") {
        testClass<AbstractFirOnAirResolveTest> {
            model("onAirResolve")
        }

        testClass<AbstractFirLazyDeclarationResolveTest> {
            model("lazyResolve")
        }

        testClass<AbstractFileStructureTest> {
            model("fileStructure")
        }

        testClass<AbstractFirContextCollectionTest> {
            model("fileStructure")
        }

        testClass<AbstractDiagnosticTraversalCounterTest> {
            model("diagnosticTraversalCounter")
        }

        testClass<AbstractInnerDeclarationsResolvePhaseTest> {
            model("innerDeclarationsResolve")
        }

        testClass<AbstractPartialRawFirBuilderTestCase> {
            model("partialRawBuilder", testMethod = "doRawFirTest")
        }

        testClass<AbstractGetOrBuildFirTest> {
            model("getOrBuildFir")
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
                excludedPattern = excludedFirTestdataPattern,
            )
            model(
                "diagnostics/testsWithStdLib",
                excludedPattern = excludedFirTestdataPattern,
                excludeDirs = listOf("native")
            )
        }
    }


    testGroup("analysis/low-level-api-fir/tests", testDataRoot = GeneralConfiguration.SPEC_TESTDATA_PATH) {
        testClass<AbstractDiagnosisCompilerTestDataSpecTest>(suiteTestClassName = "FirIdeSpecTest") {
            model(
                "diagnostics",
                excludeDirs = listOf("helpers") + detectDirsWithTestsMapFileOnly("diagnostics"),
                excludedPattern = excludedFirTestdataPattern,
            )
        }
    }
}