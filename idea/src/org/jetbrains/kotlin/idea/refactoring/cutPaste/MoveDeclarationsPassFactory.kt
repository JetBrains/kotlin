/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.idea.conversion.copy.range

class MoveDeclarationsPassFactory(highlightingPassRegistrar: TextEditorHighlightingPassRegistrar)
    : ProjectComponent, TextEditorHighlightingPassFactory {
    
    init {
        highlightingPassRegistrar.registerTextEditorHighlightingPass(this, TextEditorHighlightingPassRegistrar.Anchor.BEFORE, Pass.POPUP_HINTS, true, true)
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
