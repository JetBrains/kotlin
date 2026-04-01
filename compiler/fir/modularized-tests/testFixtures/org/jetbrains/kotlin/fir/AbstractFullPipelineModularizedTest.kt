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
import org.jetbrains.kotlin.stats.ModulesReportsData
import org.jetbrains.kotlin.stats.StatsCalculator
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.util.PerformanceManagerImpl
import org.jetbrains.kotlin.util.PhaseType
import org.jetbrains.kotlin.util.Time
import org.jetbrains.kotlin.util.UnitStats
import org.jetbrains.kotlin.util.forEachPhaseMeasurement
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

    private val passResults: MutableMap<String, UnitStats> = mutableMapOf()

    override fun beforePass(pass: Int) {
        passResults.clear()
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
        val totalStats = StatsCalculator(ModulesReportsData(passResults)).totalStats
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
            for (gcStats in totalStats.gcStats) {
                totalGcTimeMs += gcStats.millis
                totalGcCount += gcStats.count
                gcRow(gcStats.kind, gcStats.millis, gcStats.count)
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

            totalStats.forEachPhaseMeasurement { type, time ->
                val finalTime = when (type) {
                    PhaseType.Initialization -> {
                        // Append the extra time to the initialization time to preserve compatibility with the dashboard measurements
                        (time ?: Time.ZERO) + totalStats.findJavaClassStats?.time + totalStats.findKotlinClassStats?.time
                    }
                    PhaseType.Analysis -> {
                        // Always fill the Analysis time to make sure it doesn't break dashboard measurements
                        time ?: Time.ZERO
                    }
                    else -> {
                        time ?: return@forEachPhaseMeasurement
                    }
                }
                // Preserve old names and drop some new measurements for compatibility with the dashboard format
                val name = when (type) {
                    PhaseType.Initialization -> "Init"
                    PhaseType.Analysis -> "Analysis"
                    PhaseType.TranslationToIr -> "Translation"
                    PhaseType.IrPreLowering,
                    PhaseType.IrSerialization,
                    PhaseType.KlibWriting -> {
                        return@forEachPhaseMeasurement
                    }
                    PhaseType.IrLowering -> "Lowering"
                    PhaseType.Backend -> "Generation"
                }
                phase(name, finalTime.millis, totalStats.filesCount, totalStats.linesCount)
            }
            separator()
            phase("Total", totalStats.getTotalTime().millis, totalStats.filesCount, totalStats.linesCount)
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
        val manager = PerformanceManagerImpl(JvmPlatforms.defaultJvmPlatform, "Modularized test performance manager")
        val collector = TestMessageCollector()

        CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"
        val result = isolated.processModule(moduleData, outputDir, collector, manager) { arguments ->
            configureArguments(arguments, moduleData)
            manager.detailedPerf = arguments.detailedPerf
        }
        val resultStats = manager.unitStats
        PerformanceCounter.resetAllCounters()

        outputDir.deleteRecursively()
        if (result == ExitCode.OK) {
            // Use the compound key that include the `timestamp` because module names sometimes overlap
            val key = "${moduleData.name}-${moduleData.timestamp}"
            if (!passResults.containsKey(key)) {
                passResults[key] = resultStats
            } else {
                error("All keys should be unique, the '${key}' is duplicated")
            }
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

    protected class TestMessageCollector : MessageCollectorImpl() {
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            super.report(severity, message, location)

            if (severity !in CompilerMessageSeverity.VERBOSE) {
                println(MessageRenderer.GRADLE_STYLE.render(severity, message, location))
            }
        }
    }
}
