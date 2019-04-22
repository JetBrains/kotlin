/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesUpdater
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition

class SyncScriptDependenciesLoader internal constructor(project: Project) : ScriptDependenciesLoader(project) {
    override fun isApplicable(file: VirtualFile): Boolean {
        val scriptDefinition = file.findScriptDefinition(project) ?: return false
        return !ScriptDependenciesUpdater.getInstance(project).isAsyncDependencyResolver(scriptDefinition)
    }

    override fun loadDependencies(file: VirtualFile) {
        val scriptDef = file.findScriptDefinition(project) ?: return
        debug(file) { "start sync dependencies loading" }
        val result = contentLoader.loadContentsAndResolveDependencies(scriptDef, file)
        debug(file) { "finish sync dependencies loading" }
        processResult(result, file, scriptDef)
    }

    override fun shouldShowNotification(): Boolean = false
}