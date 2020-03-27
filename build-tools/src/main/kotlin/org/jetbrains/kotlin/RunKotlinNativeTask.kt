/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.jetbrains.report.json.*
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

open class RunKotlinNativeTask @Inject constructor(private val linkTask: Task,
                                                   private val executable: String,
                                                   private val outputFileName: String
) : DefaultTask() {
    enum class RepeatingType {
        INTERNAL,  // Let the benchmark perform warmups and repeats.
        EXTERNAL,  // Repeat by relaunching benchmark
    }

    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""
    @Input
    @Option(option = "verbose", description = "Verbose mode of running benchmarks")
    var verbose: Boolean = false
    @Input
    var warmupCount: Int = 0
    @Input
    var repeatCount: Int = 0
    @Input
    var repeatingType = RepeatingType.INTERNAL

    private val argumentsList = mutableListOf<String>()

    init {
        this.dependsOn += linkTask.name
        this.finalizedBy("konanJsonReport")
    }

    fun depends(taskName: String) {
        this.dependsOn += taskName
    }

    fun args(vararg arguments: String) {
        argumentsList.addAll(arguments.toList())
    }

    private fun execBenchmarkOnce(benchmark: String, warmupCount: Int, repeatCount: Int) : String {
        val output = ByteArrayOutputStream()
        project.exec {
            it.executable = executable
            it.args(argumentsList)
            it.args("-f", benchmark)
            if (verbose) {
                it.args("-v")
            }
            it.args("-w", warmupCount.toString())
            it.args("-r", repeatCount.toString())
            it.standardOutput = output
        }
        return output.toString().removePrefix("[").removeSuffix("]")
    }

    private fun execBenchmarkRepeatedly(benchmark: String, warmupCount: Int, repeatCount: Int) : List<String> {
        for (i in 0..warmupCount) {
            execBenchmarkOnce(benchmark, 0, 1)
        }
        val result = mutableListOf<String>()
        for (i in 0..repeatCount) {
            val benchmarkReport = JsonTreeParser.parse(execBenchmarkOnce(benchmark, 0, 1)).jsonObject
            val modifiedBenchmarkReport = JsonObject(HashMap(benchmarkReport.content).apply {
                put("repeat", JsonLiteral(i))
                put("warmup", JsonLiteral(warmupCount))
            })
            result.add(modifiedBenchmarkReport.toString())
        }
        return result
    }

    @TaskAction
    fun run() {
        val output = ByteArrayOutputStream()
        project.exec {
            it.executable = executable
            it.args("list")
            it.standardOutput = output
        }
        val benchmarks = output.toString().lines()
        val filterArgs = filter.splitCommaSeparatedOption("-f")
        val filterRegexArgs = filterRegex.splitCommaSeparatedOption("-fr")
        val regexes = filterRegexArgs.map { it.toRegex() }
        val benchmarksToRun = if (filterArgs.isNotEmpty() || regexes.isNotEmpty()) {
            benchmarks.filter { benchmark -> benchmark in filterArgs || regexes.any { it.matches(benchmark) } }
        } else benchmarks.filter { !it.isEmpty() }

        val results = benchmarksToRun.flatMap { benchmark ->
            when (repeatingType) {
                RepeatingType.INTERNAL -> listOf(execBenchmarkOnce(benchmark, warmupCount, repeatCount))
                RepeatingType.EXTERNAL -> execBenchmarkRepeatedly(benchmark, warmupCount, repeatCount)
            }
        }

        File(outputFileName).printWriter().use { out ->
            out.println("[${results.joinToString(",")}]")
        }

    }
}
