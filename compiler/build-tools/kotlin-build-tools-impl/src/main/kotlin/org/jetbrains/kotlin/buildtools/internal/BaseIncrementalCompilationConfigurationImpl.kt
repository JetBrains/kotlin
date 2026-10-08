/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.BaseIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.internal.jvm.JvmSnapshotBasedIncrementalCompilationConfigurationImpl
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import java.nio.file.Path

@Serializable
internal abstract class BaseIncrementalCompilationConfigurationImpl : BaseIncrementalCompilationConfiguration,
    BaseIncrementalCompilationConfiguration.Builder {

    private fun <V> get(id: String): V =
        BaseIncrementalCompilationConfigurationImpl::class.getPropertyWithSerialNameValue(this, id)

    private fun <V> set(id: String, value: V) {
        BaseIncrementalCompilationConfigurationImpl::class.setPropertyWithSerialNameValue(this, id, value)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: BaseIncrementalCompilationConfiguration.Option<V>): V = get(key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: BaseIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        set(key.id, value)
    }

    operator fun <V> get(key: Option<V>): V = get(key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        set(key.id, value)
    }

    protected fun copyFrom(from: BaseIncrementalCompilationConfigurationImpl) {
        rootProjectDir = from.rootProjectDir
        moduleBuildDir = from.moduleBuildDir
        backupClasses = from.backupClasses
        keepIcCachesInMemory = from.keepIcCachesInMemory
        forceRecompilation = from.forceRecompilation
        outputDirs = from.outputDirs
        unsafeIncrementalCompilationForMultiplatform = from.unsafeIncrementalCompilationForMultiplatform
        monotonousIncrementalCompileSetExpansion = from.monotonousIncrementalCompileSetExpansion
        trackConfigurationInputs = from.trackConfigurationInputs
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    @SerialName("ROOT_PROJECT_DIR")
    var rootProjectDir: Path? = null

    @SerialName("MODULE_BUILD_DIR")
    var moduleBuildDir: Path? = null

    @SerialName("BACKUP_CLASSES")
    var backupClasses: Boolean = false

    @SerialName("KEEP_IC_CACHES_IN_MEMORY")
    var keepIcCachesInMemory: Boolean = false

    @SerialName("FORCE_RECOMPILATION")
    var forceRecompilation: Boolean = false

    @SerialName("OUTPUT_DIRS")
    var outputDirs: Set<Path>? = null

    @SerialName("UNSAFE_INCREMENTAL_COMPILATION_FOR_MULTIPLATFORM")
    var unsafeIncrementalCompilationForMultiplatform: Boolean = false

    @SerialName("MONOTONOUS_INCREMENTAL_COMPILE_SET_EXPANSION")
    var monotonousIncrementalCompileSetExpansion: Boolean = true

    @SerialName("TRACK_CONFIGURATION_INPUTS")
    var trackConfigurationInputs: Boolean = false

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
