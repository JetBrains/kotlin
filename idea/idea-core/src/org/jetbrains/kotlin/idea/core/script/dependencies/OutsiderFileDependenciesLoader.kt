/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.script.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.highlighter.OutsidersPsiFileSupportUtils
import org.jetbrains.kotlin.idea.highlighter.OutsidersPsiFileSupportWrapper

class OutsiderFileDependenciesLoader(project: Project) : ScriptDependenciesLoader(project) {
    override fun isApplicable(file: VirtualFile): Boolean {
        return OutsidersPsiFileSupportWrapper.isOutsiderFile(file)
    }

    override fun loadDependencies(file: VirtualFile) {
        val fileOrigin = OutsidersPsiFileSupportUtils.getOutsiderFileOrigin(project, file) ?: return
        val compilationConfiguration = ScriptDependenciesManager.getInstance(project).getRefinedCompilationConfiguration(fileOrigin) ?: return
        saveToCache(file, compilationConfiguration)
    }

    override fun shouldShowNotification(): Boolean = false
}