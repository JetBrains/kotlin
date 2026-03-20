/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.js

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration.Option
import org.jetbrains.kotlin.buildtools.api.jvm.JvmSnapshotBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.BaseOptionWithDefault
import org.jetbrains.kotlin.buildtools.internal.DeepCopyable
import org.jetbrains.kotlin.buildtools.internal.Options
import org.jetbrains.kotlin.buildtools.internal.UseFromImplModuleRestricted
import java.nio.file.Path

internal class JsHistoryBasedIncrementalCompilationConfigurationImpl(
    override val workingDirectory: Path,
    override val sourcesChanges: SourcesChanges,
    override val modulesInformation: List<IncrementalModule>,
    val options: Options = Options(
        JvmSnapshotBasedIncrementalCompilationConfiguration::class
    ),
) : JsHistoryBasedIncrementalCompilationConfiguration, JsHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JsHistoryBasedIncrementalCompilationConfigurationImpl> {

    override fun build(): JsHistoryBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JsHistoryBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JsHistoryBasedIncrementalCompilationConfigurationImpl =
        JsHistoryBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            modulesInformation,
            options.deepCopy()
        )


    @UseFromImplModuleRestricted
    override fun <V> get(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V {
        return options[key]
    }

    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }

    companion object {
        val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", null)

        val ROOT_PROJECT_BUILD_DIR: Option<Path?> = Option("ROOT_PROJECT_BUILD_DIR", null)

        val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", null)

        val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", false)

        val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", false)

        val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", false)

        val RECOMPILATION_CLEANUP_DIRS: Option<Path> = Option("REBUILD_CLEANUP_DIRS")

        val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", null)

        val ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES: Option<Boolean> =
            Option("ASSURED_NO_CLASSPATH_SNAPSHOT_CHANGES", false)

        val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM", false)

        val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION", false)
    }
}