/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinReleaseVersion
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.jps.InternalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.CompilerIncrementalCompilationComponents
import org.jetbrains.kotlin.buildtools.api.jps.jvm.incremental.trackers.*
import org.jetbrains.kotlin.buildtools.api.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.trackers.CompilerLookupTracker

/**
 * A configuration for an incremental build whose state is owned by the API consumer.
 *
 * The compiler makes a single pass over exactly the sources it is given, reading what earlier compilations recorded
 * through [incrementalCompilationComponents] and reporting what this one produced through the trackers below.
 *
 * Every tracker left unset makes the next round's set of files to recompile less precise, which shows up as stale
 * output rather than as an error.
 *
 * @since 2.5.0
 */
@OptIn(ExperimentalBuildToolsApi::class)
@InternalBuildToolsApi
public interface JvmJpsManagedIncrementalCompilationConfiguration : JvmIncrementalCompilationConfiguration {

    /**
     * What earlier compilations recorded for each module taking part in this compilation.
     */
    public val incrementalCompilationComponents: CompilerIncrementalCompilationComponents

    /**
     * Get the value for option specified by [key] if it was previously [Builder.set] or if it has a default value.
     *
     * @return the previously set value for an option
     * @throws IllegalStateException if the option was not set and has no default value
     */
    public operator fun <V> get(key: Option<V>): V

    /**
     * An option for configuring [JvmJpsManagedIncrementalCompilationConfiguration].
     *
     * @see get
     * @see Builder.set
     */
    public class Option<V> internal constructor(
        id: String,
        public val availableSinceVersion: KotlinReleaseVersion,
    ) : BaseOption<V>(id)

    /**
     * A builder for [JvmJpsManagedIncrementalCompilationConfiguration].
     */
    public interface Builder {
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

        /**
         * Creates an immutable instance of [JvmJpsManagedIncrementalCompilationConfiguration] based on the configuration of this
         * builder.
         */
        public fun build(): JvmJpsManagedIncrementalCompilationConfiguration
    }

    /**
     * Creates a builder for [JvmJpsManagedIncrementalCompilationConfiguration] that contains a copy of this configuration.
     */
    public fun toBuilder(): Builder

    public companion object {
        /**
         * A tracker informed whenever the compiler looks up a reference.
         *
         * Without it, a consumer cannot tell which files referred to a declaration that later changes, so it has no
         * basis for recompiling their use sites.
         *
         * This is the tracker for a build whose incremental state this configuration describes; it is separate from
         * [org.jetbrains.kotlin.buildtools.api.BaseCompilationOperation.LOOKUP_TRACKER], which reports the lookups of
         * a compilation the implementation keeps the state for.
         */
        @JvmField
        public val LOOKUP_TRACKER: Option<CompilerLookupTracker?> =
            Option("LOOKUP_TRACKER", KotlinReleaseVersion(2, 5, 0))

        /**
         * A tracker informed which source files produced each file the compiler writes.
         *
         * Without it, a consumer cannot tell which outputs to delete or replace when a source file changes or is
         * removed.
         */
        @JvmField
        public val FILE_MAPPING_TRACKER: Option<CompilerFileMappingTracker?> =
            Option("FILE_MAPPING_TRACKER", KotlinReleaseVersion(2, 5, 0))

        /**
         * A tracker informed whenever the compiler encounters a `when` expression over a Java enum.
         *
         * Relevant to projects with Java sources only. Without it, adding or removing an enum entry may not recompile
         * the `when` expressions over it.
         */
        @JvmField
        public val ENUM_WHEN_TRACKER: Option<CompilerEnumWhenTracker?> =
            Option("ENUM_WHEN_TRACKER", KotlinReleaseVersion(2, 5, 0))

        /**
         * A tracker informed of the import directives the compiler resolves.
         *
         * Without it, removing an imported declaration may not recompile the files importing it.
         */
        @JvmField
        public val IMPORT_TRACKER: Option<CompilerImportTracker?> =
            Option("IMPORT_TRACKER", KotlinReleaseVersion(2, 5, 0))

        /**
         * A tracker informed whenever the value of a Java constant is copied into the code being compiled.
         *
         * Relevant to projects with Java sources only. Without it, changing a constant's value may not recompile the
         * files that inlined it, leaving them holding the old value.
         */
        @JvmField
        public val INLINE_CONST_TRACKER: Option<CompilerInlineConstTracker?> =
            Option("INLINE_CONST_TRACKER", KotlinReleaseVersion(2, 5, 0))

        /**
         * A tracker informed which source files an `expect` declaration and its `actual` counterpart live in.
         *
         * Relevant to multiplatform projects only. Without it, changing an `expect` declaration may not recompile
         * the `actual` declaration implementing it, and the other way round, leaving the two out of sync.
         */
        @JvmField
        public val EXPECT_ACTUAL_TRACKER: Option<CompilerExpectActualTracker?> =
            Option("EXPECT_ACTUAL_TRACKER", KotlinReleaseVersion(2, 5, 0))
    }
}
