/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

import java.nio.file.Path

public interface JvmIncrementalCompilationConfiguration

/**
 * @property workingDirectory the working directory for the IC operation to store internal objects.
 * @property sourcesChanges changes in the source files, which can be unknown, to-be-calculated, or known.
 * @property dependenciesSnapshotFiles a list of paths to dependency snapshot files produced by [JvmPlatformToolchain.calculateClasspathSnapshot].
 * @property options an option set produced by [JvmCompilationOperation.makeSnapshotBasedIcOptions]
 */
public class JvmSnapshotBasedIncrementalCompilationConfiguration(
    public val workingDirectory: Path,
    public val sourcesChanges: SourcesChanges,
    public val dependenciesSnapshotFiles: List<Path>,
    public val options: JvmSnapshotBasedIncrementalCompilationOptions,
) : JvmIncrementalCompilationConfiguration

public interface JvmSnapshotBasedIncrementalCompilationOptions {
    public class Option<V> internal constructor(public val id: String)

    public operator fun <V> get(key: Option<V>): V?

    public operator fun <V> set(key: Option<V>, value: V)

    public companion object {
        @JvmField
        public val ROOT_PROJECT_DIR: Option<Path> = Option("ROOT_PROJECT_DIR")

        @JvmField
        public val MODULE_BUILD_DIR: Option<Path> = Option("MODULE_BUILD_DIR")

        @JvmField
        public val PRECISE_JAVA_TRACKING: Option<Boolean> =
            Option("PRECISE_JAVA_TRACKING")

        @JvmField
        public val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES")

        @JvmField
        public val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY")

        @JvmField
        public val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION")

        @JvmField
        public val RECOMPILATION_CLEANUP_DIRS: Option<Path> = Option("REBUILD_CLEANUP_DIRS")
    }
}
