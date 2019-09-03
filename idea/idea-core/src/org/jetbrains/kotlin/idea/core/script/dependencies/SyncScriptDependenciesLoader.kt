/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptsCompilationConfigurationUpdater
import org.jetbrains.kotlin.scripting.definitions.ScriptDefinition
import org.jetbrains.kotlin.scripting.resolve.VirtualFileScriptSource
import org.jetbrains.kotlin.scripting.resolve.refineScriptCompilationConfiguration

class SyncScriptDependenciesLoader(project: Project) : ScriptDependenciesLoader(project) {
    override fun isApplicable(
        file: VirtualFile,
        scriptDefinition: ScriptDefinition
    ): Boolean {
        return !ScriptsCompilationConfigurationUpdater.getInstance(project).isAsyncDependencyResolver(scriptDefinition)
    }

    override fun loadDependencies(
        file: VirtualFile,
        scriptDefinition: ScriptDefinition
    ) {
        debug(file) { "start sync dependencies loading" }
        val result = refineScriptCompilationConfiguration(VirtualFileScriptSource(file), scriptDefinition, project)
        debug(file) { "finish sync dependencies loading" }
        processRefinedConfiguration(result, file)
    }

    override fun shouldShowNotification(): Boolean = false
}