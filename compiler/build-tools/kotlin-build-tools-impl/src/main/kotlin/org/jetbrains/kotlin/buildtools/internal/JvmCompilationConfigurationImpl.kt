/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.*
import java.io.File

internal class JvmCompilationConfigurationImpl(
    override var kotlinScriptFilenameExtensions: Set<String> = emptySet(),
    override var logger: KotlinLogger = DefaultKotlinLogger,
) : JvmCompilationConfiguration {
    override fun useLogger(logger: KotlinLogger): JvmCompilationConfiguration {
        this.logger = logger
        return this
    }

    override fun useKotlinScriptFilenameExtensions(kotlinScriptExtensions: Collection<String>): JvmCompilationConfiguration {
        this.kotlinScriptFilenameExtensions = kotlinScriptExtensions.toSet()
        return this
    }

    override fun makeClasspathSnapshotBasedIncrementalCompilationConfiguration() = ClasspathSnapshotBasedIncrementalJvmCompilationConfigurationImpl()

    override fun <P : IncrementalCompilationApproachParameters> useIncrementalCompilation(
        workingDirectory: File,
        sourcesChanges: SourcesChanges,
        approachParameters: P,
        options: IncrementalJvmCompilationConfiguration<P>,
    ) = TODO("Incremental compilation is not yet supported to run via the Build Tools API")
}

internal abstract class JvmIncrementalCompilationConfigurationImpl<P : IncrementalCompilationApproachParameters>(
    override var preciseJavaTrackingEnabled: Boolean = true,
    override var preciseCompilationResultsBackupEnabled: Boolean = false,
    override var incrementalCompilationCachesKeptInMemory: Boolean = false,
    override var projectDir: File? = null,
    override var forcedNonIncrementalMode: Boolean = false,
) : IncrementalJvmCompilationConfiguration<P> {
    override fun useProjectDir(projectDir: File): IncrementalJvmCompilationConfiguration<P> {
        this.projectDir = projectDir
        return this
    }

    override fun usePreciseJavaTracking(value: Boolean): IncrementalJvmCompilationConfiguration<P> {
        preciseJavaTrackingEnabled = value
        return this
    }

    override fun usePreciseCompilationResultsBackup(value: Boolean): IncrementalJvmCompilationConfiguration<P> {
        preciseCompilationResultsBackupEnabled = value
        return this
    }

    override fun keepIncrementalCompilationCachesInMemory(value: Boolean): IncrementalJvmCompilationConfiguration<P> {
        incrementalCompilationCachesKeptInMemory = value
        return this
    }

    override fun forceNonIncrementalMode(value: Boolean): IncrementalJvmCompilationConfiguration<P> {
        forcedNonIncrementalMode = value
        return this
    }
}

internal class ClasspathSnapshotBasedIncrementalJvmCompilationConfigurationImpl(
    override var assuredNoClasspathSnapshotsChanges: Boolean = false,
) :
    JvmIncrementalCompilationConfigurationImpl<ClasspathSnapshotBasedIncrementalCompilationApproachParameters>(),
    ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
    override fun useProjectDir(projectDir: File): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        super.useProjectDir(projectDir)
        return this
    }

    override fun usePreciseJavaTracking(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        super.usePreciseJavaTracking(value)
        return this
    }

    override fun usePreciseCompilationResultsBackup(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        super.usePreciseCompilationResultsBackup(value)
        return this
    }

    override fun keepIncrementalCompilationCachesInMemory(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        super.keepIncrementalCompilationCachesInMemory(value)
        return this
    }

    override fun forceNonIncrementalMode(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        super.forceNonIncrementalMode(value)
        return this
    }

    override fun assureNoClasspathSnapshotsChanges(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration {
        assuredNoClasspathSnapshotsChanges = value
        return this
    }
}