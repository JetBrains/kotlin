/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString

/**
 * Represents a configuration that enables incremental compilation.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Currently, the only supported implementation of this interface is [JvmSnapshotBasedIncrementalCompilationConfiguration],
 * but more may be added in the future.
 *
 * @see JvmSnapshotBasedIncrementalCompilationConfiguration
 */
@ExperimentalBuildToolsApi
public interface JvmIncrementalCompilationConfiguration

/**
 * A configuration for incremental compilation based on snapshots.
 *
 * @property workingDirectory the working directory for the IC operation to store internal objects.
 * @property sourcesChanges changes in the source files, which can be unknown, to-be-calculated, or known.
 * @property dependenciesSnapshotFiles a list of paths to dependency snapshot files produced by [org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation].
 * @property options an option set produced by [JvmCompilationOperation.createSnapshotBasedIcOptions]
 */
@ExperimentalBuildToolsApi
public class JvmSnapshotBasedIncrementalCompilationConfiguration(
    public val workingDirectory: Path,
    public val sourcesChanges: SourcesChanges,
    public val dependenciesSnapshotFiles: List<Path>,
    public val shrunkClasspathSnapshot: Path,
    public val options: JvmSnapshotBasedIncrementalCompilationOptions,
) : JvmIncrementalCompilationConfiguration {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JvmSnapshotBasedIncrementalCompilationConfiguration

        if (workingDirectory != other.workingDirectory) return false
        if (sourcesChanges != other.sourcesChanges) return false
        if (dependenciesSnapshotFiles != other.dependenciesSnapshotFiles) return false
        if (shrunkClasspathSnapshot != other.shrunkClasspathSnapshot) return false
        if (options != other.options) return false

        return true
    }

    override fun hashCode(): Int {
        val projectRootPath =
            System.getProperty("kotlin.build.tools.internal.recorder.projectRootPath")?.let { Paths.get(it).absolutePathString() } ?: ""
        var result = workingDirectory.absolutePathString().removePrefix(projectRootPath).hashCode()
        result = 31 * result + sourcesChanges.hashCode()
        result = 31 * result + dependenciesSnapshotFiles.hashCode()
        result = 31 * result + shrunkClasspathSnapshot.absolutePathString().removePrefix(projectRootPath).hashCode()
        result = 31 * result + options.hashCode()
        return result
    }
}

/**
 * Options for [JvmSnapshotBasedIncrementalCompilationConfiguration].
 *
 * @since 2.3.0
 */
@ExperimentalBuildToolsApi
public interface JvmSnapshotBasedIncrementalCompilationOptions {
    /**
     * Base class for [JvmSnapshotBasedIncrementalCompilationOptions] options.
     *
     * @see get
     * @see set
     */
    public class Option<V> internal constructor(id: String) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {

        /**
         * The root project directory, used for computing relative paths for source files in the incremental compilation caches.
         *
         * If it is not specified, incremental compilation caches will be non-relocatable.
         */
        @JvmField
        public val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR")

        /**
         * The build directory, used for computing relative paths for output files in the incremental compilation caches.
         *
         * If it is not specified, incremental compilation caches will be non-relocatable.
         */
        @JvmField
        public val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR")

        /**
         * Controls whether incremental compilation will analyze Java files precisely for better changes detection.
         */
        @JvmField
        public val PRECISE_JAVA_TRACKING: Option<Boolean> =
            Option("PRECISE_JAVA_TRACKING")

        /**
         * Controls whether incremental compilation should perform file-by-file backup of previously compiled files
         * and revert them in the case of a compilation failure
         */
        @JvmField
        public val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES")

        /**
         * Controls whether caches should remain in memory
         * and not be flushed to the disk until the compilation can be marked as successful.
         */
        @JvmField
        public val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY")

        /**
         * Controls whether the non-incremental mode of the incremental compiler is forced.
         * The non-incremental mode of the incremental compiler means that during the compilation,
         * the compiler will collect enough information to perform subsequent builds incrementally.
         */
        @JvmField
        public val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION")

        /**
         * The directories that the compiler will clean in the case of fallback to non-incremental compilation.
         *
         * The default ones are calculated in the case of a `null` value as a set of the incremental compilation working directory
         * passed to [JvmSnapshotBasedIncrementalCompilationConfiguration] and the classes output directory from the compiler arguments.
         *
         * If the value is set explicitly, it must contain the above-mentioned default directories.
         */
        @JvmField
        public val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS")

        /**
         * Controls whether classpath snapshots comparing should be avoided.
         *
         * Can be used as an optimization if the check is already performed by the API consumer.
         */
        @JvmField
        public val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES")

        /**
         * Controls whether the *experimental* incremental runner based on Kotlin compiler FIR is used.
         * This runner only works with Kotlin Language Version 2.0+ and is disabled by default.
         */
        @JvmField
        @ExperimentalCompilerArgument
        public val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER")

        /**
         * By default, with the K2 compiler and KMP, we recompile the whole module if any common sources are recompiled.
         * Keeping this option disabled provides consistent builds at the cost of compilation speed. (See KT-62686 for the underlying issue.)
         * Enabling this option brings back pre-K2 behavior and may potentially introduce incorrect incremental builds.
         */
        @JvmField
        @ExperimentalCompilerArgument
        public val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> = Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM")

        /**
         * When this option is enabled, the incremental compilation scope is always expanded monotonously (see explanation below).
         *
         * For example, when recompilation of file `a.kt` introduces changes that require the recompilation of file `b.kt`, the new
         * file `b.kt` is _added_ to the compilation scope, and both files `a.kt` and `b.kt` are recompiled in the next step.
         *
         * When this option is disabled, only the files that weren't compiled previously are recompiled,
         * so only `b.kt` from the example above would be recompiled in the second step.
         */
        @JvmField
        @ExperimentalCompilerArgument
        public val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION")
    }
}
