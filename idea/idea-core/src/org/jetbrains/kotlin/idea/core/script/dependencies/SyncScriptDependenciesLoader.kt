/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition

class SyncScriptDependenciesLoader internal constructor(project: Project) : ScriptDependenciesLoader(project) {
    override fun loadDependencies(file: VirtualFile, scriptDef: KotlinScriptDefinition) {
        val result = contentLoader.loadContentsAndResolveDependencies(scriptDef, file)
        processResult(result, file, scriptDef)
    }

    override fun shouldShowNotification(): Boolean = false
}