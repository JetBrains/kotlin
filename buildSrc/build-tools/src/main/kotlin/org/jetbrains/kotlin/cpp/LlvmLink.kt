/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cpp

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.getByType
import org.gradle.process.ExecOperations
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import org.jetbrains.kotlin.execLlvmUtility
import org.jetbrains.kotlin.konan.target.PlatformManager
import javax.inject.Inject

private abstract class LlvmLinkJob : WorkAction<LlvmLinkJob.Parameters> {
    interface Parameters : WorkParameters {
        val inputFiles: ConfigurableFileCollection
        val outputFile: RegularFileProperty
        val arguments: ListProperty<String>
        val platformManager: Property<PlatformManager>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    override fun execute() {
        with(parameters) {
            execOperations.execLlvmUtility(platformManager.get(), "llvm-link") {
                args = listOf("-o", outputFile.asFile.get().absolutePath) + arguments.get() + inputFiles.map { it.absolutePath }
            }
        }
    }
}

/**
 * Run `llvm-link` on [inputFiles] with extra [arguments] and produce [outputFile]
 */
abstract class LlvmLink : DefaultTask() {
    /**
     * Bitcode files to link together.
     */
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    /**
     * Output file.
     */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    /**
     * Extra arguments for `llvm-link`.
     */
    @get:Input
    abstract val arguments: ListProperty<String>

    @get:Inject
    protected abstract val workerExecutor: WorkerExecutor

    private val platformManager = project.extensions.getByType<PlatformManager>()

    @TaskAction
    fun link() {
        val workQueue = workerExecutor.noIsolation()

        workQueue.submit(LlvmLinkJob::class.java) {
            inputFiles.from(this@LlvmLink.inputFiles)
            outputFile.set(this@LlvmLink.outputFile)
            arguments.set(this@LlvmLink.arguments)
            platformManager.set(this@LlvmLink.platformManager)
        }
    }
}