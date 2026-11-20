/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.storage

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LockBasedStorageManagerTest {
    private val storageManager = LockBasedStorageManager("test")
    private var calls = 0

    private fun failOnFirstCall(): Int {
        if (++calls == 1) throw OutOfMemoryError()
        return calls
    }

    @Test
    fun testLazyValueDoesNotMemoizeVirtualMachineError() {
        val lazyValue = storageManager.createLazyValue { failOnFirstCall() }

        assertFailsWith<OutOfMemoryError> { lazyValue() }
        assertEquals(2, lazyValue())
    }

    @Test
    fun testMemoizedFunctionDoesNotMemoizeVirtualMachineError() {
        val function = storageManager.createMemoizedFunction<String, Int> { failOnFirstCall() }

        assertFailsWith<OutOfMemoryError> { function("key") }
        assertEquals(2, function("key"))
    }
}
