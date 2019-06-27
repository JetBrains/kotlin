/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import javax.inject.Inject

open class RunKotlinNativeTask @Inject constructor(
        private val runTask: AbstractExecTask<*>
) : DefaultTask() {
    @Input
    var buildType = "RELEASE"
    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""

    override fun configure(configureClosure: Closure<Any>): Task {
        val task = super.configure(configureClosure)
        this.finalizedBy(runTask.name)
        runTask.finalizedBy("konanJsonReport")
        return task
    }

    fun depends(taskName: String) {
        this.dependsOn += taskName
    }

    @TaskAction
    fun run() {
        runTask.run {
            val filterArgs = filter.splitCommaSeparatedOption("-f")
            val filterRegexArgs = filterRegex.splitCommaSeparatedOption("-fr")
            runTask.args(filterArgs)
            runTask.args(filterRegexArgs)
        }
    }

    internal fun emptyConfigureClosure() = object : Closure<Any>(this) {
        override fun call(): RunKotlinNativeTask {
            return this@RunKotlinNativeTask
        }
    }
}
