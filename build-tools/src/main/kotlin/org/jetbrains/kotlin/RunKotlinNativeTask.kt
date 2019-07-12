/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject

open class RunKotlinNativeTask @Inject constructor(private val linkTask: KotlinNativeLink,
                                                   private val outputFileName: String
) : DefaultTask() {
    @Input
    var buildType = "RELEASE"
    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""

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

    @TaskAction
    fun run() {
        var output = ByteArrayOutputStream()
        project.exec {
            it.executable = linkTask.binary.outputFile.absolutePath
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

        val results = benchmarksToRun.map { benchmark ->
            output = ByteArrayOutputStream()
            project.exec {
                it.executable = linkTask.binary.outputFile.absolutePath
                it.args(argumentsList)
                it.args("-f", benchmark)
                it.standardOutput = output
            }
            output.toString().removePrefix("[").removeSuffix("]")
        }

        File(outputFileName).printWriter().use { out ->
            out.println("[${results.joinToString(",")}]")
        }

    }
}
