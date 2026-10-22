/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.js

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.buildtools.internal.daemon.JsIncrementalModuleSerializer
import org.jetbrains.kotlin.daemon.common.serialization.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.SourcesChangesSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.cast
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import java.nio.file.Path

@Serializable
internal class JsHistoryBasedIncrementalCompilationConfigurationImpl : BaseIncrementalCompilationConfigurationImpl, JsHistoryBasedIncrementalCompilationConfiguration,
    JsHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JsHistoryBasedIncrementalCompilationConfigurationImpl> {

    constructor(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModule>
    ) : super() {
        this.modulesInformation = modulesInformation
        this.sourcesChanges = sourcesChanges
        this.workingDirectory = workingDirectory
    }

    private constructor(other: JsHistoryBasedIncrementalCompilationConfigurationImpl) : super(other) {
        modulesInformation = other.modulesInformation
        sourcesChanges = other.sourcesChanges
        workingDirectory = other.workingDirectory
        rootProjectBuildDir = other.rootProjectBuildDir
        historyFileDir = other.historyFileDir
    }

    override val modulesInformation: List<@Serializable(with = JsIncrementalModuleSerializer::class) IncrementalModule>

    @Serializable(SourcesChangesSerializer::class)
    override val sourcesChanges: SourcesChanges
    override val workingDirectory: Path

    internal val historyFile: Path
        get() {
            return (historyFileDir ?: workingDirectory).resolve(
                IncrementalCompilerRunner.BUILD_HISTORY_FILE_NAME
            )
        }

    override fun build(): JsHistoryBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): JsHistoryBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): JsHistoryBasedIncrementalCompilationConfigurationImpl =
        JsHistoryBasedIncrementalCompilationConfigurationImpl(this)

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>): V = when (key.id) {
        "ROOT_PROJECT_BUILD_DIR" -> rootProjectBuildDir
        "HISTORY_FILE_DIR" -> historyFileDir
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: JsHistoryBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
        checkOptionIsAvailableForVersion(key)
        when (key.id) {
            "ROOT_PROJECT_BUILD_DIR" -> rootProjectBuildDir = value.cast()
            "HISTORY_FILE_DIR" -> historyFileDir = value.cast()
            else -> throwPropertyNotFound(key.id)
        }
    }

    var rootProjectBuildDir: Path? = null
    var historyFileDir: Path? = null
}
