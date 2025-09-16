/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.util.PerformanceManager
import org.jetbrains.kotlin.util.Time
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files


abstract class AbstractFullPipelineModularizedTest(config: ModularizedTestConfig) : AbstractModularizedTest(config) {

    private val asyncProfilerControl = AsyncProfilerControl()

    data class ModuleStatus(val data: ModuleData, val targetInfo: String) {
        var compilationError: String? = null
        var jvmInternalError: String? = null
        var exceptionMessage: String = "NO MESSAGE"
    }

    private val totalModules = mutableListOf<ModuleStatus>()
    private val okModules = mutableListOf<ModuleStatus>()
    private val errorModules = mutableListOf<ModuleStatus>()
    private val crashedModules = mutableListOf<ModuleStatus>()

    private val isolated = AbstractIsolatedFullPipelineModularizedTest(config)

    protected data class CumulativeTime(
        val gcInfo: Map<String, GCInfo>,
        val components: Map<String, Long>,
        val files: Int,
        val lines: Int
    ) {
        constructor() : this(emptyMap(), emptyMap(), 0, 0)

        operator fun plus(other: CumulativeTime): CumulativeTime {
            return CumulativeTime(
                (gcInfo.values + other.gcInfo.values).groupingBy { it.name }.reduce { key, accumulator, element ->
                    GCInfo(key, accumulator.gcTime + element.gcTime, accumulator.collections + element.collections)
                },
                (components.toList() + other.components.toList()).groupingBy { (name, _) -> name }.fold(0L) { a, b -> a + b.second },
                files + other.files,
                lines + other.lines
            )
        }

        fun totalTime() = components.values.sum()
    }

    protected lateinit var totalPassResult: CumulativeTime

    override fun beforePass(pass: Int) {
        totalPassResult = CumulativeTime()
        totalModules.clear()
        okModules.clear()
        errorModules.clear()
        crashedModules.clear()

        asyncProfilerControl.beforePass(pass, reportDateStr)
    }

    override fun afterPass(pass: Int) {
        asyncProfilerControl.afterPass(pass, reportDateStr)

        createReport(finalReport = pass == PASSES - 1)
        require(totalModules.isNotEmpty()) { "No modules were analyzed" }
        require(okModules.isNotEmpty()) { "All of $totalModules is failed" }
    }

    protected fun formatReportTable(stream: PrintStream) {
        val total = totalPassResult
        var totalGcTimeMs = 0L
        var totalGcCount = 0L
        printTable(stream) {
            row("Name", "Time", "Count")
            separator()
            fun gcRow(name: String, timeMs: Long, count: Long) {
                row {
                    cell(name, align = LEFT)
                    timeCell(timeMs, inputUnit = TableTimeUnit.MS)
                    cell(count.toString())
                }
            }
            for (measurement in total.gcInfo.values) {
                totalGcTimeMs += measurement.gcTime
                totalGcCount += measurement.collections
                gcRow(measurement.name, measurement.gcTime, measurement.collections)
            }
            separator()
            gcRow("Total", totalGcTimeMs, totalGcCount)

        }

        printTable(stream) {
            row("Phase", "Time", "Files", "L/S")
            separator()

            fun phase(name: String, timeMs: Long, files: Int, lines: Int) {
                row {
                    cell(name, align = LEFT)
                    timeCell(timeMs, inputUnit = TableTimeUnit.MS)
                    cell(files.toString())
                    linePerSecondCell(lines, timeMs, timeUnit = TableTimeUnit.MS)
                }
            }
            for (component in total.components) {
                phase(component.key, component.value, total.files, total.lines)
            }

            separator()
            phase("Total", total.totalTime(), total.files, total.lines)
        }

    }

    abstract fun configureArguments(args: K2JVMCompilerArguments, moduleData: ModuleData)

    protected open fun handleResult(result: ExitCode, moduleData: ModuleData, collector: TestMessageCollector, targetInfo: String): ProcessorAction {
        val status = ModuleStatus(moduleData, targetInfo)
        totalModules += status

        return when (result) {
            ExitCode.OK -> {
                okModules += status
                ProcessorAction.NEXT
            }
            ExitCode.COMPILATION_ERROR -> {
                errorModules += status
                status.compilationError = collector.messages.firstOrNull {
                    it.severity == CompilerMessageSeverity.ERROR
                }?.message
                status.jvmInternalError = collector.messages.firstOrNull {
                    it.severity == CompilerMessageSeverity.EXCEPTION
                }?.message
                ProcessorAction.NEXT
            }
            ExitCode.INTERNAL_ERROR -> {
                crashedModules += status
                status.exceptionMessage = collector.messages.firstOrNull {
                    it.severity == CompilerMessageSeverity.EXCEPTION
                }?.message?.split("\n")?.let { exceptionLines ->
                    exceptionLines.lastOrNull { it.startsWith("Caused by: ") } ?: exceptionLines.firstOrNull()
                } ?: "NO MESSAGE"
                ProcessorAction.NEXT
            }
            else -> ProcessorAction.NEXT
        }
    }


