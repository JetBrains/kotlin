/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import org.jetbrains.kotlin.util.AnalysisStats
import org.jetbrains.kotlin.util.BinaryStats
import org.jetbrains.kotlin.util.InitStats
import org.jetbrains.kotlin.util.IrStats
import org.jetbrains.kotlin.util.PhaseMeasurementType
import org.jetbrains.kotlin.util.PhaseSideMeasurementType
import org.jetbrains.kotlin.util.PhaseStats
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import org.jetbrains.kotlin.util.forEachPhaseMeasurement
import org.jetbrains.kotlin.util.forEachPhaseSideMeasurement

fun UnitStats.toJson(indent: Int = 0, trailingComma: Boolean = false): String {
    return buildString {
        appendArrayElement(this, indent, trailingComma) {
            appendKeyValue("name", name ?: "", indent + 1)
            if (platform != PlatformType.JVM) {
                appendKeyValue("platform", platform, indent + 1)
            }
            if (!isK2) {
                appendKeyValue("isK2", value = false, indent + 1)
            }
            if (hasErrors) {
                appendKeyValue("hasErrors", value = true, indent + 1)
            }

            forEachPhaseMeasurement { phaseType, stats ->
                if (stats == null) return@forEachPhaseMeasurement
                appendPhaseStats(phaseType, stats, indent + 1, trailingComma = true)
            }

            forEachPhaseSideMeasurement { phaseType, sideStats ->
                if (sideStats == null) return@forEachPhaseSideMeasurement

                val key = when (phaseType) {
                    PhaseSideMeasurementType.FindJavaClass -> "findJavaClassStats"
                    PhaseSideMeasurementType.BinaryClassFromKotlinFile -> "findKotlinClassStats"
                }

                appendBinaryStats(key, sideStats, indent + 1, trailingComma = true)
            }

            if (gcStats.isNotEmpty()) {
                appendArray("gcStats", gcStats, indent + 1, trailingComma = true) { gcStat ->
                    appendKeyValue("kind", gcStat.kind, indent + 3)
                    appendKeyValue("count", gcStat.count, indent + 3)
                    appendKeyValue("millis", gcStat.millis, indent + 3, trailingComma = false)
                }
            }

            if (jitTimeMillis != null) {
                appendKeyValue("jitTimeMillis", jitTimeMillis, indent + 1)
            }

            if (endsWith(trailingCommaSuffix)) {
                replace(length - trailingCommaSuffix.length, length, "\n")
            }
        }
    }
}

private const val trailingCommaSuffix = ",\n"
private val indentCache = mutableListOf<String>()

private fun StringBuilder.appendPhaseStats(phaseType: PhaseMeasurementType, stats: PhaseStats<*>, indent: Int, trailingComma: Boolean) {
    val key = when (phaseType) {
        PhaseMeasurementType.Initialization -> "initStats"
        PhaseMeasurementType.Analysis -> "analysisStats"
        PhaseMeasurementType.IrGeneration -> "irGenerationStats"
        PhaseMeasurementType.IrLowering -> "irLoweringStats"
        PhaseMeasurementType.BackendGeneration -> "backendStats"
    }
    appendObject(key, stats, indent, trailingComma) {
        appendTime("time", stats.time, indent + 1)
        when (stats) {
            is InitStats -> {
                appendKeyValue("linesCount", stats.linesCount, indent + 1)
                appendKeyValue("filesCount", stats.filesCount, indent + 1, trailingComma = false)
            }
            is AnalysisStats -> {
                appendKeyValue("allNodesCount", stats.allNodesCount, indent + 1)
                appendKeyValue("leafNodesCount", stats.leafNodesCount, indent + 1)
                appendKeyValue("starImportsCount", stats.starImportsCount, indent + 1, trailingComma = false)
            }
            is IrStats -> {
                appendKeyValue("allNodesAfterCount", stats.allNodesAfterCount, indent + 1)
                appendKeyValue("leafNodesAfterCount", stats.leafNodesAfterCount, indent + 1, trailingComma = false)
            }
            is BinaryStats -> {
                appendKeyValue("count", stats.count, indent + 1)
                appendKeyValue("bytesCount", stats.bytesCount, indent + 1, trailingComma = false)
            }
        }
    }
}

private fun StringBuilder.appendBinaryStats(key: String, binaryStats: BinaryStats, indent: Int = 1, trailingComma: Boolean = true) {
    appendObject(key, binaryStats, indent, trailingComma) {
        binaryStats.apply {
            appendTime("time", time, indent + 1)
            appendKeyValue("count", count, indent + 1)
            appendKeyValue("bytesCount", bytesCount, indent + 1, trailingComma = false)
        }
    }
}

private fun StringBuilder.appendTime(key: String, time: Time, indent: Int, trailingComma: Boolean = true) {
    appendObject(key, time, indent, trailingComma) {
        appendKeyValue("nano", time.nano, indent + 1)
        appendKeyValue("userNano", time.userNano, indent + 1)
        appendKeyValue("cpuNano", time.cpuNano, indent + 1, trailingComma = false)
    }
}

private fun <T> StringBuilder.appendArray(key: String, array: List<T>, indent: Int = 1, trailingComma: Boolean, appendObjectContent: (T) -> Unit) {
    appendInternal("\"$key\": [", indent, trailingComma = false)
    for ((index, item) in array.withIndex()) {
        appendArrayElement(item, indent + 1, trailingComma = index < array.size - 1, appendObjectContent)
    }
    appendInternal("]", indent, trailingComma)
}

private fun <T> StringBuilder.appendArrayElement(value: T, indent: Int, trailingComma: Boolean, appendObjectContent: (T) -> Unit) {
    appendObject(key = null, value, indent, trailingComma, appendObjectContent)
}

private fun <T> StringBuilder.appendObject(key: String?, value: T, indent: Int, trailingComma: Boolean, appendObjectContent: (T) -> Unit) {
    if (key != null)
        appendInternal("\"$key\": {", indent, trailingComma = false)
    else
        appendInternal("{", indent, trailingComma = false)
    appendObjectContent(value)
    appendInternal("}", indent, trailingComma)
}

private fun StringBuilder.appendKeyValue(key: String, value: Any, indent: Int = 1, trailingComma: Boolean = true) {
    val valueInfix = if (value is String) "\"$value\"" else value.toString()
    appendInternal("\"$key\": $valueInfix", indent, trailingComma)
}

private fun StringBuilder.appendInternal(s: String, indent: Int, trailingComma: Boolean) {
    appendIndent(indent)
    append(s)
    if (trailingComma) {
        append(',')
    }
    append('\n')
}

private fun StringBuilder.appendIndent(indent: Int) {
    (indentCache.size ..indent).forEach { index ->
        indentCache.add(" ".repeat(index * 2))
    }

    append(indentCache[indent])
}