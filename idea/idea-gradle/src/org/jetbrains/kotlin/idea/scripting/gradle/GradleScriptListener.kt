/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.core.script.configuration.listener.ScriptChangeListener

class GradleScriptListener(project: Project) : ScriptChangeListener(project) {
    init {
        // start GradleScriptInputsWatcher to track changes in gradle-configuration related files
        project.service<GradleScriptInputsWatcher>().startWatching()
    }

    override fun isApplicable(vFile: VirtualFile): Boolean {
        return GradleScriptingSupportProvider.getInstance(project).isApplicable(vFile)
    }

    override fun editorActivated(vFile: VirtualFile) {
        // do nothing
    }

    override fun documentChanged(vFile: VirtualFile) {

        GradleScriptingSupportProvider.getInstance(project).getScriptInfo(vFile)?.model?.inputs
    }
}