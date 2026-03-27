/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import java.nio.file.Path

public class JvmSnapshotBasedIncrementalCompilationConfigurationImpl2(
    override val workingDirectory: Path,
    override val sourcesChanges: SourcesChanges,
    override val dependenciesSnapshotFiles: List<Path>,
    @Deprecated("This property is no longer required and will be removed in a future release.")
    override val shrunkClasspathSnapshot: Path,
    @Deprecated("Use `get` directly instead or a `Builder` instance to set options. This property will be removed in a future release.")
    override val options: JvmSnapshotBasedIncrementalCompilationOptions
) : JvmSnapshotBasedIncrementalCompilationConfiguration, JvmSnapshotBasedIncrementalCompilationConfiguration.Builder, DeepCopyable<JvmSnapshotBasedIncrementalCompilationConfigurationImpl2>, HasSnapshotBasedIcOptionsAccessor {
    override fun toBuilder(): JvmSnapshotBasedIncrementalCompilationConfiguration.Builder {
        TODO("Not yet implemented!!1")
    }

    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>): V {
        TODO("Not yet implemented!!1")
    }

    override fun <V> set(
        key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>,
        value: V,
    ) {
        TODO("Not yet implemented!!1")
    }

    override fun build(): JvmSnapshotBasedIncrementalCompilationConfiguration {
        TODO("Not yet implemented!!1")
    }

    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>): V {
        TODO("Not yet implemented!!1")
    }

    override fun deepCopy(): JvmSnapshotBasedIncrementalCompilationConfigurationImpl2 {
        TODO("Not yet implemented!!1")
    }

}