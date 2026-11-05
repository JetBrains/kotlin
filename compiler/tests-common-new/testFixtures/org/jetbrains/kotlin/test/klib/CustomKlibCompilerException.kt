/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.klib

import org.jetbrains.kotlin.cli.common.ExitCode

/**
 * A test runs the compiler through its command line, and the compiler fails. This exception stops the test.
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
    override fun toString(): String = "${javaClass.name}: $message"
}
