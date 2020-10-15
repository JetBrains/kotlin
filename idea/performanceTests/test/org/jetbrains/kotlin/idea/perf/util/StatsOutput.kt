/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf.util

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.jetbrains.kotlin.idea.perf.Stats
import org.jetbrains.kotlin.idea.testFramework.suggestOsNeutralFileName
import java.io.BufferedWriter
import java.io.File

internal fun List<Metric>.writeCSV(name: String, header: Array<String>) {
    fun Metric.append(prefix: String, output: BufferedWriter) {
        val s = "$prefix ${this.metricName}".trim()
        output.appendLine("$s,${metricValue ?: ""},")
        metrics?.forEach {
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

internal fun Metric.writeTeamCityStats(name: String, rawMeasurementName: String = "rawMetrics", rawMetrics: Boolean = false) {
    fun Metric.append(prefix: String, depth: Int) {
        val s = if (this.metricName.isEmpty()) {
            prefix
        } else {
            if (depth == 0 && this.metricName != Stats.GEOM_MEAN) "$prefix: ${this.metricName}" else "$prefix ${this.metricName}"
        }.trim()
        if (s != prefix) {
            metricValue?.let {
                TeamCity.statValue(s, it)
            }
        }
        metrics?.let { list ->
            for (childIndex in list.withIndex()) {
                if (!rawMetrics && childIndex.index > 0) break
                childIndex.value.append(s, depth + 1)
            }
        }
    }

    append(name, 0)
}

internal val kotlinJsonMapper = jacksonObjectMapper()
    .registerKotlinModule()
    .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .configure(SerializationFeature.INDENT_OUTPUT, true)

internal fun Benchmark.statsFile() = statsFile(fileName(), "json")

internal fun Benchmark.writeJson() {
    val json = kotlinJsonMapper.writeValueAsString(this)
    val statsFile = statsFile()
    logMessage { "write $statsFile" }
    statsFile.bufferedWriter().use { output ->
        output.appendLine(json)
        output.flush()
    }
}

internal fun Benchmark.loadJson() {
    val statsFile = statsFile()
    if (statsFile.exists()) {
        val value = kotlinJsonMapper.readValue(statsFile, object : TypeReference<Benchmark>() {})
        metrics = value.metrics
    }
}

private fun statsFile(name: String, extension: String) =
    File(pathToResource("stats${statFilePrefix(name)}.$extension")).absoluteFile

internal fun pathToResource(resource: String) = "build/$resource"

internal fun statFilePrefix(name: String) = if (name.isNotEmpty()) "-${plainname(name)}" else ""

internal fun plainname(name: String) = suggestOsNeutralFileName(name)
