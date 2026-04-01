/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.compiler


import org.jetbrains.kotlin.analysis.test.data.manager.fakes.FakeManagedTest
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@TestMetadata("testData/compiler/fir")
class FakeFirTestGenerated : FakeManagedTest() {
    override val variantChain = emptyList<String>()

    @Test
    @TestMetadata("checker.kt")
    fun testChecker() {
    }

    @Nested
    @TestMetadata("testData/compiler/fir/resolve")
    inner class Resolve {
        @Test
        @TestMetadata("inference.kt")
        fun testInference() {
        }
    }
}
