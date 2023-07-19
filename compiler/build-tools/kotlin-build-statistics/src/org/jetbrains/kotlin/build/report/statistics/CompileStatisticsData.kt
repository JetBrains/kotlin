/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.statistics

import org.jetbrains.kotlin.build.report.metrics.*
import java.text.SimpleDateFormat
import java.util.*

//Sensitive data. This object is used directly for statistic via http
private val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").also { it.timeZone = TimeZone.getTimeZone("UTC") }

interface CompileStatisticsData<B : BuildTime, P : BuildPerformanceMetric> {
    fun getVersion(): Int = 4
    fun getProjectName(): String?
    fun getLabel(): String?
    fun getTaskName(): String
    fun getTaskResult(): String?
    fun getStartTimeMs(): Long
    fun getDurationMs(): Long
    fun getTags(): Set<StatTag>
    fun getChanges(): List<String>
    fun getBuildUuid(): String = "Unset"
    fun getKotlinVersion(): String
    fun getKotlinLanguageVersion(): String?
    fun getHostName(): String? = "Unset"
    fun getFinishTime(): Long
    fun getTimestamp(): String = formatter.format(getFinishTime())
    fun getCompilerArguments(): List<String>
    fun getNonIncrementalAttributes(): Set<BuildAttribute>

    //TODO think about it,time in milliseconds
    fun getBuildTimesMetrics(): Map<B, Long>
    fun getPerformanceMetrics(): Map<P, Long>
    fun getGcTimeMetrics(): Map<String, Long>?
    fun getGcCountMetrics(): Map<String, Long>?
    fun getType(): String = BuildDataType.TASK_DATA.name
    fun getFromKotlinPlugin(): Boolean?
    fun getCompiledSources(): List<String> = emptyList()
    fun getSkipMessage(): String?
    fun getIcLogLines(): List<String>
}

enum class StatTag(val readableString: String) {
    ABI_SNAPSHOT("ABI Snapshot"),
    ARTIFACT_TRANSFORM("Classpath Snapshot"),
    INCREMENTAL("Incremental compilation"),
    NON_INCREMENTAL("Non incremental compilation"),
    INCREMENTAL_AND_NON_INCREMENTAL("Incremental and Non incremental compilation"),
    GRADLE_DEBUG("Gradle debug enabled"),
    KOTLIN_DEBUG("Kotlin debug enabled"),
    CONFIGURATION_CACHE("Configuration cache enabled"),
    BUILD_CACHE("Build cache enabled"),
    KOTLIN_1("Kotlin language version 1"),
    KOTLIN_2("Kotlin language version 2"),
    KOTLIN_1_AND_2("Kotlin language version 1 and 2"),
}

enum class BuildDataType {
    TASK_DATA,
    BUILD_DATA,
    JPS_DATA
}

//Sensitive data. This object is used directly for statistic via http
data class BuildStartParameters(
    val tasks: List<String>,
    val excludedTasks: Set<String> = emptySet(),
    val currentDir: String? = null,
    val projectProperties: List<String> = emptyList(),
    val systemProperties: List<String> = emptyList(),
) : java.io.Serializable

//Sensitive data. This object is used directly for statistic via http
data class BuildFinishStatisticsData(
    val projectName: String,
    val startParameters: BuildStartParameters,
    val buildUuid: String = "Unset",
    val label: String?,
    val totalTime: Long,
    val type: String = BuildDataType.BUILD_DATA.name,
    val finishTime: Long,
    val timestamp: String = formatter.format(finishTime),
    val hostName: String? = "Unset",
    val tags: Set<StatTag>,
    val gitBranch: String = "Unset",
)



