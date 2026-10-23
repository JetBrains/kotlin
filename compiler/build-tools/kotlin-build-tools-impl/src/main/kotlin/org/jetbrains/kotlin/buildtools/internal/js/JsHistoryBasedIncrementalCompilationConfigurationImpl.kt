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
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.serializability.JsIncrementalModuleSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.SourcesChangesSerializer
import org.jetbrains.kotlin.buildtools.internal.serializability.getPropertyByName
import org.jetbrains.kotlin.buildtools.internal.serializability.setPropertyByName
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import java.nio.file.Path

@Serializable
internal class JsHistoryBasedIncrementalCompilationConfigurationImpl(
    override val workingDirectory: Path,
    @Serializable(SourcesChangesSerializer::class)
    override val sourcesChanges: SourcesChanges,
    override val modulesInformation: List<@Serializable(JsIncrementalModuleSerializer::class) IncrementalModule>,
) : BaseIncrementalCompilationConfigurationImpl(), JsHistoryBasedIncrementalCompilationConfiguration,
    JsHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JsHistoryBasedIncrementalCompilationConfigurationImpl> {

    internal val historyFile: Path
        get() {
            return (historyFileDir ?: workingDirectory).resolve(
                IncrementalCompilerRunner.BUILD_HISTORY_FILE_NAME
            )
        }

    override fun build(): JsHistoryBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JsHistoryBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JsHistoryBasedIncrementalCompilationConfigurationImpl =
        JsHistoryBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            modulesInformation
        ).also { it.copyFrom(this) }


    private fun copyFrom(other: JsHistoryBasedIncrementalCompilationConfigurationImpl) {
        super.copyFrom(other)
        rootProjectBuildDir = other.rootProjectBuildDir
        historyFileDir = other.historyFileDir
    }

    @UseFromImplModuleRestricted
    override fun <V> get(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>): V = getPropertyByName(propertyAccessMap, key.id)

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        setPropertyByName(propertyAccessMap, key.id, value)
    }

    @SerialName("ROOT_PROJECT_BUILD_DIR")
    var rootProjectBuildDir: Path? = null

    @SerialName("HISTORY_FILE_DIR")
    var historyFileDir: Path? = null

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        private val propertyAccessMap = mapOf(
            "ROOT_PROJECT_BUILD_DIR" to JsHistoryBasedIncrementalCompilationConfigurationImpl::rootProjectBuildDir,
            "HISTORY_FILE_DIR" to JsHistoryBasedIncrementalCompilationConfigurationImpl::historyFileDir,
        )
    }
}
