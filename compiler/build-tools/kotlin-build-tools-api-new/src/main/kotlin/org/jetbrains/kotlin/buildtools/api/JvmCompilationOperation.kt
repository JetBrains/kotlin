/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

public interface JvmCompilationOperation : BuildOperation<Unit> {
    public class Option<V> internal constructor(public val id: String)

    public operator fun <V> get(key: Option<V>): V?

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
     * @see JvmSnapshotBasedIncrementalCompilationConfiguration
     */
    public fun makeSnapshotBasedIcOptions(): JvmSnapshotBasedIncrementalCompilationOptions

    public companion object {
        @JvmField
        public val INCREMENTAL_COMPILATION: Option<JvmIncrementalCompilationConfiguration> =
            Option("INCREMENTAL_COMPILATION")

        @JvmField
        public val LOOKUP_TRACKER: Option<CompilerLookupTracker> = Option("LOOKUP_TRACKER")

        @JvmField
        public val SOURCE_TO_OUTPUTS_TRACKER: Option<SourceToOutputsTracker> = Option("SOURCE_TO_OUTPUTS_TRACKER")
    }
}