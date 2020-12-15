/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.generators

import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.runners.*

fun main(args: Array<String>) {
    val excludedFirTestdataPattern = "^(.+)\\.fir\\.kts?\$"

    generateNewTestGroupSuite(args) {
        testGroup("compiler/tests-common-new/tests-gen", "compiler/testData") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics/tests", pattern = "^(.*)\\.kts?$", excludedPattern = excludedFirTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractDiagnosticUsingJavacTest> {
                model("diagnostics/tests/javac", pattern = "^(.*)\\.kts?$", excludedPattern = excludedFirTestdataPattern)
            }

            testClass<AbstractFirDiagnosticTest>(suiteTestClassName = "FirOldFrontendDiagnosticsTestGenerated") {
                model("diagnostics/tests", excludedPattern = excludedFirTestdataPattern)
                model("diagnostics/testsWithStdLib", excludedPattern = excludedFirTestdataPattern)
            }
        }

        testGroup("compiler/tests-common-new/tests-gen", "compiler/fir/analysis-tests/testData") {
            testClass<AbstractFirDiagnosticTest> {
                model("resolve", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
                model("resolveWithStdlib", pattern = TestGeneratorUtil.KT_WITHOUT_DOTS_IN_NAME)
            }
        }
    }
}
