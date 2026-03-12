package org.jetbrains.kotlin.test.runner

import java.io.File

val String.asPathWithoutAllExtensions: String
    get() {
        val separatorLastIndex = lastIndexOf(File.separatorChar)
        var dotPreviousIndex: Int
        var dotIndex = length

        do {
            dotPreviousIndex = dotIndex
            dotIndex = lastIndexOf('.', dotPreviousIndex - 1)
        } while (
            dotIndex > separatorLastIndex && // it also handles `-1`
            !subSequence(dotIndex + 1, dotPreviousIndex).let { it.isNotEmpty() && it.all { c -> c.isDigit() } }
        )

        return substring(0, dotPreviousIndex)
    }

private val testNameReplacementRegex = "[.-]".toRegex()

fun String.toNormalizedTestName(): String =
    asPathWithoutAllExtensions
        .replaceFirstChar { it.uppercaseChar() }
        .replace(testNameReplacementRegex, "_")
