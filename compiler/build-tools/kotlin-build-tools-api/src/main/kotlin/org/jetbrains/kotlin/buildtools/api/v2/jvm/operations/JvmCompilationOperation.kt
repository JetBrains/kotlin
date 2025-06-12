/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.jvm.operations

import org.jetbrains.kotlin.buildtools.api.CompilationResult
import org.jetbrains.kotlin.buildtools.api.v2.trackers.CompilerLookupTracker
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.api.v2.trackers.SourceToOutputsTracker
import org.jetbrains.kotlin.buildtools.api.v2.BuildOperation
import org.jetbrains.kotlin.buildtools.api.v2.JvmCompilerArguments
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.Mandatory
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.WithDefault

public interface JvmCompilationOperation : BuildOperation<CompilationResult> {
    public interface Option<V>

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public val compilerArguments: JvmCompilerArguments

    /**
     * Creates an options set for snapshot-based incremental compilation (IC) in JVM projects.
     * May be used to observe the defaults, adjust them, and configure incremental compilation as follows:
     * ```
     * val icOptions = compilation.makeSnapshotBasedIcOptions()
     *
     * icOptions[JvmIncrementalCompilationOptions.BACKUP_CLASSES] = true
     *
     * compilation[JvmCompilationOperation.INCREMENTAL_COMPILATION] = JvmIncrementalCompilationConfiguration(
     *     workingDirectory = Paths.get("build/kotlin"),
     *     sourcesChanges = SourcesChanges.ToBeCalculated,
     *     dependenciesSnapshotFiles = snapshots,
     *     options = icOptions,
     * )
     * ```
     * @see org.jetbrains.kotlin.buildtools.api.v2.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
     */
    public fun createSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions

    public companion object {
        private fun <V> optional(id: String, defaultValue: V): Option<V> =
            object : WithDefault<V>(id, defaultValue), Option<V> {}


        @JvmField
        public val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration?> =
            optional("INCREMENTAL_COMPILATION", null)

        @JvmField
        public val LOOKUP_TRACKER: Option<CompilerLookupTracker?> = optional("LOOKUP_TRACKER", null)

        @JvmField
        public val SOURCE_TO_OUTPUTS_TRACKER: Option<SourceToOutputsTracker?> = optional("SOURCE_TO_OUTPUTS_TRACKER", null)

        @JvmField
        public val KOTLINSCRIPT_EXTENSIONS: Option<Array<String>?> = optional("KOTLINSCRIPT_EXTENSIONS", null)
    }
}