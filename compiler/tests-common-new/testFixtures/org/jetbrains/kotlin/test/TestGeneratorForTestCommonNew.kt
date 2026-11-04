/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.test.runners.AbstractMetadataDiagnosticTest
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN

fun main(args: Array<String>) {
    val testsRoot = args[0]
    val mainClassName = TestGeneratorUtil.getMainClassName()

    generateTestGroupSuiteWithJUnit5(args, mainClassName) {
        testGroup(testsRoot, testDataRoot = "compiler/testData") {
            testClass<AbstractMetadataDiagnosticTest> {
                model("diagnostics/metadataDiagnosticTests", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
        }
    }
}
