/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")
@file:UseSerializers(PathAsStringSerializer::class)

package org.jetbrains.kotlin.buildtools.internal.wasm

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.wasm.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.daemon.common.serialization.PathAsStringSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.SourcesChangesSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.WasmIncrementalModuleSerializer
import org.jetbrains.kotlin.buildtools.internal.daemon.cast
import org.jetbrains.kotlin.buildtools.internal.daemon.throwPropertyNotFound
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import java.nio.file.Path

@Serializable
internal class WasmHistoryBasedIncrementalCompilationConfigurationImpl : BaseIncrementalCompilationConfigurationImpl, WasmHistoryBasedIncrementalCompilationConfiguration,
    WasmHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<WasmHistoryBasedIncrementalCompilationConfigurationImpl> {

    constructor(
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModule>
    ) : super() {
        this.modulesInformation = modulesInformation
        this.sourcesChanges = sourcesChanges
        this.workingDirectory = workingDirectory
    }

    private constructor(other: WasmHistoryBasedIncrementalCompilationConfigurationImpl) : super(other) {
        modulesInformation = other.modulesInformation
        sourcesChanges = other.sourcesChanges
        workingDirectory = other.workingDirectory
        rootProjectBuildDir = other.rootProjectBuildDir
        historyFileDir = other.historyFileDir
    }

    override val modulesInformation: List<@Serializable(with = WasmIncrementalModuleSerializer::class) IncrementalModule>

    @Serializable(SourcesChangesSerializer::class)
    override val sourcesChanges: SourcesChanges
    override val workingDirectory: Path
    internal val historyFile: Path
        get() {
            return (historyFileDir ?: workingDirectory).resolve(
                IncrementalCompilerRunner.BUILD_HISTORY_FILE_NAME
            )
        }

    override fun build(): WasmHistoryBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): WasmHistoryBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): WasmHistoryBasedIncrementalCompilationConfigurationImpl =
        WasmHistoryBasedIncrementalCompilationConfigurationImpl(this)

    @Suppress("UNCHECKED_CAST")
    @UseFromImplModuleRestricted
    override fun <V> get(key: WasmHistoryBasedIncrementalCompilationConfiguration.Option<V>): V = when (key.id) {
        "ROOT_PROJECT_BUILD_DIR" -> rootProjectBuildDir
        "HISTORY_FILE_DIR" -> historyFileDir
        else -> throwPropertyNotFound(key.id)
    } as V

    @UseFromImplModuleRestricted
    override fun <V> set(key: WasmHistoryBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
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
