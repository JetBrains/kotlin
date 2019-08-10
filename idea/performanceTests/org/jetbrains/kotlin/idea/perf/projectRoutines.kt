/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.EdtTestUtil
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.parameterInfo.HintType
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

fun commitAllDocuments() {
    val fileDocumentManager = FileDocumentManager.getInstance()
    runInEdtAndWait {
        fileDocumentManager.saveAllDocuments()
    }

    ProjectManagerEx.getInstanceEx().openProjects.forEach { project ->
        val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase

        runInEdtAndWait {
            psiDocumentManagerBase.clearUncommittedDocuments()
            psiDocumentManagerBase.commitAllDocuments()
        }
    }
}

fun commitDocument(project: Project, document: Document) {
    val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase
    runInEdtAndWait {
        psiDocumentManagerBase.commitDocument(document)
    }
}

fun saveDocument(document: Document) {
    val fileDocumentManager = FileDocumentManager.getInstance()

    runInEdtAndWait {
        fileDocumentManager.saveDocument(document)
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
    waitForAllEditorsFinallyLoaded(project, 5, TimeUnit.MINUTES)
}

fun waitForAllEditorsFinallyLoaded(project: Project, timeout: Long, unit: TimeUnit) {
    ApplicationManager.getApplication().assertIsDispatchThread()
    val deadline = unit.toMillis(timeout) + System.currentTimeMillis()
    while (true) {
        if (System.currentTimeMillis() > deadline) throw TimeoutException()
        if (waitABitForEditorLoading(project)) break
        UIUtil.dispatchAllInvocationEvents()
    }
}

private fun waitABitForEditorLoading(project: Project): Boolean {
    for (editor in FileEditorManager.getInstance(project).allEditors) {
        if (editor is TextEditorImpl) {
            try {
                editor.waitForLoaded(100, TimeUnit.MILLISECONDS)
            } catch (ignored: TimeoutException) {
                return false
            }

        }
    }
    return true
}