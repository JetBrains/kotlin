/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.scriptDependencies

class FromFileAttributeScriptDependenciesLoader(project: Project) : ScriptDependenciesLoader(project) {

    override fun isApplicable(file: VirtualFile): Boolean {
        return file.scriptDependencies != null
    }

    override fun loadDependencies(file: VirtualFile) {
        val deserializedDependencies = file.scriptDependencies ?: return
        debug(file) { "dependencies from fileAttributes = $deserializedDependencies" }
        saveToCache(file, deserializedDependencies)
    }

    override fun shouldShowNotification(): Boolean = false
}