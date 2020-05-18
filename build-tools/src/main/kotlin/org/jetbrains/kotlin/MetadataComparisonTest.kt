/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.klib.metadata.CInteropComparisonConfig
import org.jetbrains.kotlin.klib.metadata.MetadataCompareResult
import org.jetbrains.kotlin.klib.metadata.compareKlibMetadata
import org.jetbrains.kotlin.klib.metadata.expandFail
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File

/**
 * Produces 2 interop libraries from the given [defFile].
 * One in legacy `sourcecode` mode and another one in `metadata` mode.
 * Then structurally compares metadata of the produced klibs.
 *
 *
 * Motivation for this kind of tests:
 *
 * `sourcecode` mode generates Kotlin source file that is passes to a
 * regular Kotlin/Native compiler.
 * Thus, the produced metadata is correct (from compiler's point of view)
 * because it is produced by the compiler itself.
 *
 * `metadata` mode generates metadata directly from parsed Clang AST.
 * Since another algorithm is used, produced metadata may be incorrect.
 *
 * So we have to check that these two are more or less the same.
 */
open class MetadataComparisonTest : DefaultTask() {

    private enum class Mode {
        METADATA, SOURCECODE
    }

    /**
     * Path to a cinterop *.def file.
     */
    @Input
    lateinit var defFile: String

    @TaskAction
    fun run() {
        val metadataLibrary = cinterop(project.file(defFile), Mode.METADATA)
        val sourcecodeLibrary = cinterop(project.file(defFile), Mode.SOURCECODE)
        compareKlibMetadata(CInteropComparisonConfig(), sourcecodeLibrary.absolutePath, metadataLibrary.absolutePath).let { result ->
            if (result is MetadataCompareResult.Fail) {
                val message = StringBuilder().also {
                    expandFail(result, it::appendln)
                }.toString()
                throw TestFailedException(message)
            }
        }
    }

    private fun cinterop(defFile: File, mode: Mode): File {
        val dist = project.kotlinNativeDist
        val output = "${project.buildDir.absolutePath}/${defFile.nameWithoutExtension}_$mode"
        val tool = if (HostManager.hostIsMingw) "cinterop.bat" else "cinterop"
        val cinterop = File("${dist.canonicalPath}/bin/$tool").absolutePath
        val args = listOf(
                "-def", defFile.absolutePath,
                "-mode", mode.name.toLowerCase(),
                "-target", project.testTarget.visibleName,
                "-no-default-libs", "-no-endorsed-libs",
                "-o", output
        )
        runProcess(localExecutor(project), cinterop, args).let { result ->
            if (result.exitCode != 0) {
                println("""
                    cinterop failed.
                    exitCode: ${result.exitCode}
                    stdout:
                    ${result.stdOut}
                    stderr:
                    ${result.stdErr}
                """.trimIndent())
            }
        }
        return File(output)
    }
}