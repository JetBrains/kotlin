/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

data class FrontendStats(
    val allNodesCount: Int,
    val leafNodesCount: Int,
    val starImportsCount: Int,
) {
    companion object {
        val EMPTY = FrontendStats(0, 0, 0)
    }

    operator fun plus(other: FrontendStats): FrontendStats = FrontendStats(
        allNodesCount + other.allNodesCount,
        leafNodesCount + other.leafNodesCount,
        starImportsCount + other.starImportsCount
    )
}

data class OldBackendStats(
    val allNodesCount: Int,
    val leafNodesCount: Int,
) {
    companion object {
        val EMPTY = OldBackendStats(0, 0)
    }

    operator fun plus(other: OldBackendStats): OldBackendStats = OldBackendStats(
        allNodesCount + other.allNodesCount,
        leafNodesCount + other.leafNodesCount,
    )
}

annotation class Serializable

fun PerformanceMeasurementResult.getJson(trailingComma: Boolean = false): String {
    return buildString {
        appendInternal("{", indent = 0, trailingComma = false)

        appendKeyValue("moduleName", moduleName)
        if (platform != PlatformType.JVM) {
            appendKeyValue("platform", platform)
        }
        if (!isK2) {
            appendKeyValue("isK2", false)
        }
        if (hasErrors) {
            appendKeyValue("hasErrors", true)
        }

        if (initStats != null) {
            appendOpening("initStats", 1)
            initStats.apply {
                appendKeyValue("filesCount", filesCount, indent = 2)
                appendKeyValue("linesCount", linesCount, indent = 2)
                appendTime(time, indent = 2, trailingComma = false)
            }
            appendClosing(1, trailingComma = true)
        }

        if (firStats != null) {
            appendOpening("firStats", 1)
            firStats.apply {
                appendKeyValue("allNodesCount", allNodesCount, indent = 2)
                appendKeyValue("leafNodesCount", leafNodesCount, indent = 2)
                appendKeyValue("starImportsCount", starImportsCount, indent = 2)
                appendTime(time, 2, trailingComma = false)
            }
            appendClosing(1, trailingComma = true)
        }

        irStats?.let { appendIrStats("irStats", it) }
        irLoweringStats?.let { appendIrStats("irLoweringStats", it) }

        backendStats?.let { appendBinaryStats("backendStats", it) }

        findJavaClassStats?.let { appendBinaryStats("findJavaClassStats", it) }
        findBinaryClassStats?.let { appendBinaryStats("findBinaryClassStats", it) }

        appendKeyValue("jitTimeMilliseconds", jitTimeMilliseconds)

        appendOpening("gcStats", 1)
        gcStats.apply {
            appendKeyValue("kind", kind, indent = 2)
            appendKeyValue("count", count, indent = 2)
            appendKeyValue("milliseconds", milliseconds, indent = 2, trailingComma = false)
        }
        appendClosing(1, trailingComma = false)

        appendClosing(indent = 0, trailingComma = trailingComma)
    }
}

private val indentCache = mutableMapOf<Int, String>()

private fun StringBuilder.appendIrStats(key: String, irStats: IrStats, indent: Int = 1, trailingComma: Boolean = true) {
    appendOpening(key, indent)
    irStats.apply {
        appendKeyValue("allNodesCountAfter", allNodesCountAfter, indent + 1)
        appendKeyValue("leafNodesCountAfter", leafNodesCountAfter, indent + 1)
        appendTime(time, indent + 1, trailingComma = false)
    }
    appendInternal("}", indent, trailingComma = trailingComma)
}

private fun StringBuilder.appendBinaryStats(key: String, binaryStats: BinaryStats, indent: Int = 1, trailingComma: Boolean = true) {
    appendOpening(key, indent)
    binaryStats.apply {
        appendKeyValue("count", count, indent + 1)
        appendKeyValue("bytesCount", bytesCount, indent + 1)
        appendTime(time, indent + 1, trailingComma = false)
    }
    appendInternal("}", indent, trailingComma = trailingComma)
}

private fun StringBuilder.appendKeyValue(key: String, value: Any, indent: Int = 1, trailingComma: Boolean = true) {
    val valueInfix = if (value is String) "\"$value\"" else value.toString()
    appendInternal("\"$key\": $valueInfix", indent, trailingComma)
}

private fun StringBuilder.appendTime(time: Time, indent: Int, trailingComma: Boolean) {
    appendOpening("time", indent)
    appendKeyValue("nanoseconds", time.nanoseconds, indent + 1)
    appendKeyValue("userTimeNanoseconds", time.userTimeNanoseconds, indent + 1)
    appendKeyValue("cpuTimeNanoseconds", time.cpuTimeNanoseconds, indent + 1, trailingComma = false)
    appendClosing(indent, trailingComma)
}

private fun StringBuilder.appendOpening(key: String, indent: Int) {
    appendInternal("\"$key\": {", indent, trailingComma = false)
}

private fun StringBuilder.appendClosing(indent: Int, trailingComma: Boolean) {
    appendInternal("}", indent, trailingComma)
}

private fun StringBuilder.appendInternal(s: String, indent: Int, trailingComma: Boolean = true) {
    append(indentCache.getOrPut(indent) { " ".repeat(indent * 2) })
    append(s)
    if (trailingComma) {
        append(',')
    }
    append('\n')
}