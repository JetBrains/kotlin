/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager.fakes.conflicts


import org.jetbrains.kotlin.analysis.test.data.manager.fakes.FakeManagedTest
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Test

/**
 * Fake test with prefixes ["b", "a"] for testing conflict detection.
 * Conflicts with [FakeConflictingTestABGenerated] (swapped prefixes - 'b' is last here but in AB's list,
 * and 'a' is last in AB but in this list).
 */
@TestMetadata("testData/conflicts")
class FakeConflictingTestBAGenerated : FakeManagedTest() {
    override val variantChain = listOf("b", "a")

    @Test
    @TestMetadata("ba.kt")
    fun testBA() {
    }
}