    private fun String.shorten(): String {
        val split = split("\n")
        return split.mapIndexedNotNull { index, s ->
            if (index < 4 || index >= split.size - 6) s else null
        }.joinToString("\n")
    }

    open fun formatReport(stream: PrintStream, finalReport: Boolean) {
        stream.println("TOTAL MODULES: ${totalModules.size}")
        stream.println("OK MODULES: ${okModules.size}")
        stream.println("FAILED MODULES: ${totalModules.size - okModules.size}")

        formatReportTable(stream)

        if (finalReport) {
            with(stream) {
                println()
                println("SUCCESSFUL MODULES")
                println("------------------")
                println()
                for (okModule in okModules) {
                    println("${okModule.data.qualifiedName}: ${okModule.targetInfo}")
                }
                println()
                println("COMPILATION ERRORS")
                println("------------------")
                println()
                for (errorModule in errorModules.filter { it.jvmInternalError == null }) {
                    println("${errorModule.data.qualifiedName}: ${errorModule.targetInfo}")
                    println("        1st error: ${errorModule.compilationError}")
                }
                println()
                println("JVM INTERNAL ERRORS")
                println("------------------")
                println()
                for (errorModule in errorModules.filter { it.jvmInternalError != null }) {
                    println("${errorModule.data.qualifiedName}: ${errorModule.targetInfo}")
                    println("        1st error: ${errorModule.jvmInternalError?.shorten()}")
                }
                val crashedModuleGroups = crashedModules.groupBy { it.exceptionMessage.take(60) }
                for (modules in crashedModuleGroups.values) {
                    println()
                    println(modules.first().exceptionMessage)
                    println("--------------------------------------------------------")
                    println()
                    for (module in modules) {
                        println("${module.data.qualifiedName}: ${module.targetInfo}")
                        println("        ${module.exceptionMessage}")
                    }
                }
            }
        }
    }

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val outputDir = Files.createTempDirectory("compile-output").toFile()
        val manager = CompilerPerformanceManager()
        val collector = TestMessageCollector()

        CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"
        val result = isolated.processModule(moduleData, outputDir, collector, manager) { arguments ->
            configureArguments(arguments, moduleData)
            manager.detailedPerf = arguments.detailedPerf
        }
        val resultTime = manager.reportCumulativeTime()
        PerformanceCounter.resetAllCounters()

        outputDir.deleteRecursively()
        if (result == ExitCode.OK) {
            totalPassResult = totalPassResult + resultTime
        }
        return handleResult(result, moduleData, collector, manager.getTargetInfo())
    }

    protected fun createReport(finalReport: Boolean) {
        formatReport(System.out, finalReport)

        PrintStream(
            FileOutputStream(
                reportDir().resolve("report-$reportDateStr.log"),
                true
            )
        ).use { stream ->
            formatReport(stream, finalReport)
            stream.println()
            stream.println()
        }
    }


    private class CompilerPerformanceManager : PerformanceManager(JvmPlatforms.defaultJvmPlatform, "Modularized test performance manager") {

        fun reportCumulativeTime(): CumulativeTime {
            val gcInfo = unitStats.gcStats.associate { it.kind to GCInfo(it.kind, it.millis, it.count) }

            val initTime = unitStats.let {
                (it.initStats ?: Time.ZERO) +
                        (it.findJavaClassStats?.time ?: Time.ZERO) +
                        (it.findKotlinClassStats?.time ?: Time.ZERO)
            }

            val components = buildMap {
                put("Init", initTime.millis)
                put("Analysis", unitStats.analysisStats?.millis ?: 0)
                unitStats.translationToIrStats?.millis?.let { put("Translation", it) }
                unitStats.irPreLoweringStats?.millis?.let { put("Pre-lowering", it) }
                unitStats.irSerializationStats?.millis?.let { put("Serialization", it) }
                unitStats.klibWritingStats?.millis?.let { put("Klib writing", it) }
                unitStats.irLoweringStats?.millis?.let { put("Lowering", it) }
                unitStats.backendStats?.millis?.let { put("Generation", it) }
            }

            return CumulativeTime(
                gcInfo,
                components,
                files,
                lines
            )
        }
    }

    protected class TestMessageCollector : MessageCollectorImpl() {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            super.report(severity, message, location)

            if (severity !in CompilerMessageSeverity.VERBOSE) {
                println(MessageRenderer.GRADLE_STYLE.render(severity, message, location))
            }
        }
    }
}
