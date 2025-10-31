/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.jvm

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationOptions
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import java.nio.file.Path

internal class JvmSnapshotBasedIncrementalCompilationConfigurationImpl(
    workingDirectory: Path,
    sourcesChanges: SourcesChanges,
    dependenciesSnapshotFiles: List<Path>,
    shrunkClasspathSnapshot: Path,
    @Deprecated("Use `get` and `set` directly instead. This property will be removed in a future release.") // Remove in 2.7
    override val options: JvmSnapshotBasedIncrementalCompilationOptionsImpl = JvmSnapshotBasedIncrementalCompilationOptionsImpl(
        Options(
            JvmSnapshotBasedIncrementalCompilationConfiguration::class
        )
    ),
) : JvmSnapshotBasedIncrementalCompilationConfiguration(
    workingDirectory,
    sourcesChanges,
    dependenciesSnapshotFiles,
    shrunkClasspathSnapshot,
    options
), HasSnapshotBasedIcOptionsAccessor {

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>): V {
        return options.options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        options.options[key] = value
    }

    override operator fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>): V {
        return options.options[key]
    }

    operator fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>, value: V) {
        options.options[key] = value
    }
}

// Remove in 2.7
@Deprecated("Use `JvmSnapshotBasedIncrementalCompilationConfiguration` and `JvmCompilationOperation.snapshotBasedIcConfigurationBuilder`. This interface will be removed in a future release.")
internal class JvmSnapshotBasedIncrementalCompilationOptionsImpl internal constructor(
    internal val options: Options = Options(
        JvmSnapshotBasedIncrementalCompilationOptions::class
    ),
) : JvmSnapshotBasedIncrementalCompilationOptions, HasSnapshotBasedIcOptionsAccessor {

    constructor() : this(Options(JvmSnapshotBasedIncrementalCompilationOptions::class))

    override operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: JvmSnapshotBasedIncrementalCompilationOptions.Option<V>, value: V) {
        options[key] = value
    }

    open class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", null)

        val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", null)

        val PRECISE_JAVA_TRACKING: Option<Boolean> = Option("PRECISE_JAVA_TRACKING", false)

        val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", false)

        val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", false)

        val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", false)

        val RECOMPILATION_CLEANUP_DIRS: Option<Path> = Option("REBUILD_CLEANUP_DIRS")

        val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", null)

        val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)

        val USE_FIR_RUNNER: Option<Boolean> = Option("USE_FIR_RUNNER", false)

        val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM", false)

        val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION", false)
    }
}

// Remove in 2.7
internal interface HasSnapshotBasedIcOptionsAccessor {
    operator fun <V> get(key: JvmSnapshotBasedIncrementalCompilationOptionsImpl.Option<V>): V
}

// Remove in 2.7
@Suppress("DEPRECATION")
internal fun JvmSnapshotBasedIncrementalCompilationConfiguration.toOptions(): HasSnapshotBasedIcOptionsAccessor {
    // In older BTA-APIs JvmSnapshotBasedIncrementalCompilationConfiguration is final,
    // so we have to avoid loading JvmSnapshotBasedIncrementalCompilationConfigurationImpl, or we'd get a verification error

    return if (JvmSnapshotBasedIncrementalCompilationConfiguration::class.isFinal || this !is JvmSnapshotBasedIncrementalCompilationConfigurationImpl) {
        // we're on an older BTA-API or user created JvmSnapshotBasedIncrementalCompilationConfiguration through the deprecated constructor directly
        options as JvmSnapshotBasedIncrementalCompilationOptionsImpl
    } else {
        // we're on a newer BTA-API and user created JvmSnapshotBasedIncrementalCompilationConfiguration through the factory method
        this
    }
}