/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.konan

import org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based.AbstractLLNativeBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based.AbstractLLNativeDiagnosticsTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based.AbstractLLReversedNativeBlackBoxTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based.AbstractLLReversedNativeDiagnosticsTest
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

internal fun TestGroupSuite.generateFirNativeLowLevelTests() {
    val llFirNativeAnnotation = annotation(Tag::class.java, "llFirNative")

    testGroup("analysis/low-level-api-fir/low-level-api-fir-native/tests-gen", "compiler/testData/diagnostics") {
        testClass<AbstractLLNativeDiagnosticsTest>(
            annotations = listOf(llFirNativeAnnotation)
        ) {
            model("nativeTests", testMethod = "doTest", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
        }

        testClass<AbstractLLReversedNativeDiagnosticsTest>(
            annotations = listOf(llFirNativeAnnotation)
        ) {
            model("nativeTests", testMethod = "doTest", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
        }
    }

    testGroup(
        "analysis/low-level-api-fir/low-level-api-fir-native/tests-gen",
        "compiler/testData",
    ) {
        fun TestGroup.TestClass.blackBoxInit() {
            model(
                "codegen/box",
                excludeDirs = listOf(
                    "multiplatform/k1",
                )
            )
        }

        testClass<AbstractLLNativeBlackBoxTest>(
            annotations = listOf(llFirNativeAnnotation)
        ) {
            blackBoxInit()
        }

        testClass<AbstractLLReversedNativeBlackBoxTest>(
            annotations = listOf(llFirNativeAnnotation)
        ) {
            blackBoxInit()
        }
    }
}
