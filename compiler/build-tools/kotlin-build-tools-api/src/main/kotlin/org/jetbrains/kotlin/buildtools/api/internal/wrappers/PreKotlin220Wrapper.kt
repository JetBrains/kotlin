/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.api.internal.wrappers

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.CompilationService
import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.ProjectId
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalCompilationApproachParameters
import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import java.io.File

internal class PreKotlin220Wrapper(
    private val base: CompilationService
) : CompilationService by base {
    override fun makeJvmCompilationConfiguration(): JvmCompilationConfiguration {
        return Pre220JvmCompilationConfigurationWrapper(
            base.makeJvmCompilationConfiguration(),
            getCompilerVersion(),
        )
    }

    override fun compileJvm(
        projectId: ProjectId,
        strategyConfig: CompilerExecutionStrategyConfiguration,
        compilationConfig: JvmCompilationConfiguration,
        sources: List<File>,
        arguments: List<String>
    ): CompilationResult {
        val unwrappedCompilationConfig = if (compilationConfig is Pre220JvmCompilationConfigurationWrapper) {
            compilationConfig.base
        } else {
            compilationConfig
        }

        return base.compileJvm(
            projectId,
            strategyConfig,
            unwrappedCompilationConfig,
            sources,
            arguments
        )
    }
}

private class Pre220JvmCompilationConfigurationWrapper(
    val base: JvmCompilationConfiguration,
    private val kotlinVersion: String,
) : JvmCompilationConfiguration {
    override val logger: KotlinLogger
        get() = base.logger
    override val kotlinScriptFilenameExtensions: Set<String>
        get() = base.kotlinScriptFilenameExtensions

    override fun useLogger(logger: KotlinLogger): JvmCompilationConfiguration {
        base.useLogger(logger)
        return this
    }

    override fun useKotlinScriptFilenameExtensions(kotlinScriptExtensions: Collection<String>): JvmCompilationConfiguration {
        base.useKotlinScriptFilenameExtensions(kotlinScriptExtensions)
        return this
    }

    override fun makeClasspathSnapshotBasedIncrementalCompilationConfiguration(
    ): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        return Pre220ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration(
            base.makeClasspathSnapshotBasedIncrementalCompilationConfiguration(),
            logger,
            kotlinVersion,
        )
    }

    override fun <P : IncrementalCompilationApproachParameters> useIncrementalCompilation(
        workingDirectory: File,
        sourcesChanges: SourcesChanges,
        approachParameters: P,
        options: IncrementalJvmCompilationConfiguration<P>
    ) {
        base.useIncrementalCompilation(workingDirectory, sourcesChanges, approachParameters, options)
    }
}

private class Pre220ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration(
    val base: ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration,
    private val logger: KotlinLogger,
    private val kotlinVersion: String,
) : ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
    override val assuredNoClasspathSnapshotsChanges: Boolean
        get() = base.assuredNoClasspathSnapshotsChanges

    override fun assureNoClasspathSnapshotsChanges(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.assureNoClasspathSnapshotsChanges(value)
        return this
    }

    override fun setRootProjectDir(rootProjectDir: File): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.setRootProjectDir(rootProjectDir)
        return this
    }

    override fun setBuildDir(buildDir: File): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.setBuildDir(buildDir)
        return this
    }

    override fun usePreciseJavaTracking(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.usePreciseJavaTracking(value)
        return this
    }

    override fun usePreciseCompilationResultsBackup(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.usePreciseCompilationResultsBackup(value)
        return this
    }

    override fun keepIncrementalCompilationCachesInMemory(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.keepIncrementalCompilationCachesInMemory(value)
        return this
    }

    override fun forceNonIncrementalMode(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.forceNonIncrementalMode(value)
        return this
    }

    override fun useOutputDirs(outputDirs: Collection<File>): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        base.useOutputDirs(outputDirs)
        return this
    }

    override val rootProjectDir: File?
        get() = base.rootProjectDir
    override val buildDir: File?
        get() = base.buildDir
    override val preciseJavaTrackingEnabled: Boolean
        get() = base.preciseJavaTrackingEnabled
    override val preciseCompilationResultsBackupEnabled: Boolean
        get() = base.preciseCompilationResultsBackupEnabled
    override val incrementalCompilationCachesKeptInMemory: Boolean
        get() = base.incrementalCompilationCachesKeptInMemory
    override val forcedNonIncrementalMode: Boolean
        get() = base.forcedNonIncrementalMode
    override val outputDirs: Set<File>?
        get() = base.outputDirs

    override val isUsingFirRunner: Boolean = false

    override fun useFirRunner(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        // not available in BTA implementations < 2.2.0
        if (value) logger.warn(
            "Kotlin FIR incremental compilation runner is not available in Kotlin compiler version $kotlinVersion.\n" +
                    "Please ensure Kotlin Build Tools implementation version is at least 2.2.0."
        )
        return this
    }
}