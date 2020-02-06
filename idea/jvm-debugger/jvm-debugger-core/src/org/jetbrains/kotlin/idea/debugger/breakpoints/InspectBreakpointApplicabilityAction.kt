/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.TextAnnotationGutterProvider
import com.intellij.openapi.editor.colors.ColorKey
import com.intellij.openapi.editor.colors.EditorFontType
import org.jetbrains.kotlin.idea.core.util.getLineCount
import org.jetbrains.kotlin.idea.debugger.breakpoints.BreakpointChecker.BreakpointType
import org.jetbrains.kotlin.psi.KtFile
import java.awt.Color
import java.util.*

@Suppress("ComponentNotRegistered")
class InspectBreakpointApplicabilityAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val data = e.getData() ?: return

        if (data.editor.gutter.isAnnotationsShown) {
            data.editor.gutter.closeAllAnnotations()
        }

        val checker = BreakpointChecker()

        val lineCount = data.file.getLineCount()
        val breakpoints = (0..lineCount).map { line -> checker.check(data.file, line) }
        val gutterProvider = BreakpointsGutterProvider(breakpoints)
        data.editor.gutter.registerTextAnnotation(gutterProvider)
    }

    private class BreakpointsGutterProvider(private val breakpoints: List<EnumSet<BreakpointType>>) : TextAnnotationGutterProvider {
        override fun getLineText(line: Int, editor: Editor?): String? {
            val breakpoints = breakpoints.getOrNull(line) ?: return null
            return breakpoints.map { it.prefix }.distinct().joinToString()
        }

        override fun getToolTip(line: Int, editor: Editor?): String? = null
        override fun getStyle(line: Int, editor: Editor?) = EditorFontType.PLAIN

        override fun getPopupActions(line: Int, editor: Editor?) = emptyList<AnAction>()
        override fun getColor(line: Int, editor: Editor?): ColorKey? = null
        override fun getBgColor(line: Int, editor: Editor?): Color? = null
        override fun gutterClosed() {}
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isVisible = ApplicationManager.getApplication().isInternal
        e.presentation.isEnabled = e.getData() != null
    }

    class ActionData(val editor: Editor, val file: KtFile)

    private fun AnActionEvent.getData(): ActionData? {
        val editor = getData(CommonDataKeys.EDITOR) ?: return null
        val file = getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return null
        return ActionData(editor, file)
    }
}