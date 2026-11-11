/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.apple

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AppleToolExceptionTest {
    private val invocation = AppleToolInvocation(path("/usr/bin/xcrun"), listOf("lipo", "-create", "a b.a"))

    @Test
    fun `message has the command, exit code and stderr`() {
        val exception = AppleToolException(invocation, result(stdout = "ignored", stderr = "fatal error\n", exitCode = 1))
        assertEquals("Command failed with exit code 1: /usr/bin/xcrun lipo -create 'a b.a'\nfatal error", exception.message)
    }

    @Test
    fun `message falls back to stdout when stderr is blank`() {
        val exception = AppleToolException(invocation, result(stdout = "error on stdout", stderr = " \n", exitCode = 2))
        assertEquals("Command failed with exit code 2: /usr/bin/xcrun lipo -create 'a b.a'\nerror on stdout", exception.message)
    }

    @Test
    fun `message puts the hint before the output`() {
        val exception = AppleToolException(invocation, result(stderr = "boom", exitCode = 1), hint = "Install the platform.")
        assertEquals(
            "Command failed with exit code 1: /usr/bin/xcrun lipo -create 'a b.a'\nInstall the platform.\nboom",
            exception.message,
        )
    }

    @Test
    fun `message keeps the last 50 lines of long output`() {
        val output = (1..60).joinToString("\n") { "line $it" }
        val exception = AppleToolException(invocation, result(stderr = output, exitCode = 1))
        val lines = exception.message!!.lines()
        assertEquals("[10 earlier lines omitted]", lines[1])
        assertEquals("line 11", lines[2])
        assertEquals("line 60", lines.last())
    }

    @Test
    fun `message without output is a single line`() {
        val exception = AppleToolException(invocation, result(exitCode = 5))
        assertEquals("Command failed with exit code 5: /usr/bin/xcrun lipo -create 'a b.a'", exception.message)
    }

    @Test
    fun `runChecked accepts listed exit codes and throws on others`() {
        val runner = RecordingAppleToolRunner { result(stdout = "ok", exitCode = 1) }

        assertEquals("ok", runner.runChecked(invocation.copy(acceptedExitCodes = setOf(0, 1))).stdout)
        val exception = assertFailsWith<AppleToolException> { runner.runChecked(invocation) { "hint" } }
        assertSame(invocation, exception.invocation)
        assertEquals(1, exception.result.exitCode)
        assertEquals("hint", exception.hint)
    }
}
