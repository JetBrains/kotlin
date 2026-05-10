/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.buildtools.internal.js

import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.js.IncrementalModule
import org.jetbrains.kotlin.buildtools.api.js.JsHistoryBasedIncrementalCompilationConfiguration
import org.jetbrains.kotlin.buildtools.internal.*
import org.jetbrains.kotlin.incremental.IncrementalCompilerRunner
import java.nio.file.Path

internal class JsHistoryBasedIncrementalCompilationConfigurationImpl private constructor(
    override val workingDirectory: Path,
    override val sourcesChanges: SourcesChanges,
    override val modulesInformation: List<IncrementalModule>,
    override val options: Options = Options(
        JsHistoryBasedIncrementalCompilationConfiguration::class
    ),
) : BaseIncrementalCompilationConfigurationImpl(), JsHistoryBasedIncrementalCompilationConfiguration,
    JsHistoryBasedIncrementalCompilationConfiguration.Builder,
    DeepCopyable<JsHistoryBasedIncrementalCompilationConfigurationImpl> {

    constructor(
        rootProjectDir: Path,
        workingDirectory: Path,
        sourcesChanges: SourcesChanges,
        modulesInformation: List<IncrementalModule>,
    ) : this(
        workingDirectory,
        sourcesChanges,
        modulesInformation,
        Options(JsHistoryBasedIncrementalCompilationConfiguration::class)
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
        checkOptionIsAvailableForVersion(key)
        options[key] = value
    }

    operator fun <V> get(key: Option<V>): V {
        return options[key]
    }

    operator fun <V> set(key: Option<V>, value: V) {
        options[key] = value
    }

    class Option<V>(id: String, default: V) : BaseOptionWithDefault<V>(id, defaultValue = default)

    companion object {
        val ROOT_PROJECT_BUILD_DIR: Option<Path?> = Option("ROOT_PROJECT_BUILD_DIR", null)
        val HISTORY_FILE_DIR: Option<Path?> = Option("HISTORY_FILE_DIR", null)
    }
}
