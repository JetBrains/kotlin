/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode

/**
 * Thrown when a compiler call made by a test through the command-line interface fails, to abort further execution of the test.
 *
 * @property compilerOutput The raw output of the compiler. [CustomKlibCompilerFirstStageTestSuppressor] matches it against the expected
 *   kinds of errors.
 * @property compilerCallLog A full report on the failed compiler call (its output, arguments, environment), if the thrower has one.
 *   Used only to build the message of the exception, where it stands in place of [compilerOutput].
 */
data class CustomKlibCompilerException(
    val exitCode: ExitCode,
    val compilerOutput: String,
    val compilerCallLog: String? = null,
) : Exception("Compilation failed with exit code $exitCode\n${compilerCallLog ?: compilerOutput}") {
    /**
     * Returns the same text as [Throwable.toString], not the one that a data class generates. A stack trace starts with this text,
     * and with [compilerCallLog] set, the generated one would print the compiler output twice: as [compilerOutput], and as a part
     * of [compilerCallLog].
     *
     * The text is spelled out, because a call of `super.toString()` is reported as deprecated here.
     */
    override fun toString(): String = "${javaClass.name}: $message"
}
