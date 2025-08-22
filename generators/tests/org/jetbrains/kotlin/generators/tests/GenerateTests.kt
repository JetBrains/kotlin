/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.fir.dataframe.AbstractCompilerFacilityTestForDataFrame
import org.jetbrains.kotlin.fir.dataframe.AbstractDataFrameBlackBoxCodegenTest
import org.jetbrains.kotlin.fir.dataframe.AbstractDataFrameDiagnosticTest
import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5


fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    generateTestGroupSuiteWithJUnit5 {

        testGroup("plugins/kotlin-dataframe/tests-gen", "plugins/kotlin-dataframe/testData") {
            testClass<AbstractDataFrameDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractDataFrameBlackBoxCodegenTest> {
                model("box")
            }

            testClass<AbstractCompilerFacilityTestForDataFrame> {
                model("compilerFacility")
            }
        }
    }
}
