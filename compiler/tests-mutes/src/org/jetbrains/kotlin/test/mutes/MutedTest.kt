/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.mutes

import java.io.File

class MutedTest(
    val key: String,
    @Suppress("unused") val issue: String?,
    val hasFailFile: Boolean,
    val isFlaky: Boolean
) {
    val methodKey: String
    val classNameKey: String
    val simpleClassName: String

    init {
        val noQuoteKey = key.replace("`", "")
        val beforeParamsKey = noQuoteKey.substringBefore("[")
        val params = noQuoteKey.substringAfterWithDelimiter("[", "")

        methodKey = (beforeParamsKey.substringAfterLast(".", "") + params)
            .also {
                if (it.isEmpty()) throw IllegalArgumentException("Can't get method name: '$key'")
            }

        classNameKey = beforeParamsKey.substringBeforeLast(".", "").also {
            if (it.isEmpty()) throw IllegalArgumentException("Can't get class name: '$key'")
        }

        simpleClassName = classNameKey.substringAfterLast(".")
    }

    companion object {
        fun String.substringAfterWithDelimiter(delimiter: String, missingDelimiterValue: String = this): String {
            val index = indexOf(delimiter)
            return if (index == -1) missingDelimiterValue else (delimiter + substring(index + 1, length))
        }
    }
}

fun getMutedTest(testClass: Class<*>, methodKey: String): MutedTest? {
    return mutedSet.mutedTest(testClass, methodKey)
}

internal fun loadMutedTests(file: File): List<MutedTest> {
    if (!file.exists()) {
        System.err.println("Can't find mute file: ${file.absolutePath}")
        return listOf()
    }

    try {
        val testLines = file.readLines()
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        return testLines.drop(1).map { parseMutedTest(it) }
    } catch (ex: Throwable) {
        throw ParseError("Couldn't parse file with muted tests: $file", cause = ex)
    }
}

private val COLUMN_PARSE_REGEXP = Regex("\\s*(?:(?:\"((?:[^\"]|\"\")*)\")|([^,]*))\\s*")
private val MUTE_LINE_PARSE_REGEXP = Regex("$COLUMN_PARSE_REGEXP,$COLUMN_PARSE_REGEXP,$COLUMN_PARSE_REGEXP,$COLUMN_PARSE_REGEXP")
private fun parseMutedTest(str: String): MutedTest {
    val matchResult = MUTE_LINE_PARSE_REGEXP.matchEntire(str) ?: throw ParseError("Can't parse the line: $str")
    val resultValues = matchResult.groups.filterNotNull()

    val testKey = resultValues[1].value
    val issue = resultValues[2].value
    val stateStr = resultValues[3].value
    val statusStr = resultValues[4].value

    val hasFailFile = when (stateStr) {
        "MUTE", "" -> false
        "FAIL" -> true
        else -> throw ParseError("Invalid state (`$stateStr`), MUTE, FAIL or empty are expected: $str")
    }
    val isFlaky = when (statusStr) {
        "FLAKY" -> true
        "" -> false
        else -> throw ParseError("Invalid status (`$statusStr`), FLAKY or empty are expected: $str")
    }

    return MutedTest(testKey, issue, hasFailFile, isFlaky)
}

private class ParseError(message: String, override val cause: Throwable? = null) : IllegalArgumentException(message)

internal fun flakyTests(file: File) = loadMutedTests(file).filter { it.isFlaky }