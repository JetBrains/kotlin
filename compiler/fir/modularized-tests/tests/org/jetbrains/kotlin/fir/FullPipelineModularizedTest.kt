/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import com.intellij.util.io.delete
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.fir.TableTimeUnit.MS
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.util.PerformanceCounter
import java.io.FileOutputStream
import java.io.PrintStream
import java.nio.file.Files

class FullPipelineModularizedTest : AbstractModularizedTest() {
    override fun beforePass() {
        totalPassResult = CumulativeTime()
        totalModules = 0
        okModules = 0
    }

    private data class CumulativeTime(
        val gcInfo: Map<String, GCInfo>,
        val analysis: Long,
        val translation: Long,
        val generation: Long,
        val files: Int,
        val lines: Int
    ) {
        constructor() : this(emptyMap(), 0, 0, 0, 0, 0)

        operator fun plus(other: CumulativeTime): CumulativeTime {
            return CumulativeTime(
                (gcInfo.values + other.gcInfo.values).groupingBy { it.name }.reduce { key, accumulator, element ->
                    GCInfo(key, accumulator.gcTime + element.gcTime, accumulator.collections + element.collections)
                },
                analysis + other.analysis,
                translation + other.translation,
                generation + other.generation,
                files + other.files,
                lines + other.lines
            )
        }

        fun totalTime() = analysis + translation + generation
    }

    private lateinit var totalPassResult: CumulativeTime
    private var totalModules = 0
    private var okModules = 0

    override fun afterPass(pass: Int) {
        createReport()
        require(totalModules > 0) { "No modules were analyzed" }
        require(okModules > 0) { "All of $totalModules is failed" }
    }

    private fun createReport() {
        formatReport(System.out)

        PrintStream(
            FileOutputStream(
                reportDir().resolve("report-$reportDateStr.log"),
                true
            )
        ).use { stream ->
            formatReport(stream)
            stream.println()
            stream.println()
        }
    }

    private fun formatReport(stream: PrintStream) {
        stream.println("TOTAL MODULES: $totalModules")
        stream.println("OK MODULES: $okModules")
        val total = totalPassResult
        var totalGcTimeMs = 0L
        var totalGcCount = 0L
        printTable(stream) {
            row("Name", "Time", "Count")
            separator()
            fun gcRow(name: String, timeMs: Long, count: Long) {
                row {
                    cell(name, align = LEFT)
                    timeCell(timeMs, inputUnit = MS)
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
                    timeCell(timeMs, inputUnit = MS)
                    cell(files.toString())
                    linePerSecondCell(lines, timeMs, timeUnit = MS)
                }
            }
            phase("Analysis", total.analysis, total.files, total.lines)
            phase("Translation", total.translation, total.files, total.lines)
            phase("Generation", total.generation, total.files, total.lines)

            separator()
            phase("Total", total.totalTime(), total.files, total.lines)
        }
    }

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        val compiler = K2JVMCompiler()
        val args = compiler.createArguments()
        args.reportPerf = true
        args.jvmTarget = "1.8"
        args.useFir = true
        args.classpath = moduleData.classpath.joinToString(separator = ":") { it.absolutePath }
        args.javaSourceRoots = moduleData.javaSourceRoots.map { it.absolutePath }.toTypedArray()
        args.allowKotlinPackage = true
        args.freeArgs = moduleData.sources.map { it.absolutePath }
        val tmp = Files.createTempDirectory("compile-output")
        args.destination = tmp.toAbsolutePath().toFile().toString()
        val manager = CompilerPerformanceManager()
        val services = Services.Builder().register(CommonCompilerPerformanceManager::class.java, manager).build()
        val result = try {
            compiler.exec(PrintingMessageCollector(System.out, MessageRenderer.GRADLE_STYLE, false), services, args)
        } catch (e: Exception) {
            e.printStackTrace()
            ExitCode.INTERNAL_ERROR
        }
        val resultTime = manager.reportCumulativeTime()

        PerformanceCounter.resetAllCounters()

        tmp.delete(recursively = true)
        totalModules++

        return when (result) {
            ExitCode.OK -> {
                totalPassResult += resultTime
                okModules++
                ProcessorAction.NEXT
            }
            else -> ProcessorAction.NEXT
        }
    }


    private inner class CompilerPerformanceManager : CommonCompilerPerformanceManager("Modularized test performance manager") {
        fun reportCumulativeTime(): CumulativeTime {
            val gcInfo = measurements.filterIsInstance<GarbageCollectionMeasurement>()
                .associate { it.garbageCollectionKind to GCInfo(it.garbageCollectionKind, it.milliseconds, it.count) }

            val analysisMeasurement = measurements.filterIsInstance<CodeAnalysisMeasurement>().firstOrNull()
            val irMeasurements = measurements.filterIsInstance<IRMeasurement>()

            return CumulativeTime(
                gcInfo,
                analysisMeasurement?.milliseconds ?: 0,
                irMeasurements.firstOrNull { it.kind == IRMeasurement.Kind.TRANSLATION }?.milliseconds ?: 0,
                irMeasurements.firstOrNull { it.kind == IRMeasurement.Kind.GENERATION }?.milliseconds ?: 0,
                files ?: 0,
                lines ?: 0
            )
        }
    }

    fun testTotalKotlin() {
        for (i in 0 until PASSES) {
            println("Pass $i")
            runTestOnce(i)
        }
    }
}
