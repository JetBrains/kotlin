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
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.parameterInfo.HintType
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl.waitForAllEditorsFinallyLoaded
import java.util.concurrent.TimeUnit

fun commitAllDocuments() {
    ProjectManagerEx.getInstanceEx().openProjects.forEach { project ->
        val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase

        EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
            psiDocumentManagerBase.clearUncommittedDocuments()
            psiDocumentManagerBase.commitAllDocuments()
        })
    }
}

fun enableHints(enable: Boolean) =
    HintType.values().forEach { it.option.set(enable) }

fun runInEdtAndWait(block: () -> Unit) {
    EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
        block()
    })
}

fun dispatchAllInvocationEvents() {
    runInEdtAndWait {
        UIUtil.dispatchAllInvocationEvents()
    }
}

fun closeProject(project: Project) {
    dispatchAllInvocationEvents()
    val projectManagerEx = ProjectManagerEx.getInstanceEx()
    projectManagerEx.forceCloseProject(project, true)
}

fun waitForAllEditorsFinallyLoaded(project: Project) {
    waitForAllEditorsFinallyLoaded(project, 30, TimeUnit.SECONDS)
}