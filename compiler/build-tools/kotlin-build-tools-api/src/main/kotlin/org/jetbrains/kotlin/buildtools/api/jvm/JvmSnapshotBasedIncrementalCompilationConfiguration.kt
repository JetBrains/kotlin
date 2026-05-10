/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path

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
 * @property shrunkClasspathSnapshot The path to the shrunk classpath snapshot file from a previous compilation.
 * @property options is deprecated and unused
 *
 * @see JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder
 */
@Suppress("DEPRECATION_ERROR")
@ExperimentalBuildToolsApi
public open class JvmSnapshotBasedIncrementalCompilationConfiguration
@Deprecated(
    "Instantiating this class directly will not be possible and it will become abstract in a future release. Use `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`.",
    level = DeprecationLevel.ERROR
)
constructor(
    public val workingDirectory: Path,
    public val sourcesChanges: SourcesChanges,
    public val dependenciesSnapshotFiles: List<Path>,
    @Deprecated("This property is no longer required and will be removed in a future release.")
    public val shrunkClasspathSnapshot: Path,
    @Deprecated("Use `get` directly instead or a `Builder` instance to set options. This property will be removed in a future release.", level = DeprecationLevel.ERROR)
    public open val options: JvmSnapshotBasedIncrementalCompilationOptions,
) : JvmIncrementalCompilationConfiguration, BaseIncrementalCompilationConfiguration {

    /**
     * When instantiating JvmSnapshotBasedIncrementalCompilationConfiguration through `snapshotBasedIcConfigurationBuilder`,
     * the `options` property is completely unused, but we cannot remove it or change it to nullable for compatibility reasons.
     * So, we put a dummy implementation that does nothing in there instead.
     */
    private object DUMMY_OPTIONS : JvmSnapshotBasedIncrementalCompilationOptions {
        override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V {
            error("Not implemented. Do not use `JvmSnapshotBasedIncrementalCompilationConfiguration.options` - it's deprecated.")
        }

        override fun <V> set(
            key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>,
            value: V,
        ) {
            error("Not implemented. Do not use `JvmSnapshotBasedIncrementalCompilationConfiguration.options` - it's deprecated.")
        }

        override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
            error("Not implemented. Do not use `JvmSnapshotBasedIncrementalCompilationConfiguration.options` - it's deprecated.")
        }
    }

    @Deprecated("Instantiating this class directly will not be possible and it will become abstract in a future release. Use `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`.")
    public constructor(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
    ) : this(
        workingDirectory,
        sourcesChanges,
        dependenciesSnapshotFiles,
        workingDirectory.resolve("classpath-snapshot"),
        DUMMY_OPTIONS
    )

    @Deprecated("Instantiating this class directly will not be possible and it will become abstract in a future release. Use `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`.")
    public constructor(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        dependenciesSnapshotFiles: List<Path>,
        shrunkClasspathSnapshot: Path,
    ) : this(
        workingDirectory,
        sourcesChanges,
        dependenciesSnapshotFiles,
        shrunkClasspathSnapshot,
        DUMMY_OPTIONS
    )

    /**
     * A builder for [JvmIncrementalCompilationConfiguration].
     *
     * @since 2.3.20
     */
    public interface Builder : BaseIncrementalCompilationConfiguration.Builder {
        /**
         * The working directory for the IC operation to store internal objects
         *
         * @since 2.3.20
         */
        public val workingDirectory: Path

        /**
         * Changes in the source files, which can be unknown, to-be-calculated, or known
         *
         * @since 2.3.20
         */
        public val sourcesChanges: SourcesChanges

        /**
         * A list of paths to dependency snapshot files produced by [org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmClasspathSnapshottingOperation].
         *
         * @since 2.3.20
         */
        public val dependenciesSnapshotFiles: List<Path>

        /**
         * The path to the shrunk classpath snapshot file from a previous compilation.
         * @deprecated The property is no longer required. Will be promoted to an error in KT-83937.
         *
         * @since 2.3.20
         */
        @Deprecated("This property is no longer required and will be removed in a future release.")
        public val shrunkClasspathSnapshot: Path

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         * @since 2.3.20
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         * @since 2.3.20
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [JvmSnapshotBasedIncrementalCompilationConfiguration] based on the configuration of this builder.
         *
         * @since 2.3.20
         */
        public fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration
    }

    /**
     * Creates a builder for [JvmSnapshotBasedIncrementalCompilationConfiguration] that contains a copy of this configuration.
     *
     * @since 2.3.20
     */
    public open fun toBuilder(): Builder {
        error("To use `toBuilder` you must instantiate this object through a Builder obtained from `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder()`.")
    }

    /**
     * An option for configuring a [JvmSnapshotBasedIncrementalCompilationConfiguration].
     *
     * @see get
     * @see set
     */
    public class Option<V> internal constructor(
        id: String,
        public val availableSinceVersion: KotlinReleaseVersion,
    ) : BaseOption<V>(id)

    /**
     * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public open operator fun <V> get(key: Option<V>): V {
        error("To use `get` and `set` you must instantiate this object through `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`.")
    }

    /**
     * Set the [value] for option specified by [key], overriding any previous value for that option.
     */
    @Deprecated("Use `JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder` to create a `Builder` instead.")
    public open operator fun <V> set(key: Option<V>, value: V) {
        error("To use `get` and `set` you must instantiate this object through `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`.")
    }

    override operator fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V {
        error("To use `get` and `set` you must instantiate this object through `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`.")
    }

    public companion object {

        /**
         * The root project directory, used for computing relative paths for source files in the incremental compilation caches.
         *
         * If it is not specified, incremental compilation caches will be non-relocatable.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.ROOT_PROJECT_DIR` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.ROOT_PROJECT_DIR",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        public val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", KotlinReleaseVersion(2, 3, 0))

        /**
         * The build directory, used for computing relative paths for output files in the incremental compilation caches.
         *
         * If it is not specified, incremental compilation caches will be non-relocatable.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.MODULE_BUILD_DIR` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.MODULE_BUILD_DIR",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        public val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether incremental compilation will analyze Java files precisely for better changes detection.
         */
        @JvmField
        public val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether incremental compilation should perform file-by-file backup of previously compiled files
         * and revert them in the case of a compilation failure
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.BACKUP_CLASSES` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.BACKUP_CLASSES",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        public val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether caches should remain in memory
         * and not be flushed to the disk until the compilation can be marked as successful.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.KEEP_IC_CACHES_IN_MEMORY",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        public val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether the non-incremental mode of the incremental compiler is forced.
         * The non-incremental mode of the incremental compiler means that during the compilation,
         * the compiler will collect enough information to perform subsequent builds incrementally.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.FORCE_RECOMPILATION` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.FORCE_RECOMPILATION",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        public val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", KotlinReleaseVersion(2, 3, 0))

        /**
         * The directories that the compiler will clean in the case of fallback to non-incremental compilation.
         *
         * The default ones are calculated in the case of a `null` value as a set of the incremental compilation working directory
         * passed to [JvmSnapshotBasedIncrementalCompilationConfiguration] and the classes output directory from the compiler arguments.
         *
         * If the value is set explicitly, it must contain the above-mentioned default directories.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.OUTPUT_DIRS` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.OUTPUT_DIRS",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        public val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether classpath snapshots comparing should be avoided.
         *
         * Can be used as an optimization if the check is already performed by the API consumer.
         */
        @JvmField
        public val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> = Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether the *experimental* incremental runner based on Kotlin compiler FIR is used.
         * This runner only works with Kotlin Language Version 2.0+ and is disabled by default.
         */
        @JvmField
        @ExperimentalCompilerArgument
        public val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER", KotlinReleaseVersion(2, 3, 0))

        /**
         * By default, with the K2 compiler and KMP, we recompile the whole module if any common sources are recompiled.
         * Keeping this option disabled provides consistent builds at the cost of compilation speed. (See KT-62686 for the underlying issue.)
         * Enabling this option brings back pre-K2 behavior and may potentially introduce incorrect incremental builds.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        @ExperimentalCompilerArgument
        public val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM", KotlinReleaseVersion(2, 3, 0))

        /**
         * When this option is enabled, the incremental compilation scope is always expanded monotonously (see explanation below).
         *
         * For example, when recompilation of file `a.kt` introduces changes that require the recompilation of file `b.kt`, the new
         * file `b.kt` is _added_ to the compilation scope, and both files `a.kt` and `b.kt` are recompiled in the next step.
         *
         * When this option is disabled, only the files that weren't compiled previously are recompiled,
         * so only `b.kt` from the example above would be recompiled in the second step.
         */
        @Deprecated(
            "Use `BaseIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION` instead.",
            ReplaceWith(
                "BaseIncrementalCompilationConfiguration.MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION",
                "org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration"
            )
        )
        @JvmField
        @ExperimentalCompilerArgument
        public val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION", KotlinReleaseVersion(2, 3, 0))
    }
}

/**
 * Options for [JvmSnapshotBasedIncrementalCompilationConfiguration].
 *
 * @since 2.3.0
 */
@Suppress("DEPRECATION_ERROR")
@Deprecated(
    "Use `JvmSnapshotBasedIncrementalCompilationConfiguration` and `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`. This interface will be removed in a future release.",
    level = DeprecationLevel.ERROR
)
@ExperimentalBuildToolsApi
public interface JvmSnapshotBasedIncrementalCompilationOptions : BaseIncrementalCompilationConfiguration {
    /**
     * An option for configuring a [JvmSnapshotBasedIncrementalCompilationOptions].
     *
     * @see get
     * @see set
     */
    @Deprecated(
        "Use `JvmSnapshotBasedIncrementalCompilationConfiguration.Option` This interface will be removed in a future release.",
        level = DeprecationLevel.ERROR
    )
    public open class Option<V> internal constructor(id: String) : BaseOption<V>(id)

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

    @Deprecated(
        "Use `JvmSnapshotBasedIncrementalCompilationConfiguration` and `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`. This interface will be removed in a future release.",
        level = DeprecationLevel.ERROR
    )
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
        public val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING")

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
        public val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> = Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES")

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
        public val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM")

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
