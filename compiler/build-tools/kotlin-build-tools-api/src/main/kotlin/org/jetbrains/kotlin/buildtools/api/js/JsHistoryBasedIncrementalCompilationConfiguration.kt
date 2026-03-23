/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.js

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jvm.operations.JvmCompilationOperation
import java.nio.file.Path

/**
 * Represents a configuration that enables incremental compilation.
 *
 * This interface is not intended to be implemented by the API consumers.
 *
 * Currently, the only supported implementation of this interface is [JsHistoryBasedIncrementalCompilationConfiguration],
 * but more may be added in the future.
 *
 * @see JsHistoryBasedIncrementalCompilationConfiguration
 */
@ExperimentalBuildToolsApi
public interface JsIncrementalCompilationConfiguration

/**
 * A configuration for incremental compilation based on snapshots.
 *
 * @property workingDirectory the working directory for the IC operation to store internal objects.
 * @property sourcesChanges changes in the source files, which can be unknown, to-be-calculated, or known.
 *
 * @see JvmCompilationOperation.Builder.snapshotBasedIcConfigurationBuilder
 */
@ExperimentalBuildToolsApi
public interface JsHistoryBasedIncrementalCompilationConfiguration : JsIncrementalCompilationConfiguration,
    BaseIncrementalCompilationConfiguration {

    public val workingDirectory: Path
    public val sourcesChanges: SourcesChanges
    public val modulesInformation: List<IncrementalModule>

    /**
     * A builder for [JsHistoryBasedIncrementalCompilationConfiguration].
     *
     * @since 2.4.0
     */
    public interface Builder : BaseIncrementalCompilationConfiguration.Builder {
        /**
         * The working directory for the IC operation to store internal objects
         *
         * @since 2.4.0
         */
        public val workingDirectory: Path

        /**
         * Changes in the source files, which can be unknown, to-be-calculated, or known
         *
         * @since 2.4.0
         */
        public val sourcesChanges: SourcesChanges

        /**
         * TODO
         */
        public val modulesInformation: List<IncrementalModule>

        /**
         * Get the value for option specified by [key] if it was previously [set] or if it has a default value.
         *
         * @return the previously set value for an option
         * @throws IllegalStateException if the option was not set and has no default value
         * @since 2.4.0
         */
        public operator fun <V> get(key: Option<V>): V

        /**
         * Set the [value] for option specified by [key], overriding any previous value for that option.
         *
         * @since 2.4.0
         */
        public operator fun <V> set(key: Option<V>, value: V)

        /**
         * Creates an immutable instance of [JsHistoryBasedIncrementalCompilationConfiguration] based on the configuration of this builder.
         *
         * @since 2.4.0
         */
        public fun build(): JsHistoryBasedIncrementalCompilationConfiguration
    }

    /**
     * Creates a builder for [JsHistoryBasedIncrementalCompilationConfiguration] that contains a copy of this configuration.
     *
     * @since 2.4.0
     */
    public fun toBuilder(): Builder

    /**
     * An option for configuring a [JsHistoryBasedIncrementalCompilationConfiguration].
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

    public companion object {
        /**
         * The root project directory, used for computing relative paths for source files in the incremental compilation caches.
         *
         * If it is not specified, incremental compilation caches will be non-relocatable.
         */
        @JvmField
        public val ROOT_PROJECT_BUILD_DIR: Option<Path?> = Option("ROOT_PROJECT_BUILD_DIR")

        /**
         * The directory where the build history files will be stored.
         *
         * The contents of this directory should not be cached, as they are only valid for subsequent local executions.
         */
        @JvmField
        public val HISTORY_FILE_DIR: Option<Path?> = Option("HISTORY_FILE_DIR")
    }
}