/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scratch

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.ide.scratch.ScratchRootType
import com.intellij.openapi.application.Application
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.ExpectedHighlightingData
import com.intellij.testFramework.FileEditorManagerTestCase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeInsight.AbstractLineMarkersTest
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager
import org.jetbrains.kotlin.idea.scratch.AbstractScratchRunActionTest.Companion.configureOptions
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

abstract class AbstractScratchLineMarkersTest : FileEditorManagerTestCase() {
    fun doScratchTest(path: String) {
        val fileText = FileUtil.loadFile(File(path))

        val scratchVirtualFile = ScratchRootType.getInstance().createScratchFile(
            project,
            "scratch.kts",
            KotlinLanguage.INSTANCE,
            fileText,
            ScratchFileService.Option.create_if_missing
        ) ?: error("Couldn't create scratch file")

        myFixture.openFileInEditor(scratchVirtualFile)

        ScriptConfigurationManager.updateScriptDependenciesSynchronously(myFixture.file)

        val scratchFileEditor = getScratchEditorForSelectedFile(FileEditorManager.getInstance(project), myFixture.file.virtualFile)
            ?: error("Couldn't find scratch panel")

        configureOptions(scratchFileEditor, fileText, null)

        val project = myFixture.project
        val document = myFixture.editor.document

        val data = ExpectedHighlightingData(document, false, false, false)
        data.init()

        PsiDocumentManager.getInstance(project).commitAllDocuments()

        val markers = doAndCheckHighlighting(document, data, File(path))

        AbstractLineMarkersTest.assertNavigationElements(myFixture.project, myFixture.file as KtFile, markers)
        runWriteAction {
            scratchVirtualFile.delete(this)
        }
    }

    private fun doAndCheckHighlighting(
        documentToAnalyze: Document, expectedHighlighting: ExpectedHighlightingData, expectedFile: File
    ): List<LineMarkerInfo<*>> {
        myFixture.doHighlighting()

        return AbstractLineMarkersTest.checkHighlighting(myFixture.file, documentToAnalyze, expectedHighlighting, expectedFile)
    }

}
