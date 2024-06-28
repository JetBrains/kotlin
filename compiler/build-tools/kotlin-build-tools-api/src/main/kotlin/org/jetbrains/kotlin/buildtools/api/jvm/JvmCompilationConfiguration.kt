/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinLogger
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import java.io.File

/**
 * Allows users to customize the compilation process and to observe the configured settings (or the default ones).
 *
 * This interface defines a set of properties and methods that allow users to customize the compilation process.
 * It provides control over various aspects of compilation, such as incremental compilation, logging customization and other.
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface JvmCompilationConfiguration {
    /**
     * A logger used during the compilation.
     *
     * Managed by [useLogger]
     * Default logger is a logger just printing messages to stdin and stderr.
     */
    public val logger: KotlinLogger

    /**
     * @see [JvmCompilationConfiguration.logger]
     */
    public fun useLogger(logger: KotlinLogger): JvmCompilationConfiguration

    /**
     * A set of additional to the `.kt` and `.kts` Kotlin script extensions.
     *
     * Managed by [useKotlinScriptFilenameExtensions]
     * Default value is an empty set.
     */
    public val kotlinScriptFilenameExtensions: Set<String>

    /**
     * @see [JvmCompilationConfiguration.kotlinScriptFilenameExtensions]
     */
    public fun useKotlinScriptFilenameExtensions(kotlinScriptExtensions: Collection<String>): JvmCompilationConfiguration

    /**
     * Provides a default [ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration] allowing to use it as is or customizing for specific requirements.
     * Could be used as an overview to default values of the options (as they are implementation-specific).
     * @see [useIncrementalCompilation]
     */
    public fun makeClasspathSnapshotBasedIncrementalCompilationConfiguration(): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    /**
     * Configures usage of incremental compilation.
     * @param workingDirectory a working directory for incremental compilation internal state
     * @param sourcesChanges an instance of [SourcesChanges]
     * @param approachParameters an object representing mandatory parameters specific for the selected incremental compilation approach
     * @param options an object representing optional parameters and handles specific for the selected incremental compilation approach
     * @see [makeClasspathSnapshotBasedIncrementalCompilationConfiguration]
     */
    public fun <P : IncrementalCompilationApproachParameters> useIncrementalCompilation(
        workingDirectory: File,
        sourcesChanges: SourcesChanges,
        approachParameters: P,
        options: IncrementalJvmCompilationConfiguration<P>,
    ) {
        error("This version of the Build Tools API does not support incremental compilation")
    }
}

