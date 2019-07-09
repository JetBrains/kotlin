/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.util

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.idea.KotlinFileType
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

open class DialogWithEditor(
        val project: Project,
        title: String,
        val initialText: String
) : DialogWrapper(project, true) {
    val editor: Editor = createEditor()

    init {
        init()
        setTitle(title)
    }

    override final fun init() {
        super.init()
    }

    private fun createEditor(): Editor {
        val editorFactory = EditorFactory.getInstance()!!
        val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, initialText)
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)!!

        val editor = editorFactory.createEditor(document, project, KotlinFileType.INSTANCE, false)
        val settings = editor.settings
        settings.isVirtualSpace = false
        settings.isLineMarkerAreaShown = false
        settings.isFoldingOutlineShown = false
        settings.isRightMarginShown = false
        settings.isAdditionalPageAtBottom = false
        settings.additionalLinesCount = 2
        settings.additionalColumnsCount = 12

        assert(editor is EditorEx)
        (editor as EditorEx).isEmbeddedIntoDialogWrapper = true

        editor.getColorsScheme().setColor(EditorColors.CARET_ROW_COLOR, editor.getColorsScheme().defaultBackground)

        return editor
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.add(editor.component, BorderLayout.CENTER)
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return editor.contentComponent
    }

    override fun dispose() {
        super.dispose()
        EditorFactory.getInstance()!!.releaseEditor(editor)
    }
}
