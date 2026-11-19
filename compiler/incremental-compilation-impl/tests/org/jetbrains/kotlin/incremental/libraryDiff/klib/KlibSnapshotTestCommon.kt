/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.libraryDiff.klib

import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollectorImpl
import org.jetbrains.kotlin.cli.js.K2JSCompiler
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.config.Services
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files

/** The klib counterpart of `ClasspathSnapshotTestCommon`. */
abstract class KlibSnapshotTestCommon {

    @field:TempDir
    lateinit var tmpDir: File

    val klibTestDataDir: File
        get() = ForTestCompileRuntime.transformTestDataPath("compiler/incremental-compilation-impl/testData/org/jetbrains/kotlin/incremental/libraryDiff/klib")

    fun snapshotKlib(srcDir: File, granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL): KlibEntrySnapshot =
        snapshotKlib(srcDir, tmpDir, granularity)

    /** Compiles the Kotlin sources in [srcDir] to a klib and snapshots it. */
    private fun snapshotKlib(
        srcDir: File,
        tmpDir: File,
        granularity: ClassSnapshotGranularity = CLASS_MEMBER_LEVEL,
    ): KlibEntrySnapshot {
        val klib = compileToKlib(srcDir, Files.createTempDirectory(tmpDir.toPath(), "klib").toFile())
        val settings = ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses = false, expandTypeAliases = false)
        return KlibEntrySnapshotter.snapshot(klib, settings)
    }

    /** Compiles the Kotlin sources in [srcDir] to a klib file in [outputDir] and returns it. */
    private fun compileToKlib(srcDir: File, outputDir: File, moduleName: String = "test"): File {
        val sources = srcDir.walk().filter { it.isFile && it.extension == "kt" }.map { it.path }.sorted().toList()
        check(sources.isNotEmpty()) { "No Kotlin sources in $srcDir" }

        val arguments = K2JSCompilerArguments().apply {
            freeArgs = sources
            libraries = ForTestCompileRuntime.stdlibJs().absolutePath
            this.outputDir = outputDir.path
            this.moduleName = moduleName
        }
        val messageCollector = MessageCollectorImpl()
        val exitCode = K2JSCompiler().exec(messageCollector, Services.EMPTY, arguments)
        check(exitCode == ExitCode.OK) { "Failed to compile $srcDir to a klib:\n$messageCollector" }

        return File(outputDir, "$moduleName.klib").also { check(it.isFile) { "Klib not found: $it" } }
    }
}
