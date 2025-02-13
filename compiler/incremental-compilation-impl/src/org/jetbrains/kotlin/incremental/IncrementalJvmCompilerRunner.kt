/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS
import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.metrics.measure
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.classpathDiff.ClasspathSnapshotBuildReporter
import org.jetbrains.kotlin.incremental.classpathDiff.shrinkAndSaveClasspathSnapshot
import org.jetbrains.kotlin.incremental.dirtyFiles.JvmSourcesToCompileCalculator
import org.jetbrains.kotlin.incremental.snapshots.LazyClasspathSnapshot
import java.io.File

open class IncrementalJvmCompilerRunner(
    workingDir: File,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    outputDirs: Collection<File>?,
    private val classpathChanges: ClasspathChanges,
    kotlinSourceFilesExtensions: Set<String> = DEFAULT_KOTLIN_SOURCE_FILES_EXTENSIONS,
    icFeatures: IncrementalCompilationFeatures = IncrementalCompilationFeatures.DEFAULT_CONFIGURATION,
) : IncrementalJvmCompilerRunnerBase(
    workingDir = workingDir,
    reporter = reporter,
    buildHistoryFile = null,
    outputDirs = outputDirs,
    kotlinSourceFilesExtensions = kotlinSourceFilesExtensions,
    icFeatures = icFeatures,
) {
    override val shouldTrackChangesInLookupCache
        get() = classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled.IncrementalRun

    private val lazyClasspathSnapshot = LazyClasspathSnapshot(classpathChanges, ClasspathSnapshotBuildReporter(reporter))

    override fun calculateSourcesToCompile(
        caches: IncrementalJvmCachesManager,
        changedFiles: DeterminableFiles.Known,
        args: K2JVMCompilerArguments,
        messageCollector: MessageCollector,
        classpathAbiSnapshots: Map<String, AbiSnapshot>
    ): CompilationMode {
        return try {
            val sourcesToCompileCalculator = JvmSourcesToCompileCalculator(
                caches,
                changedFiles,
                kotlinSourceFilesExtensions,
                javaInteropCoordinator,
                dirtyFilesProvider,
                reporter
            )

            sourcesToCompileCalculator.calculateWithClasspathSnapshot(
                classpathChanges,
                lazyClasspathSnapshot
            )
        } finally {
            this.messageCollector.forward(messageCollector)
            this.messageCollector.clear()
        }
    }

    override fun performWorkAfterCompilation(compilationMode: CompilationMode, exitCode: ExitCode, caches: IncrementalJvmCachesManager) {
        super.performWorkAfterCompilation(compilationMode, exitCode, caches)

        // No need to shrink and save classpath snapshot if exitCode != ExitCode.OK as the task will fail anyway
        if (classpathChanges is ClasspathChanges.ClasspathSnapshotEnabled && exitCode == ExitCode.OK) {
            reporter.measure(GradleBuildTime.SHRINK_AND_SAVE_CURRENT_CLASSPATH_SNAPSHOT_AFTER_COMPILATION) {
                shrinkAndSaveClasspathSnapshot(
                    compilationWasIncremental = compilationMode is CompilationMode.Incremental, classpathChanges, caches.lookupCache,
                    lazyClasspathSnapshot, ClasspathSnapshotBuildReporter(reporter)
                )
            }
        }
    }
}
