/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.tasks.JavaExec
import org.gradle.api.Task
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import javax.inject.Inject
import java.io.File

open class RunJvmTask : JavaExec() {
    var outputFileName: String? = null
    @Input
    @Option(option = "filter", description = "Benchmarks to run (comma-separated)")
    var filter: String = ""
    @Input
    @Option(option = "filterRegex", description = "Benchmarks to run, described by regular expressions (comma-separated)")
    var filterRegex: String = ""

    override fun configure(configureClosure: Closure<Any>): Task {
        return super.configure(configureClosure)
    }

    private fun executeTask(output: java.io.OutputStream? = null) {
        val filterArgs = filter.splitCommaSeparatedOption("-f")
        val filterRegexArgs = filterRegex.splitCommaSeparatedOption("-fr")
        args(filterArgs)
        args(filterRegexArgs)
        exec()
    }

    @TaskAction
    fun run() {
        if (outputFileName != null)
            File(outputFileName).outputStream().use { output -> executeTask(output) }
        else
            executeTask()
    }
}
