/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.ICReporter
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_LEVEL
import org.jetbrains.kotlin.buildtools.api.jvm.ClassSnapshotGranularity.CLASS_MEMBER_LEVEL
import org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.incremental.ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun.ToBeComputedByIncrementalCompiler
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathEntrySnapshotter
import org.jetbrains.kotlin.incremental.libraryDiff.klib.KlibEntrySnapshotExternalizer
import org.jetbrains.kotlin.incremental.libraryDiff.klib.KlibEntrySnapshotter
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistoryJs
import org.jetbrains.kotlin.incremental.storage.saveToFile
import java.io.File

/**
 * Runs the multi-module JS klib tests with klib snapshots instead of build history files. Like the Gradle transform, it snapshots each
 * dependency klib, and passes the snapshot files to the runner as [ClasspathChanges].
 */
abstract class AbstractIncrementalJsKlibMultiModuleWithKlibSnapshotsCompilerRunnerTest :
    AbstractIncrementalJsKlibMultiModuleCompilerRunnerTest() {

    override fun transformToDependency(moduleName: String, rawArtifact: File): File {
        val dependencyFile = super.transformToDependency(moduleName, rawArtifact)
        snapshotKlib(dependencyFile, snapshotFileOf(dependencyFile), CLASS_MEMBER_LEVEL)
        return dependencyFile
    }

    override fun makeForSingleModule(
        moduleCacheDir: File,
        sourceRoots: Iterable<File>,
        args: K2JSCompilerArguments,
        moduleBuildHistoryFile: File,
        messageCollector: MessageCollector,
        reporter: ICReporter,
        scopeExpansion: CompileScopeExpansionMode,
        modulesApiHistory: ModulesApiHistoryJs,
        providedChangedFiles: ChangedFiles?
    ) {
        val snapshotFiles = args.libraries!!.split(File.pathSeparator).map(::File).map {
            if (it == stdlib) stdlibSnapshotFile else snapshotFileOf(it)
        }
        // Like the JVM tests, keep the shrunk snapshot outside the caches dir, which is cleared on non-incremental builds
        val classpathSnapshotDir = File(workingDir, "klibSnapshot/${moduleCacheDir.name}").apply { mkdirs() }
        val classpathChanges = ToBeComputedByIncrementalCompiler(ClasspathSnapshotFiles(snapshotFiles, classpathSnapshotDir))

        makeJsIncrementally(
            moduleCacheDir,
            sourceRoots,
            args,
            buildHistoryFile = null,
            messageCollector,
            reporter,
            scopeExpansion,
            modulesApiHistory,
            providedChangedFiles,
            classpathChanges,
        )
    }

    companion object {
        private val stdlib: File = ForTestCompileRuntime.stdlibJs()

        // All tests use the same stdlib, so snapshot it once and reuse the file
        private val stdlibSnapshotFile: File by lazy {
            File.createTempFile("stdlib-klib-snapshot", ".bin").apply {
                deleteOnExit()
                snapshotKlib(stdlib, this, CLASS_LEVEL)
            }
        }

        private fun snapshotFileOf(klib: File): File = File(klib.path + ".snapshot.bin")

        private fun snapshotKlib(klib: File, snapshotFile: File, granularity: ClassSnapshotGranularity) {
            val settings = ClasspathEntrySnapshotter.Settings(granularity, parseInlinedLocalClasses = false, expandTypeAliases = false)
            KlibEntrySnapshotExternalizer.saveToFile(snapshotFile, KlibEntrySnapshotter.snapshot(klib, settings))
        }
    }
}
