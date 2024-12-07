/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.util

fun Iterable<*>.joinToArrayString(): String = joinToString(separator = ", ", prefix = "[", postfix = "]")
fun Array<*>.joinToArrayString(): String = joinToString(separator = ", ", prefix = "[", postfix = "]")

private const val DEFAULT_LINE_SEPARATOR = "\n"

fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.trimTrailingWhitespaces().let { result -> if (result.endsWith("\n")) result else result + "\n" }

fun String.trimTrailingWhitespaces(): String =
    this.split('\n').joinToString(separator = "\n") { it.trimEnd() }

fun String.trimTrailingWhitespacesAndRemoveRedundantEmptyLinesAtTheEnd(): String {
    val lines = this.split('\n').map { it.trimEnd() }
    return lines.dropLastWhile { it.isBlank() }.joinToString("\n", postfix = "\n")
}

fun String.convertLineSeparators(separator: String = DEFAULT_LINE_SEPARATOR): String {
    return replace(Regex.fromLiteral("\r\n|\r|\n"), separator)
}
