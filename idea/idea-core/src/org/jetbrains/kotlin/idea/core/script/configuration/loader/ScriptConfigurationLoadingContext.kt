/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationFileAttributeCache
import org.jetbrains.kotlin.idea.core.script.configuration.cache.ScriptConfigurationSnapshot

interface ScriptConfigurationLoadingContext {
    fun getCachedConfiguration(file: VirtualFile): ScriptConfigurationSnapshot?

    /**
     * Show notification about new configuration with suggestion to apply it.
     * User may disable this notifications, in this case configuration will be saved immediately.
     *
     * If configuration is null, then the result will be treated as failed, and
     * reports will be displayed immediately.
     *
     * @sample DefaultScriptConfigurationLoader.loadDependencies
     */
    fun suggestNewConfiguration(
        file: VirtualFile,
        newResult: ScriptConfigurationSnapshot
    )

    /**
     * Save [newResult] for [file] into caches and update highlighting.
     *
     * @sample ScriptConfigurationFileAttributeCache.loadDependencies
     */
    fun saveNewConfiguration(
        file: VirtualFile,
        newResult: ScriptConfigurationSnapshot
    )
}