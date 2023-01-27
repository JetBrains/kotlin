/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.konan.target.*
import java.io.File
import java.nio.file.Path

/**
 * Gradle task that wraps FileCheck LLVM utility.
 */
open class FileCheckTest : DefaultTask() {
    /**
     * File annotated with FileCheck directives.
     */
    @InputFile
    lateinit var annotatedSource: File

    /**
     * LLVM IR that should match [annotatedSource].
     */
    @get:Internal
    lateinit var llvmIr: File

    /**
     * Optional cinterop task dependency.
     */
    @get:Optional
    @get:Input
    var interop: String? = null

    @TaskAction
    fun run() {
        runFileCheck(annotatedSource.toPath(), llvmIr.toPath())
    }

    /**
     * What prefix should checked for pattern instead of default CHECK?
     */
    @get:Input
    @get:Optional
    var checkPrefix: String? = null

    /**
     * Compiler pipeline phase name, after which check should be done
     */
    @get:Input
    var phaseToCheck: String = "CStubs"

    /**
     * Should we generate framework instead of an executable?
     * This option is useful for, well, checking framework-specific code.
     */
    @get:Input
    var generateFramework: Boolean = false

    @get:Input
    @get:Optional
    var additionalFileCheckFlags: List<String>? = null

    @get:Input
    var extraOpts: List<String> = emptyList()

    @get:Optional
    @get:Input
    var targetName: String = project.testTarget.name

    @get:Internal
    val target: KonanTarget
        get() = project.platformManager.targetByName(targetName)

    override fun configure(closure: Closure<*>): Task {
        super.configure(closure)
        if (target != HostManager.host) {
            dependsOnCrossDist(target)
        }
        return this
    }

    /**
     * Check that [inputFile] matches [annotatedFile] with FileCheck.
     */
    private fun runFileCheck(annotatedFile: Path, inputFile: Path): ProcessOutput {
        val args = mutableListOf(
                annotatedFile.toAbsolutePath().toString(),
                "--input-file", inputFile.toAbsolutePath().toString()
        )
        checkPrefix?.let {
            args.addAll(listOf("--check-prefix", it))
        }
        additionalFileCheckFlags?.let {
            args.addAll(it)
        }
        val platform = project.platformManager.platform(target)
        val configurables = platform.configurables
        val llvmBin = "${configurables.absoluteLlvmHome}/bin"
        val fileCheck = "$llvmBin/FileCheck"
        return runProcess(localExecutor(project), fileCheck, *args.toTypedArray())
                .ensureSuccessful(fileCheck, *args.toTypedArray())
    }

    private fun ProcessOutput.ensureSuccessful(vararg command: String): ProcessOutput {
        if (exitCode != 0) {
            println("""
                    ${command.joinToString(separator = " ")} failed.
                    exitCode: $exitCode
                    stdout:
                    $stdOut
                    stderr:
                    $stdErr
                """.trimIndent())
            throw TestFailedException("${command.joinToString(separator = " ")} failed")
        }
        return this
    }
}