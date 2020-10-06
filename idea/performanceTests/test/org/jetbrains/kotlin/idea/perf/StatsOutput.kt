/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import org.jetbrains.kotlin.idea.perf.util.TeamCity
import org.jetbrains.kotlin.idea.testFramework.suggestOsNeutralFileName
import java.io.BufferedWriter
import java.io.File

internal fun List<Metric>.writeCSV(name: String, header: Array<String>) {
    fun Metric.append(prefix: String, output: BufferedWriter) {
        val s = "$prefix ${this.name}".trim()
        output.appendLine("$s,${value ?: ""},")
        children.forEach {
            it.append(s, output)
        }
    }

    val statsFile = statsFile(name, "csv")
    statsFile.bufferedWriter().use { output ->
        output.appendLine(header.joinToString())
        forEach { it.append("$name:", output) }
        output.flush()
    }
}

internal fun Metric.writeTeamCityStats(name: String, rawMeasurementName: String = "raw_metrics", rawMetrics: Boolean = false) {
    fun Metric.append(prefix: String, depth: Int) {
        val s = if (this.name.isEmpty()) {
            prefix
        } else {
            if (depth == 0 && this.name != Stats.GEOM_MEAN) "$prefix: ${this.name}" else "$prefix ${this.name}"
        }.trim()
        if (s != prefix) {
            value?.let {
                TeamCity.statValue(s, it)
            }
        }
        for (childIndex in children.withIndex()) {
            if (!rawMetrics && childrenName == rawMeasurementName && childIndex.index > 0) break
            childIndex.value.append(s, depth + 1)
        }
    }

    append(name, 0)
}

internal fun Metric.writeJson() {
    val statsFile = statsFile(name, "json")
    statsFile.bufferedWriter().use { output ->
        output.appendLine(toJson(""))
        output.flush()
    }
}

private fun statsFile(name: String, extension: String) =
    File(pathToResource("stats${statFilePrefix(name)}.$extension")).absoluteFile

private fun List<Metric>.toJson(prefix: String) = joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson(prefix) }

private fun String.jsonValue() = replace("\"", "\\\"")

private fun Metric.toJson(prefix: String): String =
    buildString {
        append("{")
        val nam = if (name.isEmpty()) "_value" else name
        val nm = nam.jsonValue()
        val s = if (name == Stats.GEOM_MEAN) {
            "${prefix.substring(0, prefix.length - 1)} $name".trim()
        } else {
            "$prefix $name${if (prefix.isEmpty()) ":" else ""}".trim()
        }

        var commaRequired = false
        if (value != null) {
            append("\"metric_name\":\"").append(nm).append("\"")
            append(",\"metric_value\":").append(value)
            measurementError?.let {
                append(",\"metric_error\":").append(it)
            }
            append(",\"legacy_name\":").append("\"${s.jsonValue()}\"")
            commaRequired = true
        }

        if (properties != null) {
            for ((k, v) in properties) {
                if (commaRequired) append(",")
                append("\"").append(k).append("\":")
                if (v is Number) {
                    append(v)
                } else {
                    append("\"").append(v).append("\"")
                }
                commaRequired = true
            }
        }

        if (children.isNotEmpty()) {
            val cnm = childrenName.jsonValue()
            if (commaRequired) append(",")
            append("\"").append(cnm).append("\"")
            append(":").append(children.toJson(s))
        }
        append("}")
    }


internal fun pathToResource(resource: String) = "build/$resource"

internal fun statFilePrefix(name: String) = if (name.isNotEmpty()) "-${plainname(name)}" else ""

internal fun plainname(name: String) = suggestOsNeutralFileName(name)