/**
 * Allows to observe and customize general JVM incremental compilation settings.
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface IncrementalJvmCompilationConfiguration<P : IncrementalCompilationApproachParameters> {

    /**
     * The root project directory, used for computing relative paths in the incremental compilation caches.
     *
     * If it is not specified, incremental compilation caches will be non-relocatable.
     *
     * Managed by [setRootProjectDir]
     * Default value is `null`
     */
    public val rootProjectDir: File?

    /**
     * @see [IncrementalJvmCompilationConfiguration.rootProjectDir]
     */
    public fun setRootProjectDir(rootProjectDir: File): IncrementalJvmCompilationConfiguration<P>

    /**
     * The build directory, used for computing relative paths in the incremental compilation caches.
     *
     * If it is not specified, incremental compilation caches will be non-relocatable.
     *
     * Managed by [setBuildDir]
     * Default value is `null`
     */
    public val buildDir: File?

    /**
     * @see [IncrementalJvmCompilationConfiguration.buildDir]
     */
    public fun setBuildDir(buildDir: File): IncrementalJvmCompilationConfiguration<P>

    /**
     * An indicator whether incremental compilation will analyze Java files precisely for better changes detection
     *
     * Managed by [usePreciseJavaTracking]
     * Default value is defined by implementation of the API
     */
    public val preciseJavaTrackingEnabled: Boolean

    /**
     * @see [IncrementalJvmCompilationConfiguration.preciseJavaTrackingEnabled]
     */
    public fun usePreciseJavaTracking(value: Boolean): IncrementalJvmCompilationConfiguration<P>

    /**
     * An indicator whether incremental compilation should perform file-by-file backup of files and revert them in the case of a failure
     *
     * Managed by [usePreciseCompilationResultsBackup]
     * Default value is defined by implementation of the API
     */
    public val preciseCompilationResultsBackupEnabled: Boolean

    /**
     * @see [IncrementalJvmCompilationConfiguration.preciseCompilationResultsBackupEnabled]
     */
    public fun usePreciseCompilationResultsBackup(value: Boolean): IncrementalJvmCompilationConfiguration<P>

    /**
     * Incremental compilation uses the PersistentHashMap of the intellij platform for storing caches.
     * An indicator whether the changes should remain in memory and not being flushed to the disk until we could mark the compilation as successful.
     *
     * Managed by [keepIncrementalCompilationCachesInMemory]
     * Default value is defined by implementation of the API
     */
    public val incrementalCompilationCachesKeptInMemory: Boolean

    /**
     * @see [IncrementalJvmCompilationConfiguration.incrementalCompilationCachesKeptInMemory]
     */
    public fun keepIncrementalCompilationCachesInMemory(value: Boolean): IncrementalJvmCompilationConfiguration<P>

    /**
     * An indicator whether the non-incremental mode of the incremental compiler is forced.
     * The non-incremental mode of the incremental compiler means that during the non-incremental compilation
     * the compiler will collect enough information to perform the following builds incrementally.
     *
     * Manager by [forceNonIncrementalMode]
     * By default, the compilation is considered incremental
     */
    public val forcedNonIncrementalMode: Boolean

    /**
     * @see [forcedNonIncrementalMode]
     */
    public fun forceNonIncrementalMode(value: Boolean = true): IncrementalJvmCompilationConfiguration<P>

    /**
     * The directories that the compiler will clean in the case of fallback to non-incremental compilation.
     *
     * The default ones are calculated in the case of a `null` value as a set of the incremental compilation working directory
     * passed to [JvmCompilationConfiguration.useIncrementalCompilation] and the classes output directory from the compiler arguments.
     *
     * If the value is set explicitly, it must contain the above-mentioned default directories.
     */
    public val outputDirs: Set<File>?

    /**
     * @see [IncrementalJvmCompilationConfiguration.outputDirs]]
     */
    public fun useOutputDirs(outputDirs: Collection<File>): IncrementalJvmCompilationConfiguration<P>
}

/**
 * Allows to observe and customize JVM incremental compilation settings specific to the classpath snapshots based approach.
 *
 * This interface is not intended to be implemented by the API consumers.
 */
@ExperimentalBuildToolsApi
public interface ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration :
    IncrementalJvmCompilationConfiguration<ClasspathSnapshotBasedIncrementalCompilationApproachParameters> {
    /**
     * An indicator whether classpath snapshots comparing should be avoided.
     * Could be used if the check is already performed by the API consumer for the sake of optimization
     *
     * Managed by [assureNoClasspathSnapshotsChanges]
     * By default, the incremental compiler will compare the snapshots itself.
     */
    public val assuredNoClasspathSnapshotsChanges: Boolean

    /**
     * @see [assuredNoClasspathSnapshotsChanges]
     */
    public fun assureNoClasspathSnapshotsChanges(value: Boolean = true): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun setRootProjectDir(rootProjectDir: File): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun setBuildDir(buildDir: File): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun usePreciseJavaTracking(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun usePreciseCompilationResultsBackup(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun keepIncrementalCompilationCachesInMemory(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun forceNonIncrementalMode(value: Boolean): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration

    override fun useOutputDirs(outputDirs: Collection<File>): ClasspathSnapshotBasedIncrementalJvmCompilationConfiguration
}

/**
 * Mandatory parameters for an incremental compilation approach
 */
@ExperimentalBuildToolsApi
public sealed interface IncrementalCompilationApproachParameters

/**
 * Mandatory parameters of the classpath snapshots based incremental compilation approach
 */
@ExperimentalBuildToolsApi
public class ClasspathSnapshotBasedIncrementalCompilationApproachParameters(
    /**
     * The classpath snapshots files actual at the moment of compilation
     */
    public val newClasspathSnapshotFiles: List<File>,
    /**
     * The shrunk classpath snapshot, a result of the previous compilation. Could point to a non-existent file.
     * At the successful end of the compilation, the shrunk version of the [newClasspathSnapshotFiles] will be stored at this path.
     */
    public val shrunkClasspathSnapshot: File,
) : IncrementalCompilationApproachParameters