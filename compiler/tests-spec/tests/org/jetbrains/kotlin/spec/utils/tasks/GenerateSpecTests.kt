/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.spec.utils.tasks

import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.spec.checkers.AbstractDiagnosticsTestSpec
import org.jetbrains.kotlin.spec.codegen.AbstractBlackBoxCodegenTestSpec
import org.jetbrains.kotlin.spec.parsing.AbstractParsingTestSpec
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TESTDATA_PATH
import org.jetbrains.kotlin.spec.utils.GeneralConfiguration.TEST_PATH
import org.jetbrains.kotlin.spec.utils.TestsJsonMapGenerator

fun generateTests() {
    testGroup(TEST_PATH, TESTDATA_PATH) {
        testClass<AbstractDiagnosticsTestSpec> {
            model("diagnostics", excludeDirs = listOf("helpers"))
        }
        testClass<AbstractParsingTestSpec> {
            model("psi", testMethod = "doParsingTest", excludeDirs = listOf("helpers", "templates"))
        }
        testClass<AbstractBlackBoxCodegenTestSpec> {
            model("codegen/box", excludeDirs = listOf("helpers", "templates"))
        }
    }
}

fun main() {
    TestsJsonMapGenerator.buildTestsMapPerSection()
    generateTests()
}
