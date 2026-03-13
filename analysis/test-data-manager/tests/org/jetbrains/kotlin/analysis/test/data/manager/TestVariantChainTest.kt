/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.test.data.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestVariantChainTest {
    @Test
    fun `empty chain with additional variant`() {
        assertEquals(listOf("dangling"), emptyList<String>().withAdditionalVariant("dangling"))
    }

    @Test
    fun `single element chain with additional variant`() {
        assertEquals(
            listOf("standalone.fir", "dangling", "standalone.fir.dangling"),
            listOf("standalone.fir").withAdditionalVariant("dangling"),
        )
    }

    @Test
    fun `multi-element chain with additional variant`() {
        assertEquals(
            listOf("knm", "wasm", "dangling", "knm.dangling", "wasm.dangling"),
            listOf("knm", "wasm").withAdditionalVariant("dangling"),
        )
    }
}
