/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.cli.common.CommonCompilerPerformanceManager
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.util.PerformanceCounter
import org.jetbrains.kotlin.utils.addToStdlib.measureTimeMillisWithResult
import java.io.PrintStream
import java.nio.file.Files
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

abstract class AbstractFullPipelineParallelTest : AbstractFullPipelineModularizedTest() {

    private val executorService = Executors.newFixedThreadPool(System.getProperty("fir.bench.threads", "2").toInt())

    override fun processModule(moduleData: ModuleData): ProcessorAction {
        println("(${Thread.currentThread().name}) took ${moduleData.qualifiedName}")
        val compiler = K2JVMCompiler()
        val args = compiler.createArguments()
        val tmp = Files.createTempDirectory("compile-output")
        configureBaseArguments(args, moduleData, tmp)
        configureArguments(args, moduleData)

        val manager = CompilerPerformanceManager()
        val services = Services.Builder().register(CommonCompilerPerformanceManager::class.java, manager).build()
        val collector = TestMessageCollector()
        val result = try {
            CompilerSystemProperties.KOTLIN_COMPILER_ENVIRONMENT_KEEPALIVE_PROPERTY.value = "true"
            compiler.exec(collector, services, args)
        } catch (e: Exception) {
            e.printStackTrace()
            ExitCode.INTERNAL_ERROR
        }
        val resultTime = manager.reportCumulativeTime()
        PerformanceCounter.resetAllCounters()

        tmp.toFile().deleteRecursively()
        println("(${Thread.currentThread().name}) finished ${moduleData.qualifiedName}")
        synchronized(this) {
            if (result == ExitCode.OK) {
                totalPassResult += resultTime
            }

            return handleResult(result, moduleData, collector, manager.getTargetInfo(), resultTime)
        }
    }

    private val timePerModule = mutableMapOf<ModuleData, Long>()

    override fun processModules(modules: List<ModuleData>) {
        val resultSeq = modules
            .map { it to executorService.submit(Callable { processModule(it) }) }
            .progress(step = 0.0) { (module, _) -> "Analyzing ${module.qualifiedName}" }
            .asSequence()
            .map { (module, future) -> module to measureTimeMillisWithResult { future.get() } }
        for ((module, result) in resultSeq) {
            timePerModule[module] = result.first
            if (result.second.stop()) {
                break
            }
        }
    }

    override fun formatReport(stream: PrintStream, finalReport: Boolean) {
        formatReportTableNew(stream)
    }

    protected fun formatReportTableNew(stream: PrintStream) {
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
            row("Module", "Time", "Files", "L/S")
            separator()

            fun module(name: String, timeMs: Long, files: Int, lines: Int) {
                row {
                    cell(name, align = LEFT)
                    timeCell(timeMs, inputUnit = TableTimeUnit.MS)
                    cell(files.toString())
                    linePerSecondCell(lines, timeMs, timeUnit = TableTimeUnit.MS)
                }
            }

            for (module in totalModules) {
                module(module.data.qualifiedName, timePerModule[module.data]!!, module.time.files, module.time.lines)
            }

            separator()
            module("Total", timePerModule.values.sum(), total.files, total.lines)
        }
    }
    override fun afterAllPasses() {
        super.afterAllPasses()
        executorService.shutdown()
    }
}