/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.wasm

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.wasm.WasmHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import java.nio.file.Path

internal class WasmHistoryBasedIncrementalCompilationConfigurationImpl private constructor(
    override val workingDirectory: Path,
    override val sourcesChanges: SourcesChanges,
    override val modulesInformation: List<IncrementalModule>,
    override val options: Options = Options(
        WasmHistoryBasedIncrementalCompilationConfiguration::class
    ),
) : BaseIncrementalCompilationConfigurationImpl(), WasmHistoryBasedIncrementalCompilationConfiguration,
    WasmHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<WasmHistoryBasedIncrementalCompilationConfigurationImpl> {

    constructor(
        rootProjectDir: Path,
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModule>,
    ) : this(
        workingDirectory,
        sourcesChanges,
        modulesInformation,
        Options(WasmHistoryBasedIncrementalCompilationConfiguration::class)
    ) {
        initializeOptions(this::class, options)
        options[ROOT_PROJECT_DIR] = rootProjectDir
    }

    internal val historyFile: Path
        get() {
            return (this[HISTORY_FILE_DIR] ?: workingDirectory).resolve(
                IncrementalCompilerRunner.BUILD_HISTORY_FILE_NAME
            )
        }

    override fun build(): WasmHistoryBasedIncrementalCompilationConfiguration = deepCopy()

    override fun toBuilder(): WasmHistoryBasedIncrementalCompilationConfiguration.Builder = deepCopy()

    override fun deepCopy(): WasmHistoryBasedIncrementalCompilationConfigurationImpl =
        WasmHistoryBasedIncrementalCompilationConfigurationImpl(
            workingDirectory,
            sourcesChanges,
            modulesInformation,
            options.deepCopy()
        )


    @UseFromImplModuleRestricted
    override fun <V> get(key: WasmHistoryBasedIncrementalCompilationConfiguration.Option<V>): V {
        return options[key]
    }

    @UseFromImplModuleRestricted
    override fun <V> set(key: WasmHistoryBasedIncrementalCompilationConfiguration.Option<V>, value: V) {
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
        val ROOT_PROJECT_BUILD_DIR: Option<Path?> = Option("ROOT_PROJECT_BUILD_DIR", null)
        val HISTORY_FILE_DIR: Option<Path?> = Option("HISTORY_FILE_DIR", null)
    }
}
