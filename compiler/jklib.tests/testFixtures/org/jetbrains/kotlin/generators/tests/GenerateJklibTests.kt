/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.jklib.test.cli.AbstractJKlibCliTest
import org.jetbrains.kotlin.jklib.test.irText.AbstractFirJKlibIrTextTest
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

fun main(args: Array<String>) {
    val testsRoot = args[0]
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup(testsRoot, "compiler/testData") {
            testClass<AbstractFirJKlibIrTextTest> {
                model("ir/irText", excludeDirs = listOf("declarations/multiplatform/k1"))
            }
            testClass<AbstractJKlibCliTest>(
                annotations = listOf(annotation<Execution>("value" to ExecutionMode.SAME_THREAD))
            ) {
                model("cli/jklib", extension = "args", testMethod = "doJklibTest", recursive = false)
            }
        }
    }
}
