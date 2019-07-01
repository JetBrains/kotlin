/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable

fun commitAllDocuments() {
    ProjectManagerEx.getInstanceEx().openProjects.forEach { project ->
        val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase

        EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
            psiDocumentManagerBase.clearUncommittedDocuments()
            psiDocumentManagerBase.commitAllDocuments()
        })
    }
}

fun closeProject(project: Project) {
    commitAllDocuments()
    val projectManagerEx = ProjectManagerEx.getInstanceEx()
    projectManagerEx.forceCloseProject(project, true)
}
