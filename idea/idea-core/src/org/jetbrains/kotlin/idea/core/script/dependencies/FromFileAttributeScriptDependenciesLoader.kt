/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.scriptDependencies
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import kotlin.script.experimental.dependencies.ScriptDependencies

class FromFileAttributeScriptDependenciesLoader(
    file: VirtualFile,
    scriptDef: KotlinScriptDefinition,
    project: Project
) : ScriptDependenciesLoader(file, scriptDef, project, true) {

    override fun loadDependencies() {
        val deserializedDependencies = file.scriptDependencies ?: return
        saveToCache(deserializedDependencies)
    }

    private fun saveToCache(deserialized: ScriptDependencies) {
        val rootsChanged = cache.hasNotCachedRoots(deserialized)
        cache.save(file, deserialized)
        if (rootsChanged) {
            notifyRootsChanged()
        }
    }

    override fun shouldUseBackgroundThread() = false
    override fun shouldShowNotification(): Boolean = false
}