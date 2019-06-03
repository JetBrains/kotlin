/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import javax.inject.Inject
import java.io.File

open class RunBenchmarksExecutableTask @Inject constructor() : DefaultTask() {
    var workingDir: Any = project.projectDir
    var outputFileName: String? = null
    var executable: String? = null
    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""

    private var curArgs: List<String> = emptyList()
    private val curEnvironment: MutableMap<String, Any> = mutableMapOf()

    fun args(vararg args: Any) {
        curArgs = args.map { it.toString() }
    }

    fun environment(map: Map<String, Any>) {
        curEnvironment += map
    }

    override fun configure(configureClosure: Closure<Any>): Task {
        val task = super.configure(configureClosure)
        return task
    }

    fun depends(taskName: String) {
        this.dependsOn += taskName
    }

    private fun executeTask(output: java.io.OutputStream? = null) {
        val filterArgs = filter.splitCommaSeparatedOption("-f")
        val filterRegexArgs = filterRegex.splitCommaSeparatedOption("-fr")
        project.exec {
            it.executable = executable
            it.args = curArgs + filterArgs + filterRegexArgs
            it.environment = curEnvironment
            it.workingDir(workingDir)
            if (output != null)
                it.standardOutput = output
        }
    }

    @TaskAction
    fun run() {
        if (outputFileName != null)
            File(outputFileName).outputStream().use { output -> executeTask(output) }
        else
            executeTask()
    }

    internal fun emptyConfigureClosure() = object : Closure<Any>(this) {
        override fun call(): RunBenchmarksExecutableTask {
            return this@RunBenchmarksExecutableTask
        }
    }
}