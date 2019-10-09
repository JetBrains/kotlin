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
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.parameterInfo.HintType
import java.nio.file.Paths
import java.io.PrintWriter
import java.io.StringWriter

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

fun loadProjectWithName(path: String, name: String): Project? =
    ProjectManagerEx.getInstanceEx().loadProject(Paths.get(path), name)

fun closeProject(project: Project) {
    dispatchAllInvocationEvents()
    val projectManagerEx = ProjectManagerEx.getInstanceEx()
    projectManagerEx.forceCloseProject(project, true)
}

fun runStartupActivities(project: Project) {
    with(StartupManager.getInstance(project) as StartupManagerImpl) {
        //scheduleInitialVfsRefresh()
        runStartupActivities()
        runPostStartupActivities()
    }
}

fun waitForAllEditorsFinallyLoaded(project: Project) {
    // routing is obsolete in 192
}

fun replaceWithCustomHighlighter(parentDisposable: Disposable, fromImplementationClass: String, toImplementationClass: String) {
    val pointName = ExtensionPointName.create<LanguageExtensionPoint<Annotator>>(LanguageAnnotators.EP_NAME.name)
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
        ExtensionTestUtil.maskExtensions(pointName, filteredExtensions + listOf(point), parentDisposable)
    }
}

fun logMessage(message: () -> String) {
    println("-- ${message()}")
}

fun logMessage(t: Throwable, message: () -> String) {
    val writer = StringWriter()
    PrintWriter(writer).use {
        t.printStackTrace(it)
    }
    println("-- ${message()}:\n$writer")
}