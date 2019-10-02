/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition

/**
 * Provides the way to loading and saving script configuration.
 *
 * @see [org.jetbrains.kotlin.idea.core.script.configuration.DefaultScriptConfigurationManager] for more details.
 */
internal interface ScriptConfigurationLoader {
    fun shouldRunInBackground(scriptDefinition: ScriptDefinition): Boolean = false

    /**
     * Implementation should load configuration and call `context.suggestNewConfiguration` or `saveNewConfiguration`.
     *
     * @return true when this loader is applicable.
     */
    fun loadDependencies(
        isFirstLoad: Boolean,
        virtualFile: VirtualFile,
        scriptDefinition: ScriptDefinition,
        context: ScriptConfigurationLoadingContext
    ): Boolean
}