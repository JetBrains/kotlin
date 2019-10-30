/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.configuration.loader

import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.ScriptCompilationConfigurationResult

// TODO: rename - this is not only about dependencies anymore
internal interface ScriptConfigurationLoader {
    fun isAsync(file: KtFile, scriptDefinition: ScriptDefinition) = false

    val skipSaveToAttributes: Boolean
        get() = false

    val skipNotification: Boolean
        get() = false

    fun loadDependencies(
        firstLoad: Boolean,
        file: KtFile,
        scriptDefinition: ScriptDefinition
    ): ScriptCompilationConfigurationResult?
}