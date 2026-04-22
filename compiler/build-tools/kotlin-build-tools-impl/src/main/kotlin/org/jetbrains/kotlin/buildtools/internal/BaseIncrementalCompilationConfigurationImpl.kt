/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalCompilerArgument::class)

package org.jetbrains.kotlin.buildtools.internal

import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.arguments.ExperimentalCompilerArgument
import java.nio.file.Path

internal abstract class BaseIncrementalCompilationConfigurationImpl : BaseIncrementalCompilationConfiguration,
    BaseIncrementalCompilationConfiguration.Builder {

    protected abstract val options: Options

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = options[key]

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V = options[key]

    @OptIn(UseFromImplModuleRestricted::class)
    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V> : BaseOptionWithDefault<V> {
        constructor(id: String) : super(id)
        constructor(id: String, default: V) : super(id, default = default)
    }


    companion object {
        val ROOT_PROJECT_DIR: Option<Path?> = Option("ROOT_PROJECT_DIR", null)

        val MODULE_BUILD_DIR: Option<Path?> = Option("MODULE_BUILD_DIR", null)

        val BACKUP_CLASSES: Option<Boolean> = Option("BACKUP_CLASSES", false)

        val KEEP_IC_CACHES_IN_MEMORY: Option<Boolean> = Option("KEEP_IC_CACHES_IN_MEMORY", false)

        val FORCE_RECOMPILATION: Option<Boolean> = Option("FORCE_RECOMPILATION", false)

        val OUTPUT_DIRS: Option<Set<Path>?> = Option("OUTPUT_DIRS", null)

        val UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM: Option<Boolean> =
            Option("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM", false)

        val MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION: Option<Boolean> = Option("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION", true)

        val TRACK_CONFIGURATION_INPUTS: Option<Boolean> = Option("TRACK_CONFIGURATION_INPUTS", false)
    }
}
