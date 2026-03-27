/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
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
 * @property options an option set produced by [JvmCompilationOperation.createSnapshotBasedIcOptions]
 *
 * @see JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder
 */
@ExperimentalBuildToolsApi
public interface JvmSnapshotBasedIncrementalCompilationConfiguration : JvmIncrementalCompilationConfiguration {
    public val workingDirectory: Path
    public val sourcesChanges: SourcesChanges
    public val dependenciesSnapshotFiles: List<Path>

    @Deprecated("This property is no longer required and will be removed in a future release.")
    public val shrunkClasspathSnapshot: Path

    @Deprecated("Use `get` directly instead or a `Builder` instance to set options. This property will be removed in a future release.") // Hide in 2.4, remove in 2.7
    public val options: JvmSnapshotBasedIncrementalCompilationOptions

    public interface Builder {
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
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [JvmSnapshotBasedIncrementalCompilationConfiguration].
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
    @Deprecated("Use `JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder` to create a `Builder` instead.")
    public operator fun <V> set(key: Option<V>, value: V)
}

public interface JvmSnapshotBasedIncrementalCompilationOptions