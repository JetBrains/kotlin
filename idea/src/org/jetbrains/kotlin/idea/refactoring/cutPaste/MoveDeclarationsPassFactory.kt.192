/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.cutPaste

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.kotlin.idea.core.util.range

class MoveDeclarationsPassFactory(highlightingPassRegistrar: TextEditorHighlightingPassRegistrar) : ProjectComponent,
    TextEditorHighlightingPassFactory {

    init {
        highlightingPassRegistrar.registerTextEditorHighlightingPass(
            this,
            TextEditorHighlightingPassRegistrar.Anchor.BEFORE,
            Pass.POPUP_HINTS,
            true,
            true
        )
    }

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        return MyPass(file.project, file, editor)
    }

    private class MyPass(
        private val project: Project,
        private val file: PsiFile,
        private val editor: Editor
    ) : TextEditorHighlightingPass(project, editor.document, true) {

        override fun doCollectInformation(progress: ProgressIndicator) {}

        override fun doApplyInformationToEditor() {
            val info = buildHighlightingInfo()
            UpdateHighlightersUtil.setHighlightersToEditor(project, myDocument!!, 0, file.textLength, listOfNotNull(info), colorsScheme, id)
        }

        private fun buildHighlightingInfo(): HighlightInfo? {
            val cookie = editor.getUserData(MoveDeclarationsEditorCookie.KEY) ?: return null

            if (cookie.modificationCount != PsiModificationTracker.SERVICE.getInstance(project).modificationCount) return null

            val processor = MoveDeclarationsProcessor.build(editor, cookie)

            if (processor == null) {
                editor.putUserData(MoveDeclarationsEditorCookie.KEY, null)
                return null
            }

            val info = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION)
                .range(cookie.bounds.range!!)
                .createUnconditionally()
            QuickFixAction.registerQuickFixAction(info, MoveDeclarationsIntentionAction(processor, cookie.bounds, cookie.modificationCount))

            return info
        }
    }
}
