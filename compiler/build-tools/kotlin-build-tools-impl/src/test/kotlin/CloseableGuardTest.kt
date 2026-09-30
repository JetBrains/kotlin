/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// GuardedCloseableTest.kt
@file:OptIn(ExperimentalAtomicApi::class)

import org.jetbrains.kotlin.buildtools.internal.CloseableGuard
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class CloseableGuardTest {

    private class TestCloseable : AutoCloseable {
        private val closeableGuard = CloseableGuard(this)
        var closedTimes = 0
        var guardedFunctionCalledTimes = 0

        fun guardedFunction() {
            closeableGuard.requireNotClosed()
            guardedFunctionCalledTimes++
        }

        override fun close() {
            closeableGuard.close {
                closedTimes++
            }
        }
    }


    private val testCloseable = TestCloseable()

    /**
     * Test case to verify that `closeImpl` is invoked when the resource is closed.
     * Ensures the `isClosed` flag behaves as expected.
     */
    @Test
    fun `closeImpl gets invoked on resource close`() {
        assertEquals(0, testCloseable.closedTimes)

        testCloseable.close()
        assertEquals(1, testCloseable.closedTimes)
    }

    /**
     * Test case to verify that `closeImpl` is not invoked multiple times.
     */
    @Test
    fun `closeImpl is not invoked multiple times`() {
        assertEquals(0, testCloseable.closedTimes)

        testCloseable.close()
        testCloseable.close()

        assertEquals(1, testCloseable.closedTimes)
    }

    /**
     * Test case to verify that `requireNotClosed` throws an exception if the resource is already closed.
     */
    @Test
    fun `requireNotClosed throws exception when already closed`() {
        testCloseable.guardedFunction()
        assertEquals(1, testCloseable.guardedFunctionCalledTimes)

        testCloseable.close()

        val exception = assertThrows(IllegalStateException::class.java) {
            testCloseable.guardedFunction()
        }

        assertEquals(
            "Cannot perform operation: Resource class CloseableGuardTest\$TestCloseable was already closed.",
            exception.message
        )
        assertEquals(1, testCloseable.guardedFunctionCalledTimes)
    }

}
