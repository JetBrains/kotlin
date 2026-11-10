/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.daemon.common.serialization.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.cast
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound
import java.nio.file.Path

@Serializable
internal abstract class BaseIncrementalCompilationConfigurationImpl : BaseIncrementalCompilationConfiguration,
    BaseIncrementalCompilationConfiguration.Builder {

    protected constructor() : super()
    protected constructor(other: BaseIncrementalCompilationConfigurationImpl) : super() {
        rootProjectDir = other.rootProjectDir
        moduleBuildDir = other.moduleBuildDir
        backupClasses = other.backupClasses
        keepIcCachesInMemory = other.keepIcCachesInMemory
        forceRecompilation = other.forceRecompilation
        outputDirs = other.outputDirs
        unsafeIncrementalCompilationForMultiplatform = other.unsafeIncrementalCompilationForMultiplatform
        monotonousIncrementalCompileSetExpansion = other.monotonousIncrementalCompileSetExpansion
        trackConfigurationInputs = other.trackConfigurationInputs
    }

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = when (key.id) {
        "ROOT_PROJECT_DIR" -> rootProjectDir
        "MODULE_BUILD_DIR" -> moduleBuildDir
        "BACKUP_CLASSES" -> backupClasses
        "KEEP_IC_CACHES_IN_MEMORY" -> keepIcCachesInMemory
        "FORCE_RECOMPILATION" -> forceRecompilation
        "OUTPUT_DIRS" -> outputDirs
        "UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM" -> unsafeIncrementalCompilationForMultiplatform
        "MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION" -> monotonousIncrementalCompileSetExpansion
        "TRACK_CONFIGURATION_INPUTS" -> trackConfigurationInputs
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "ROOT_PROJECT_DIR" -> rootProjectDir = value.cast()
            "MODULE_BUILD_DIR" -> moduleBuildDir = value.cast()
            "BACKUP_CLASSES" -> backupClasses = value.cast()
            "KEEP_IC_CACHES_IN_MEMORY" -> keepIcCachesInMemory = value.cast()
            "FORCE_RECOMPILATION" -> forceRecompilation = value.cast()
            "OUTPUT_DIRS" -> outputDirs = value.cast()
            "UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM" -> unsafeIncrementalCompilationForMultiplatform = value.cast()
            "MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION" -> monotonousIncrementalCompileSetExpansion = value.cast()
            "TRACK_CONFIGURATION_INPUTS" -> trackConfigurationInputs = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    var rootProjectDir: Path? = null
    var moduleBuildDir: Path? = null
    var backupClasses: Boolean = false
    var keepIcCachesInMemory: Boolean = false
    var forceRecompilation: Boolean = false
    var outputDirs: Set<Path>? = null
    var unsafeIncrementalCompilationForMultiplatform: Boolean = false
    var monotonousIncrementalCompileSetExpansion: Boolean = true
    var trackConfigurationInputs: Boolean = false

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

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
