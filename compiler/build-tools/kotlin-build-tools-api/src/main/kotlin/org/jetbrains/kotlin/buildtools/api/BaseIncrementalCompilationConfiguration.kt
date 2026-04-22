/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import java.nio.file.Path

@ExperimentalBuildToolsApi
public interface BaseIncrementalCompilationConfiguration {

    public interface Builder {
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
    }

    /**
     * An option for configuring a [BaseIncrementalCompilationConfiguration].
     *
     * @see get
     * @see set
     */
    public class Option<V> internal constructor(id: String, public val availableSinceVersion: KotlinReleaseVersion) : BaseOption<V>(id)

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
        public val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", KotlinReleaseVersion(2, 3, 0))

        /**
         * The build directory, used for computing relative paths for output files in the incremental compilation caches.
         *
         * If it is not specified, incremental compilation caches will be non-relocatable.
         */
        @JvmField
        public val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether incremental compilation should perform file-by-file backup of previously compiled files
         * and revert them in the case of a compilation failure
         */
        @JvmField
        public val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether caches should remain in memory
         * and not be flushed to the disk until the compilation can be marked as successful.
         */
        @JvmField
        public val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether the non-incremental mode of the incremental compiler is forced.
         * The non-incremental mode of the incremental compiler means that during the compilation,
         * the compiler will collect enough information to perform subsequent builds incrementally.
         */
        @JvmField
        public val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", KotlinReleaseVersion(2, 3, 0))

        /**
         * The directories that the compiler will clean in the case of fallback to non-incremental compilation.
         *
         * The default ones are calculated in the case of a `null` value as a set of the incremental compilation working directory
         * passed to this incremental compilation and the classes output directory from the compiler arguments.
         *
         * If the value is set explicitly, it must contain the above-mentioned default directories.
         */
        @JvmField
        public val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", KotlinReleaseVersion(2, 3, 0))

        /**
         * By default, with the K2 compiler and KMP, we recompile the whole module if any common sources are recompiled.
         * Keeping this option disabled provides consistent builds at the cost of compilation speed. (See KT-62686 for the underlying issue.)
         * Enabling this option brings back pre-K2 behavior and may potentially introduce incorrect incremental builds.
         */
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
        @JvmField
        @ExperimentalCompilerArgument
        public val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> =
            Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION", KotlinReleaseVersion(2, 3, 0))

        /**
         * Controls whether configuration inputs should be tracked to automatically trigger a full recompilation
         * when changes are detected in compiler arguments or incremental compilation settings that affect the compilation result.
         *
         * When this feature is active, the system monitors changes to compiler arguments and
         * incremental compilation settings to determine if the previous compilation state remains valid.
         * If a change is detected that could compromise the integrity of the build, the system
         * automatically triggers a full (non-incremental) recompilation to ensure a correct
         * and consistent output.
         *
         * The system specifically tracks:
         * * **Compiler arguments** that impact the final compilation result.
         * * **Incremental configuration options** that define how changes are processed.
         *
         * This option will have no effect when used with compiler versions below 2.4.0.
         *
         * @since 2.4.0
         */
        @JvmField
        public val TRACK_CONFIGURATION_INPUTS: Option<Boolean> = Option("TRACK_CONFIGURATION_INPUTS", KotlinReleaseVersion(2, 4, 0))
    }
}
