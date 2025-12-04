/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics.json

import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.jetbrains.kotlin.build.report.metrics.BuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.BuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.CustomBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.CustomBuildTimeMetric
import org.jetbrains.kotlin.build.report.metrics.ValueType
import org.jetbrains.kotlin.build.report.metrics.allBuildTimeMetrics
import org.jetbrains.kotlin.build.report.metrics.allBuildPerformanceMetrics
import org.jetbrains.kotlin.build.report.metrics.getAllMetrics
import java.io.File
import java.lang.reflect.Type

val buildExecutionDataGson = GsonBuilder()
    .enableComplexMapKeySerialization()
    .registerTypeAdapter(File::class.java, object : JsonSerializer<File> {
        override fun serialize(src: File?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return src?.path?.let { JsonPrimitive(it) } ?: JsonNull.INSTANCE
        }
    })
    .registerTypeAdapter(File::class.java, object : JsonDeserializer<File> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): File? {
            val path = context?.deserialize<String>(json, String::class.java)
            return path?.let { File(it) }
        }
    })
    .registerTypeAdapter(BuildPerformanceMetric::class.java, object : JsonDeserializer<BuildPerformanceMetric> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): BuildPerformanceMetric? {
            val metricName = json?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) } ?: return null
            // Try to find an existing performance metric by name first
            val metric = getAllMetrics().firstOrNull { it.name == metricName }
            if (metric != null) return metric

            // Resolve parent as a BuildPerformanceMetric (not BuildTimeMetric)
            val parentMetricName =
                json.asJsonObject["parent"]?.asJsonObject?.get("name")?.let { context?.deserialize<String>(it, String::class.java) }
            val parentMetric = allBuildPerformanceMetrics.firstOrNull { it.name == parentMetricName }

            // Resolve ValueType for BuildPerformanceMetric
            val typeJson = json.asJsonObject["type"]
            val valueType: ValueType = if (typeJson != null) {
                context?.deserialize(typeJson, ValueType::class.java) ?: ValueType.NUMBER
            } else {
                ValueType.NUMBER
            }

            // Create or return a CustomBuildPerformanceMetric
            return CustomBuildPerformanceMetric.createIfDoesNotExistAndReturn(
                name = metricName,
                type = valueType,
                parent = parentMetric
            )
        }

    }).registerTypeAdapter(BuildTimeMetric::class.java, object : JsonDeserializer<BuildTimeMetric> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?,
        ): BuildTimeMetric? {
            val metricName = json?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) } ?: return null
            val metric = allBuildTimeMetrics.firstOrNull { it.name == metricName }
            if (metric != null) return metric

            val parentMetricName =
                json.asJsonObject["parent"]?.asJsonObject["name"]?.let { context?.deserialize<String>(it, String::class.java) }
            val parentMetric = allBuildTimeMetrics.firstOrNull { it.name == parentMetricName }

            return CustomBuildTimeMetric.createIfDoesNotExistAndReturn(name = metricName, parentMetric)
        }
    })
    .create()