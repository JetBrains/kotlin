/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.js

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.internal.BaseOption
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.IncrementalModuleSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.SourcesChangesSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyWithSerialNameValue
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyWithSerialNameValue
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import java.nio.file.Path

@Serializable
internal class JsHistoryBasedIncrementalCompilationConfigurationImpl(
    override val workingDirectory: Path,
    @Serializable(SourcesChangesSerializer::class)
    override val sourcesChanges: SourcesChanges,
    override val modulesInformation: List<@Serializable(IncrementalModuleSerializer::class) IncrementalModule>,
) : BaseIncrementalCompilationConfigurationImpl(), JsHistoryBasedIncrementalCompilationConfiguration,
    JsHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JsHistoryBasedIncrementalCompilationConfigurationImpl> {


    internal val historyFile: Path
        get() {
            return (this[HISTORY_FILE_DIR] ?: workingDirectory).resolve(
                IncrementalCompilerRunner.BUILD_HISTORY_FILE_NAME
            )
        }

    override fun build(): JsHistoryBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JsHistoryBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JsHistoryBasedIncrementalCompilationConfigurationImpl =
        JsHistoryBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            modulesInformation,
        ).also { it.copyFrom(this) }

    fun copyFrom(from: JsHistoryBasedIncrementalCompilationConfigurationImpl) {
        super.copyFrom(from)
        rootProjectBuildDir = from.rootProjectBuildDir
        historyFileDir = from.historyFileDir
    }

    private fun <V> get(id: String): V =
        JsHistoryBasedIncrementalCompilationConfigurationImpl::class.getPropertyWithSerialNameValue(this, id)

    private fun <V> set(id: String, value: V) {
        JsHistoryBasedIncrementalCompilationConfigurationImpl::class.setPropertyWithSerialNameValue(this, id, value)
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>): V = get(key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        set(key.id, value)
    }

    operator fun <V> get(key: Option<V>): V = get(key.id)

    operator fun <V> set(key: Option<V>, value: V) {
        set(key.id, value)
    }

    class Option<V>(id: String) : BaseOption<V>(id)

    @SerialName("ROOT_PROJECT_BUILD_DIR")
    var rootProjectBuildDir: Path? = null

    @SerialName("HISTORY_FILE_DIR")
    var historyFileDir: Path? = null

    companion object {
        val ROOT_PROJECT_BUILD_DIR: Option<Path?> = Option("ROOT_PROJECT_BUILD_DIR")
        val HISTORY_FILE_DIR: Option<Path?> = Option("HISTORY_FILE_DIR")
    }
}
