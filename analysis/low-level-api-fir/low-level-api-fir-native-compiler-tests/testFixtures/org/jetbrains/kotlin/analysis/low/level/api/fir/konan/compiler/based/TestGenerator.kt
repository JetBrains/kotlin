/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

fun main(args: Array<String>) {
    generateTestGroupSuiteWithJUnit5(args) {
        testGroup("analysis/low-level-api-fir/low-level-api-fir-native-compiler-tests/tests-gen", "compiler/testData/diagnostics") {
            testClass<AbstractLLNativeDiagnosticsTest>(
                annotations = listOf(annotation(Tag::class.java, "llFirNative"))
            ) {
                model("nativeTests", testMethod = "doTest", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }

            testClass<AbstractLLReversedNativeDiagnosticsTest>(
                annotations = listOf(annotation(Tag::class.java, "llFirNative"))
            ) {
                model("nativeTests", testMethod = "doTest", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
            }
        }
    }
}