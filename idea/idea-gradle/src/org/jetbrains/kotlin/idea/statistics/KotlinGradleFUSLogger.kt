/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.statistics

import com.intellij.ide.highlighter.ProjectFileType
import com.intellij.ide.util.PropertiesComponent
import com.intellij.internal.statistic.eventLog.EventLogConfiguration
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtilRt
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.text.trimMiddle
import org.jetbrains.kotlin.statistics.BuildSessionLogger
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import org.jetbrains.kotlin.statistics.BuildSessionLogger.Companion.STATISTICS_FOLDER_NAME
import org.jetbrains.kotlin.statistics.fileloggers.MetricsContainer
import org.jetbrains.kotlin.statistics.metrics.BooleanMetrics
import org.jetbrains.kotlin.statistics.metrics.NumericalMetrics
import org.jetbrains.kotlin.statistics.metrics.StringMetrics
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class KotlinGradleFUSLogger : StartupActivity, DumbAware, Runnable {

    override fun runActivity(project: Project) {
        AppExecutorUtil.getAppScheduledExecutorService()
            .scheduleWithFixedDelay(this, EXECUTION_DELAY_MIN, EXECUTION_DELAY_MIN, TimeUnit.MINUTES)
    }

    override fun run() {
        reportStatistics()
    }

    companion object {

        private val IDE_STRING_ANONYMIZERS = lazy {
            mapOf(
                StringMetrics.PROJECT_PATH to { path: String ->
                    // This code duplicated logics of StatisticsUtil.getProjectId, which could not be directly reused:
                    // 1. the path of gradle project may not have corresponding project
                    // 2. the projectId should be stable and independent on IDE version
                    val presentableUrl = FileUtil.toSystemIndependentName(path)
                    val name =
                        PathUtilRt.getFileName(presentableUrl).lowercase().removeSuffix(ProjectFileType.DOT_DEFAULT_EXTENSION)
                    val locationHash = Integer.toHexString((presentableUrl).hashCode())
                    val projectHash =
                        "${name.trimMiddle(name.length.coerceAtMost(254 - locationHash.length), useEllipsisSymbol = false)}.$locationHash"
                    EventLogConfiguration.anonymize(projectHash)
                })
        }

        private fun String.anonymizeIdeString(metric: StringMetrics) = if (metric.anonymization.anonymizeOnIdeSize())
            IDE_STRING_ANONYMIZERS.value[metric]?.invoke(this)
        else
            this

        /**
         * Maximum amount of directories which were reported as gradle user dirs
         * These directories should be monitored for reported gradle statistics.
         */
        const val MAXIMUM_USER_DIRS = 10

        /**
         * Delay between sequential checks of gradle statistics
         */
        const val EXECUTION_DELAY_MIN = 60L

        /**
         * Property name used for persisting gradle user dirs
         */
        private const val GRADLE_USER_DIRS_PROPERTY_NAME = "kotlin-gradle-user-dirs"

        private val isRunning = AtomicBoolean(false)

        private fun MetricsContainer.log(event: GradleStatisticsEvents, vararg metrics: Any) {
            val data = HashMap<String, String>()
            fun putIfNotNull(key: String, value: String?) {
                if (value != null) {
                    data[key.lowercase()] = value
                }
            }

            for (metric in metrics) {
                when (metric) {
                    is BooleanMetrics -> putIfNotNull(metric.name, this.getMetric(metric)?.toStringRepresentation())
                    is StringMetrics -> putIfNotNull(
                        metric.name,
                        this.getMetric(metric)?.toStringRepresentation()?.anonymizeIdeString(metric)
                    )
                    is NumericalMetrics -> putIfNotNull(metric.name, this.getMetric(metric)?.toStringRepresentation())
                    is Pair<*, *> -> putIfNotNull(metric.first.toString(), metric.second?.toString())
                }
            }
            if (data.size > 0) {
                KotlinFUSLogger.log(FUSEventGroups.GradlePerformance, event.name, data)
            }
        }

        private fun processMetricsContainer(container: MetricsContainer, previous: MetricsContainer?) {
            container.log(
                GradleStatisticsEvents.Environment,
                NumericalMetrics.CPU_NUMBER_OF_CORES,
                StringMetrics.GRADLE_VERSION,
                NumericalMetrics.ARTIFACTS_DOWNLOAD_SPEED,
                StringMetrics.IDES_INSTALLED,
                BooleanMetrics.EXECUTED_FROM_IDEA,
                StringMetrics.PROJECT_PATH
            )
            container.log(
                GradleStatisticsEvents.Kapt,
                BooleanMetrics.ENABLED_KAPT,
                BooleanMetrics.ENABLED_DAGGER,
                BooleanMetrics.ENABLED_DATABINDING
            )

            container.log(
                GradleStatisticsEvents.CompilerPlugins,
                BooleanMetrics.ENABLED_COMPILER_PLUGIN_ALL_OPEN,
                BooleanMetrics.ENABLED_COMPILER_PLUGIN_NO_ARG,
                BooleanMetrics.ENABLED_COMPILER_PLUGIN_JPA_SUPPORT,
                BooleanMetrics.ENABLED_COMPILER_PLUGIN_SAM_WITH_RECEIVER,
                BooleanMetrics.JVM_COMPILER_IR_MODE,
                StringMetrics.JVM_DEFAULTS,
                StringMetrics.USE_OLD_BACKEND
            )

            container.log(
                GradleStatisticsEvents.JS,
                BooleanMetrics.JS_GENERATE_EXTERNALS,
                StringMetrics.JS_GENERATE_EXECUTABLE_DEFAULT,
                StringMetrics.JS_TARGET_MODE,
                BooleanMetrics.JS_SOURCE_MAP,
                StringMetrics.JS_PROPERTY_LAZY_INITIALIZATION,
                BooleanMetrics.JS_KLIB_INCREMENTAL,
                BooleanMetrics.JS_IR_INCREMENTAL,
            )

            container.log(
                GradleStatisticsEvents.MPP,
                StringMetrics.MPP_PLATFORMS,
                BooleanMetrics.ENABLED_HMPP,
                StringMetrics.JS_COMPILER_MODE
            )

            container.log(
                GradleStatisticsEvents.Libraries,
                StringMetrics.LIBRARY_SPRING_VERSION,
                StringMetrics.LIBRARY_VAADIN_VERSION,
                StringMetrics.LIBRARY_GWT_VERSION,
                StringMetrics.LIBRARY_HIBERNATE_VERSION
            )

            container.log(
                GradleStatisticsEvents.GradleConfiguration,
                NumericalMetrics.GRADLE_DAEMON_HEAP_SIZE,
                NumericalMetrics.GRADLE_BUILD_NUMBER_IN_CURRENT_DAEMON,
                NumericalMetrics.CONFIGURATION_API_COUNT,
                NumericalMetrics.CONFIGURATION_IMPLEMENTATION_COUNT,
                NumericalMetrics.CONFIGURATION_COMPILE_COUNT,
                NumericalMetrics.CONFIGURATION_RUNTIME_COUNT,
                NumericalMetrics.GRADLE_NUMBER_OF_TASKS,
                NumericalMetrics.GRADLE_NUMBER_OF_UNCONFIGURED_TASKS,
                NumericalMetrics.GRADLE_NUMBER_OF_INCREMENTAL_TASKS
            )

            container.log(
                GradleStatisticsEvents.ComponentVersions,
                StringMetrics.KOTLIN_COMPILER_VERSION,
                StringMetrics.KOTLIN_STDLIB_VERSION,
                StringMetrics.KOTLIN_REFLECT_VERSION,
                StringMetrics.KOTLIN_COROUTINES_VERSION,
                StringMetrics.KOTLIN_SERIALIZATION_VERSION,
                StringMetrics.ANDROID_GRADLE_PLUGIN_VERSION
            )

            container.log(
                GradleStatisticsEvents.KotlinFeatures,
                StringMetrics.KOTLIN_LANGUAGE_VERSION,
                StringMetrics.KOTLIN_API_VERSION,
                BooleanMetrics.BUILD_SRC_EXISTS,
                NumericalMetrics.BUILD_SRC_COUNT,
                BooleanMetrics.GRADLE_BUILD_CACHE_USED,
                BooleanMetrics.GRADLE_WORKER_API_USED,
                BooleanMetrics.KOTLIN_OFFICIAL_CODESTYLE,
                BooleanMetrics.KOTLIN_PROGRESSIVE_MODE,
                BooleanMetrics.KOTLIN_KTS_USED
            )

            container.log(
                GradleStatisticsEvents.GradlePerformance,
                NumericalMetrics.GRADLE_BUILD_DURATION,
                NumericalMetrics.GRADLE_EXECUTION_DURATION,
                NumericalMetrics.NUMBER_OF_SUBPROJECTS,
                NumericalMetrics.STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD,
                NumericalMetrics.STATISTICS_COLLECT_METRICS_OVERHEAD
            )

            val finishTime = container.getMetric(NumericalMetrics.BUILD_FINISH_TIME)?.getValue()
            val prevFinishTime = previous?.getMetric(NumericalMetrics.BUILD_FINISH_TIME)?.getValue()

            val betweenBuilds = if (finishTime != null && prevFinishTime != null) finishTime - prevFinishTime else null
            container.log(
                GradleStatisticsEvents.UseScenarios,
                Pair("time_between_builds", betweenBuilds),
                BooleanMetrics.DEBUGGER_ENABLED,
                BooleanMetrics.COMPILATION_STARTED,
                BooleanMetrics.TESTS_EXECUTED,
                BooleanMetrics.MAVEN_PUBLISH_EXECUTED,
                BooleanMetrics.BUILD_FAILED
            )
        }

        fun reportStatistics() {
            if (isRunning.compareAndSet(false, true)) {
                try {
                    for (gradleUserHome in gradleUserDirs) {
                        BuildSessionLogger.listProfileFiles(File(gradleUserHome, STATISTICS_FOLDER_NAME))?.forEach { statisticFile ->
                            var fileWasRead = true
                            try {
                                var previousEvent: MetricsContainer? = null
                                fileWasRead = MetricsContainer.readFromFile(statisticFile) { metricContainer ->
                                    processMetricsContainer(metricContainer, previousEvent)
                                    previousEvent = metricContainer
                                }
                            } catch (e: Exception) {
                                Logger.getInstance(KotlinFUSLogger::class.java)
                                    .info("Failed to process file ${statisticFile.absolutePath}: ${e.message}", e)
                            } finally {
                                if (fileWasRead && !statisticFile.delete()) {
                                    Logger.getInstance(KotlinFUSLogger::class.java)
                                        .warn("[FUS] Failed to delete file ${statisticFile.absolutePath}")
                                }
                            }
                        }
                    }
                } finally {
                    isRunning.set(false)
                }
            }
        }

        private var gradleUserDirs: Array<String>
            set(value) = PropertiesComponent.getInstance().setValues(
                GRADLE_USER_DIRS_PROPERTY_NAME, value
            )
            get() = PropertiesComponent.getInstance().getValues(GRADLE_USER_DIRS_PROPERTY_NAME) ?: emptyArray()

        fun populateGradleUserDir(path: String) {
            val currentState = gradleUserDirs
            if (path in currentState) return

            val result = ArrayList<String>()
            result.add(path)
            result.addAll(currentState)

            gradleUserDirs =
                result.filter { filePath -> File(filePath).exists() }.take(MAXIMUM_USER_DIRS).toTypedArray()
        }
    }
}