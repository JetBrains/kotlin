/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.testFramework

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl
import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.startup.impl.StartupManagerImpl
import com.intellij.lang.LanguageAnnotators
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.lang.annotation.Annotator
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.impl.NonBlockingReadActionImpl
import com.intellij.openapi.editor.Document
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ex.ProjectManagerEx
import com.intellij.openapi.startup.StartupManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.testFramework.ExtensionTestUtil
import com.intellij.testFramework.TestApplicationManager
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.perf.util.logMessage
import java.nio.file.Paths

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

fun dispatchAllInvocationEvents() {
    runInEdtAndWait {
        UIUtil.dispatchAllInvocationEvents()
        NonBlockingReadActionImpl.waitForAsyncTaskCompletion();
    }
}

fun loadProjectWithName(path: String, name: String): Project? =
    ProjectManagerEx.getInstanceEx().openProject(Paths.get(path), OpenProjectTask(projectName = name))

fun TestApplicationManager.closeProject(project: Project) {
    val name = project.name
    val startupManagerImpl = StartupManager.getInstance(project) as StartupManagerImpl
    val daemonCodeAnalyzerSettings = DaemonCodeAnalyzerSettings.getInstance()
    val daemonCodeAnalyzerImpl = DaemonCodeAnalyzer.getInstance(project) as DaemonCodeAnalyzerImpl

    setDataProvider(null)
    daemonCodeAnalyzerSettings.isImportHintEnabled = true // return default value to avoid unnecessary save
    startupManagerImpl.checkCleared()
    daemonCodeAnalyzerImpl.cleanupAfterTest()

    logMessage { "project '$name' is about to be closed" }
    dispatchAllInvocationEvents()
    val projectManagerEx = ProjectManagerEx.getInstanceEx()
    projectManagerEx.forceCloseProject(project)
    logMessage { "project '$name' successfully closed" }
}

fun runStartupActivities(project: Project) {
    // obsolete
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
