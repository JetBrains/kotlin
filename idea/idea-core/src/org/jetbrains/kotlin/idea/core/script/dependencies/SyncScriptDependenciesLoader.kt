/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.script.KotlinScriptDefinition

class SyncScriptDependenciesLoader(
    file: VirtualFile,
    scriptDef: KotlinScriptDefinition,
    project: Project,
    shouldNotifyRootsChanged: Boolean
) : ScriptDependenciesLoader(file, scriptDef, project, shouldNotifyRootsChanged) {

    override fun loadDependencies() {
        val result = contentLoader.loadContentsAndResolveDependencies(scriptDef, file)
        processResult(result)
    }

    override fun shouldUseBackgroundThread(): Boolean = false
    override fun shouldShowNotification(): Boolean = false
}