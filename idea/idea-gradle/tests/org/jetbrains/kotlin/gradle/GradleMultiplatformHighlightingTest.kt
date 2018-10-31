/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.codeInsight.gradle.GradleImportingTestCase
import org.jetbrains.kotlin.test.testFramework.runInEdtAndWait
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions
import org.junit.Assert
import org.junit.Test

class GradleMultiplatformHighlightingTest : GradleImportingTestCase() {

    @TargetVersions("4.7+")
    @Test
    fun testFirst() {
        val files = importProjectFromTestData()
        val project = myTestFixture.project

        checkFiles(files, project) { editor ->
            daemonAnalyzerTestCase.checkHighlighting(editor, project)
        }

    }

    private val daemonAnalyzerTestCase = object : DaemonAnalyzerTestCase() {
        fun checkHighlighting(editor: Editor, project: Project) {
            myProject = project
            runInEdtAndWait {
                checkHighlighting(editor, /* checkWarnings = */ true, /* checkInfos = */ false)
            }
        }
    }

    private fun checkFiles(files: List<VirtualFile>, project: Project, check: (Editor) -> Unit) {
        var atLeastOneFile = false
        files.filter { it.extension == "kt" }.forEach { file ->
            val editor = configureEditorByExistingFile(file, project)
            atLeastOneFile = true
            check(editor)
        }
        Assert.assertTrue(atLeastOneFile)
    }

    private fun configureEditorByExistingFile(virtualFile: VirtualFile, project: Project): Editor {
        var result: Editor? = null
        runInEdtAndWait {
            val editor = createEditor(virtualFile, project)
            val document = editor.document
            val editorInfo = EditorInfo(document.text)
            val newFileText = editorInfo.newFileText
            ApplicationManager.getApplication().runWriteAction {
                if (document.text != newFileText) {
                    document.setText(newFileText)
                }

                editorInfo.applyToEditor(editor)
            }
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            result = editor
        }
        return result!!
    }

    private fun createEditor(file: VirtualFile, project: Project): Editor {
        val instance = FileEditorManager.getInstance(this.myProject)
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        val editor = instance.openTextEditor(OpenFileDescriptor(this.myProject, file, 0), false)
        (editor as EditorImpl).setCaretActive()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        DaemonCodeAnalyzer.getInstance(project).restart()
        return editor
    }

    override fun testDataDirName(): String {
        return "newMultiplatformHighlighting"
    }
}