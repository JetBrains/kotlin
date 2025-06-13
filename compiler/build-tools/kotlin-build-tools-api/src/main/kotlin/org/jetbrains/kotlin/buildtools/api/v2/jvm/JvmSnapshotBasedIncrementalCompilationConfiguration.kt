/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.v2.jvm

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.WithDefault
import org.jetbrains.kotlin.buildtools.api.v2.internal.Option.Mandatory
import java.nio.file.Path

public interface JvmIncrementalCompilationConfiguration

/**
 * @property workingDirectory the working directory for the IC operation to store internal objects.
 * @property sourcesChanges changes in the source files, which can be unknown, to-be-calculated, or known.
 * @property dependenciesSnapshotFiles a list of paths to dependency snapshot files produced by [JvmPlatformToolchain.calculateClasspathSnapshot].
 * @property options an option set produced by [org.jetbrains.kotlin.buildtools.api.v2.jvm.operations.JvmCompilationOperation.createSnapshotBasedIcOptions]
 */
public class JvmSnapshotBasedIncrementalCompilationConfiguration(
    public val workingDirectory: Path,
    public val sourcesChanges: SourcesChanges,
    public val dependenciesSnapshotFiles: List<Path>,
    public val shrunkClasspathSnapshot: Path,
    public val options: JvmSnapshotBasedIncrementalCompilationOptions,
) : JvmIncrementalCompilationConfiguration

public interface JvmSnapshotBasedIncrementalCompilationOptions {
    public interface Option<V>

    public operator fun <V> get(key: Option<V>): V

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {

        private fun <V> mandatory(id: String): Option<V> =
            object : Mandatory(id), Option<V> {}

        private fun <V> optional(id: String, defaultValue: V): Option<V> =
            object : WithDefault<V>(id, defaultValue), Option<V> {}

        @JvmField
        public val ROOT_PROJECT_DIR: Option<Path> = mandatory("ROOT_PROJECT_DIR")

        @JvmField
        public val MODULE_BUILD_DIR: Option<Path> = mandatory("MODULE_BUILD_DIR")

        @JvmField
        public val PRECISE_JAVA_TRACKING: Option<Boolean> =
            optional("PRECISE_JAVA_TRACKING", true)

        @JvmField
        public val BACKUP_CLASSES: Option<Boolean> = optional("BACKUP_CLASSES", false)

        @JvmField
        public val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = optional("KEEP_IC_CACHES_IN_MEMORY", false)

        @JvmField
        public val FORCE_RECOMPILATION: Option<Boolean> = optional("FORCE_RECOMPILATION", false)

        @JvmField
        public val RECOMPILATION_CLEANUP_DIRS: Option<Path> = mandatory("REBUILD_CLEANUP_DIRS")

        @JvmField
        public val OUTPUT_DIRS: Option<Set<Path>?> = optional("OUTPUT_DIRS", null)

        @JvmField
        public val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            optional("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)

        @JvmField
        public val USE_FIR_RUNNER: Option<Boolean> = optional("USE_FIR_RUNNER", false)
    }
}
