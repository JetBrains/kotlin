/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.codeInsight.EditorInfo
import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorImpl
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.runInEdtAndWait
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Assert
import org.junit.Test

class GradleMultiplatformHighlightingTest : GradleImportingTestCase() {

    @TargetVersions("4.7+")
    @Test
    fun testFirst() {
        doTest()
    }

    @TargetVersions("4.7+")
    @Test
    fun testNoErrors() {
        doTest()
    }

    private fun doTest() {
        val files = importProjectFromTestData()
        val project = myTestFixture.project

        checkFiles(files, project, GradleDaemonAnalyzerTestCase(testLineMarkers = true, checkWarnings = true, checkInfos = false)) { file ->
            file.extension == "kt"
        }
    }

    override fun testDataDirName(): String {
        return "newMultiplatformHighlighting"
    }
}

class GradleDaemonAnalyzerTestCase(val testLineMarkers: Boolean, val checkWarnings: Boolean, val checkInfos: Boolean) :
    DaemonAnalyzerTestCase() {
    override fun doTestLineMarkers() = testLineMarkers

    fun checkHighlighting(project: Project, editor: Editor) {
        myProject = project
        runInEdtAndWait {
            checkHighlighting(editor, checkWarnings, checkInfos)
        }
    }
}

internal fun checkFiles(
    files: List<VirtualFile>,
    project: Project,
    analyzer: GradleDaemonAnalyzerTestCase,
    fileFilter: (VirtualFile) -> Boolean
) {
    var atLeastOneFile = false
    val kotlinFiles = files.filter(fileFilter)
    val content = mutableMapOf<VirtualFile, String>()
    kotlinFiles.forEach { file ->
        val (_, textWithTags) = configureEditorByExistingFile(file, project)
        atLeastOneFile = true
        content[file] = textWithTags
    }
    Assert.assertTrue(atLeastOneFile)
    kotlinFiles.forEach { file ->
        val (editor, _) = configureEditorByExistingFile(file, project, content[file])
        analyzer.checkHighlighting(project, editor)
    }
}

internal fun textWithoutTags(text: String): String {
    val regex = "</?(error|warning|lineMarker).*?>".toRegex()
    return regex.replace(text, "")
}

private fun createEditor(file: VirtualFile, project: Project): Editor {
    val instance = FileEditorManager.getInstance(project)
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    val editor = instance.openTextEditor(OpenFileDescriptor(project, file, 0), false)
    (editor as EditorImpl).setCaretActive()
    PsiDocumentManager.getInstance(project).commitAllDocuments()
    DaemonCodeAnalyzer.getInstance(project).restart()
    return editor
}

internal fun configureEditorByExistingFile(
    virtualFile: VirtualFile,
    project: Project,
    contentToSet: String? = null
): Pair<Editor, String> {
    var result: Pair<Editor, String>? = null
    runInEdtAndWait {
        val editor = createEditor(virtualFile, project)
        val document = editor.document
        val editorInfo = EditorInfo(document.text)
        val textWithTags = editorInfo.newFileText
        ApplicationManager.getApplication().runWriteAction {
            val newText = contentToSet ?: textWithoutTags(textWithTags)
            if (document.text != newText) {
                document.setText(newText)
            }

            editorInfo.applyToEditor(editor)
        }
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        result = editor to textWithTags
    }
    return result!!
}