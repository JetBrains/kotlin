/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions

import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.CompilationOutcome
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.LogLevel

fun CompilationOutcome.assertLogContainsLines(logLevel: LogLevel, vararg expectedLines: String) {
    assertLogContainsLines(logLevel, expectedLines.toSet())
}

fun CompilationOutcome.assertLogContainsLines(logLevel: LogLevel, expectedLines: Set<String>) {
    requireLogLevel(logLevel)
    val absentLines = expectedLines.filter { !uniqueLogLines.getValue(logLevel).contains(it) }
    assert(absentLines.isEmpty()) {
        """
        |The following lines were expected to be printed on $logLevel level, however they were not:
        |${absentLines.joinToString("\n")}
        """.trimMargin()
    }
}

fun CompilationOutcome.assertLogDoesNotContainLines(logLevel: LogLevel, vararg expectedLines: String) {
    assertLogDoesNotContainLines(logLevel, expectedLines.toSet())
}

fun CompilationOutcome.assertLogDoesNotContainLines(logLevel: LogLevel, expectedLines: Set<String>) {
    requireLogLevel(logLevel)
    val presentLines = expectedLines.filter { uniqueLogLines.getValue(logLevel).contains(it) }
    assert(presentLines.isEmpty()) {
        """
        |The following lines were not expected to be printed on $logLevel level, however they were:
        |${presentLines.joinToString("\n")}
        """.trimMargin()
    }
}

fun CompilationOutcome.assertLogContainsPatterns(logLevel: LogLevel, vararg expectedLines: Regex) {
    assertLogContainsPatterns(logLevel, expectedLines.toSet())
}

fun CompilationOutcome.assertLogContainsPatterns(logLevel: LogLevel, expectedLines: Set<Regex>) {
    requireLogLevel(logLevel)
    val absentLines = expectedLines.filter { regex -> logLines.getValue(logLevel).none { line -> regex.matches(line) } }
    assert(absentLines.isEmpty()) {
        """
        |The following lines were expected to be printed on $logLevel level, however they were not:
        |${absentLines.joinToString("\n")}
        """.trimMargin()
    }
}

fun CompilationOutcome.assertLogDoesNotContainPatterns(logLevel: LogLevel, vararg expectedLines: Regex) {
    assertLogDoesNotContainPatterns(logLevel, expectedLines.toSet())
}

fun CompilationOutcome.assertLogDoesNotContainPatterns(logLevel: LogLevel, expectedLines: Set<Regex>) {
    requireLogLevel(logLevel)
    val absentLines = expectedLines.filter { regex -> logLines.getValue(logLevel).any { line -> regex.matches(line) } }
    assert(absentLines.isEmpty()) {
        """
        |The following lines were not expected to be printed on $logLevel level, however they were:
        |${absentLines.joinToString("\n")}
        """.trimMargin()
    }
}