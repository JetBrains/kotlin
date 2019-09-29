/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ThrowableRunnable
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.parameterInfo.HintType

fun commitAllDocuments() {
    ProjectManagerEx.getInstanceEx().openProjects.forEach { project ->
        val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase

        EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
            psiDocumentManagerBase.clearUncommittedDocuments()
            psiDocumentManagerBase.commitAllDocuments()
        })
    }
}

fun commitDocument(project: Project, document: Document) {
    val psiDocumentManagerBase = PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase

    EdtTestUtil.runInEdtAndWait(ThrowableRunnable {
        psiDocumentManagerBase.commitDocument(document)
    })
}

fun saveDocument(document: Document) {
    val fileDocumentManager = FileDocumentManager.getInstance()

    runInEdtAndWait {
        fileDocumentManager.saveDocument(document)
    }
}

fun enableHints(enable: Boolean) =
    HintType.values().forEach { it.option.set(enable) }

fun dispatchAllInvocationEvents() {
    runInEdtAndWait {
        UIUtil.dispatchAllInvocationEvents()
    }
}

fun loadProjectWithName(path: String, name: String): Project? =
    ProjectManagerEx.getInstanceEx().loadProject(name, path)

fun closeProject(project: Project) {
    dispatchAllInvocationEvents()
    val projectManagerEx = ProjectManagerEx.getInstanceEx()
    projectManagerEx.closeAndDispose(project)
}

fun runStartupActivities(project: Project) {
    with(StartupManager.getInstance(project) as StartupManagerImpl) {
        scheduleInitialVfsRefresh()
        runStartupActivities()
        runPostStartupActivities()
    }
}

fun waitForAllEditorsFinallyLoaded(project: Project) {
 // 183 does not have this public api
}

// BUNCH: 183
fun replaceWithCustomHighlighter(parentDisposable: Disposable, fromImplementationClass: String, toImplementationClass: String) {
 // 183 does not have this public api
}

fun logMessage(message: () -> String) {
    println("-- ${message()}")
}