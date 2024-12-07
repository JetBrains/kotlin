/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.tests.analysis.api.konan

import org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based.AbstractLLFirNativeTest
import org.jetbrains.kotlin.analysis.low.level.api.fir.konan.compiler.based.AbstractLLFirReversedNativeTest
import org.jetbrains.kotlin.generators.TestGroupSuite
import org.jetbrains.kotlin.generators.model.annotation
import org.jetbrains.kotlin.test.utils.CUSTOM_TEST_DATA_EXTENSION_PATTERN
import org.junit.jupiter.api.Tag

internal fun TestGroupSuite.generateFirNativeLowLevelTests() {
    testGroup("analysis/low-level-api-fir/low-level-api-fir-native/tests-gen", "compiler/testData/diagnostics") {
        testClass<AbstractLLFirNativeTest>(
            annotations = listOf(annotation(Tag::class.java, "llFirNative"))
        ) {
            model("nativeTests", testMethod = "doTest", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
        }

        testClass<AbstractLLFirReversedNativeTest>(
            annotations = listOf(annotation(Tag::class.java, "llFirNative"))
        ) {
            model("nativeTests", testMethod = "doTest", excludedPattern = CUSTOM_TEST_DATA_EXTENSION_PATTERN)
        }
    }
}