/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts


import org.jetbrains.kotlin.analysis.test.data.manager.fakes.FakeManagedTest
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

/**
 * Fake test with prefixes ["x", "y"] for testing non-conflicting scenarios.
 * Does NOT conflict with [FakeConflictingTestABGenerated] (different last prefixes, no overlap).
 */
@TestMetadata("testData/conflicts")
class FakeNonConflictingTestXYGenerated : FakeManagedTest() {
    override val variantChain = listOf("x", "y")

    @Test
    @TestMetadata("xy.kt")
    fun testXY() {
    }
}
