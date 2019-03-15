/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hints.ParameterHintsPassFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.PsiManager

fun refreshAllOpenEditors() {
    ParameterHintsPassFactory.forceHintsUpdateOnNextPass();
    ProjectManager.getInstance().openProjects.forEach { project ->
        val psiManager = PsiManager.getInstance(project)
        val daemonCodeAnalyzer = DaemonCodeAnalyzer.getInstance(project)
        val fileEditorManager = FileEditorManager.getInstance(project)

        DaemonCodeAnalyzer.getInstance(project).restart()

        fileEditorManager.selectedFiles.forEach {
            psiManager.findFile(it)?.let { daemonCodeAnalyzer.restart(it) }
        }
    }
}
