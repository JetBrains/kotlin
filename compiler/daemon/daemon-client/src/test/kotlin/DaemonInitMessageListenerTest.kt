/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.daemon.client

import org.jetbrains.kotlin.daemon.common.COMPILE_DAEMON_IS_READY_MESSAGE
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class DaemonInitMessageListenerTest {
    @Test
    @DisplayName("detects absence of the init message")
    fun noInitMessage() {
        val initListener = DaemonInitMessageListener()
        initListener.onOutputLine("Line 1")
        initListener.onOutputLine("Line 2")
        assertFalse(initListener.caughtInitMessage)
        assertEquals(1, initListener.retrieveProblems().size)
    }

    @Test
    @DisplayName("does not report when the init message is there")
    fun withInitMessage() {
        val initListener = DaemonInitMessageListener()
        initListener.onOutputLine(COMPILE_DAEMON_IS_READY_MESSAGE)
        assertTrue(initListener.caughtInitMessage)
        assertEquals(0, initListener.retrieveProblems().size)
    }
}