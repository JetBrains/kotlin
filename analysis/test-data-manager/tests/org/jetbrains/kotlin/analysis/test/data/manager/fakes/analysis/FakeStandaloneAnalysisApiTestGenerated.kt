/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.analysis


import org.jetbrains.kotlin.analysis.test.data.manager.fakes.FakeManagedTest
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@TestMetadata("testData/analysis/api")
class FakeStandaloneAnalysisApiTestGenerated : FakeManagedTest() {
    override val variantChain = listOf("standalone")

    @Test
    @TestMetadata("symbols.kt")
    fun testSymbols() {
    }

    @Test
    @TestMetadata("types.kt")
    fun testTypes() {
    }

    @Nested
    @TestMetadata("testData/analysis/api/expressions")
    inner class Expressions {
        @Test
        @TestMetadata("call.kt")
        fun testCall() {
        }

        @Test
        @TestMetadata("lambda.kt")
        fun testLambda() {
        }
    }
}
