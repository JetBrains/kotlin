/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CustomKlibCompilerExceptionTest {
    @Test
    fun `given no compiler call log then the message holds the compiler output`() {
        val exception = CustomKlibCompilerException(ExitCode.COMPILATION_ERROR, "raw output")

        assertEquals("Compilation failed with exit code COMPILATION_ERROR\nraw output", exception.message)
    }

    @Test
    fun `given a compiler call log then it replaces the compiler output in the message`() {
        val exception = CustomKlibCompilerException(ExitCode.COMPILATION_ERROR, "raw output", "LOG WITH raw output")

        assertEquals("Compilation failed with exit code COMPILATION_ERROR\nLOG WITH raw output", exception.message)
        assertEquals("raw output", exception.compilerOutput)
    }

    @Test
    fun `given a compiler call log then toString prints the compiler output once`() {
        val exception = CustomKlibCompilerException(ExitCode.COMPILATION_ERROR, "raw output", "LOG WITH raw output")

        assertEquals(
            "org.jetbrains.kotlin.test.klib.CustomKlibCompilerException: " +
                    "Compilation failed with exit code COMPILATION_ERROR\nLOG WITH raw output",
            "$exception",
        )
    }
}
