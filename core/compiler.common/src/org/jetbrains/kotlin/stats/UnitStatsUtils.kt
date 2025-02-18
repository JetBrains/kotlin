/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.stats

import org.jetbrains.kotlin.util.PhaseMeasurementType
import org.jetbrains.kotlin.util.PhaseSideMeasurementType
import org.jetbrains.kotlin.util.PlatformType
import org.jetbrains.kotlin.util.SideStats
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

            appendKeyValue("filesCount", filesCount, indent + 1)
            appendKeyValue("linesCount", linesCount, indent + 1)

            forEachPhaseMeasurement { phaseType, time ->
                if (time == null) return@forEachPhaseMeasurement

                val key = when (phaseType) {
                    PhaseMeasurementType.Initialization -> "initStats"
                    PhaseMeasurementType.Analysis -> "analysisStats"
                    PhaseMeasurementType.IrGeneration -> "irGenerationStats"
                    PhaseMeasurementType.IrLowering -> "irLoweringStats"
                    PhaseMeasurementType.BackendGeneration -> "backendStats"
                }

                appendTime(key, time, indent + 1, trailingComma = true)
            }

            forEachPhaseSideMeasurement { phaseType, sideStats ->
                if (sideStats == null) return@forEachPhaseSideMeasurement

                val key = when (phaseType) {
                    PhaseSideMeasurementType.FindJavaClass -> "findJavaClassStats"
                    PhaseSideMeasurementType.BinaryClassFromKotlinFile -> "findKotlinClassStats"
                }

                appendSideStats(key, sideStats, indent + 1, trailingComma = true)
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

private fun StringBuilder.appendSideStats(key: String, sideStats: SideStats, indent: Int = 1, trailingComma: Boolean = true) {
    appendObject(key, sideStats, indent, trailingComma) {
        sideStats.apply {
            appendKeyValue("count", count, indent + 1)
            appendTime("time", time, indent + 1, trailingComma = false)
        }
    }
}

private fun StringBuilder.appendTime(key: String, time: Time, indent: Int, trailingComma: Boolean) {
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