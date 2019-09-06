/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.EdtTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.ThrowableRunnable
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.highlighter.KotlinPsiCheckerAndHighlightingUpdater
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
    // routing is obsolete in 192
}

fun replaceWithCustomHighlighter(parentDisposable: Disposable, fromImplementationClass: String, toImplementationClass: String) {
    val pointName = ExtensionPointName.create<LanguageExtensionPoint<Annotator>>(LanguageAnnotators.EP_NAME)
    val extensionPoint = pointName.getPoint(null)

    val point = LanguageExtensionPoint<Annotator>()
    point.language = "kotlin"
    point.implementationClass = toImplementationClass

    val extensions = extensionPoint.extensions
    val filteredExtensions =
        extensions.filter { it.language != "kotlin" || it.implementationClass != fromImplementationClass }
            .toList()
    // custom highlighter is already registered if filteredExtensions has the same size as extensions
    if (filteredExtensions.size < extensions.size) {
        PlatformTestUtil.maskExtensions(pointName, filteredExtensions + listOf(point), parentDisposable)
    }
}
