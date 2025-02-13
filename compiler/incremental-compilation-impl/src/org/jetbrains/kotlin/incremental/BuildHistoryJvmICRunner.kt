/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.build.report.BuildReporter
import org.jetbrains.kotlin.build.report.metrics.GradleBuildPerformanceMetric
import org.jetbrains.kotlin.build.report.metrics.GradleBuildTime
import org.jetbrains.kotlin.build.report.warn
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.incremental.ChangedFiles.DeterminableFiles
import org.jetbrains.kotlin.incremental.dirtyFiles.JvmSourcesToCompileCalculator
import org.jetbrains.kotlin.incremental.multiproject.ModulesApiHistory
import org.jetbrains.kotlin.incremental.util.Either
import java.io.File

/**
 * JVM incremental compilation runner with buildHistory-based compile avoidance
 *
 * It's deprecated in Gradle world, but it's used in Maven and in certain tests
 */
open class BuildHistoryJvmICRunner(
    workingDir: File,
    reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>,
    buildHistoryFile: File,
    outputDirs: Collection<File>?,
    private val modulesApiHistory: ModulesApiHistory,
    kotlinSourceFilesExtensions: Set<String>,
    icFeatures: IncrementalCompilationFeatures,
) : IncrementalJvmCompilerRunnerBase(
    workingDir = workingDir,
    reporter = reporter,
    buildHistoryFile = buildHistoryFile,
    outputDirs = outputDirs,
    kotlinSourceFilesExtensions = kotlinSourceFilesExtensions,
    icFeatures = icFeatures,
) {
    override val shouldTrackChangesInLookupCache = false

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

            sourcesToCompileCalculator.calculateWithBuildHistory(
                args,
                classpathAbiSnapshots,
                modulesApiHistory,
                buildHistoryFile,
                lastBuildInfoFile,
                icFeatures,
                messageCollector,
            )
        } finally {
            this.messageCollector.forward(messageCollector)
            this.messageCollector.clear()
        }
    }

    override fun setupJarDependencies(args: K2JVMCompilerArguments, reporter: BuildReporter<GradleBuildTime, GradleBuildPerformanceMetric>): Map<String, AbiSnapshot> {
        //fill abiSnapshots
        val abiSnapshots = HashMap<String, AbiSnapshot>()
        args.classpathAsList
            .filter { it.extension.equals("jar", ignoreCase = true) }
            .forEach {
                modulesApiHistory.abiSnapshot(it).let { result ->
                    if (result is Either.Success<Set<File>>) {
                        result.value.forEach { file ->
                            if (file.exists()) {
                                abiSnapshots[it.absolutePath] = AbiSnapshotImpl.read(file)
                            } else {
                                // FIXME: We should throw an exception here
                                reporter.warn { "Snapshot file does not exist: ${file.path}. Continue anyway." }
                            }
                        }
                    }
                }
            }
        return abiSnapshots
    }
}
