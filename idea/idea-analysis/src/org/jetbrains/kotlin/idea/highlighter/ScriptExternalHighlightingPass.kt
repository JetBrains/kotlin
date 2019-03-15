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

package org.jetbrains.kotlin.idea.highlighter

import com.intellij.codeHighlighting.Pass
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.lang.annotation.Annotation
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.lang.annotation.HighlightSeverity.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.ex.StatusBarEx
import com.intellij.psi.PsiFile
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.idea.core.script.IdeScriptReportSink
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesUpdater
import org.jetbrains.kotlin.psi.KtFile
import kotlin.script.experimental.dependencies.ScriptReport

class ScriptExternalHighlightingPass(
    private val file: KtFile,
    document: Document
) : TextEditorHighlightingPass(file.project, document), DumbAware {
    override fun doCollectInformation(progress: ProgressIndicator) = Unit

    override fun doApplyInformationToEditor() {
        val document = document ?: return

        if (!file.isScript()) return

        if (!ScriptDefinitionsManager.getInstance(file.project).isReady()) {
            showNotification(
                file,
                "Highlighting in scripts is not available until all Script Definitions are loaded"
            )
        }

        if (!ScriptDependenciesUpdater.areDependenciesCached(file)) {
            showNotification(
                file,
                "Highlighting in scripts is not available until all Script Dependencies are loaded"
            )
        }

        val reports = file.virtualFile.getUserData(IdeScriptReportSink.Reports) ?: return

        val annotations = reports.mapNotNull { (message, severity, position) ->
            val (startOffset, endOffset) = position?.let { computeOffsets(document, position) } ?: 0 to 0
            val annotation = Annotation(
                startOffset,
                endOffset,
                severity.convertSeverity() ?: return@mapNotNull null,
                message,
                message
            )

            // if range is empty, show notification panel in editor
            annotation.isFileLevelAnnotation = startOffset == endOffset

            annotation
        }

        val infos = annotations.map { HighlightInfo.fromAnnotation(it) }
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument!!, 0, file.textLength, infos, colorsScheme, id)
    }

    private fun computeOffsets(document: Document, position: ScriptReport.Position): Pair<Int, Int> {
        val startLine = position.startLine.coerceLineIn(document)
        val startOffset = document.offsetBy(startLine, position.startColumn)

        val endLine = position.endLine?.coerceAtLeast(startLine)?.coerceLineIn(document) ?: startLine
        val endOffset = document.offsetBy(
            endLine,
            position.endColumn ?: document.getLineEndOffset(endLine)
        ).coerceAtLeast(startOffset)

        return startOffset to endOffset
    }

    private fun Int.coerceLineIn(document: Document) = coerceIn(0, document.lineCount - 1)

    private fun Document.offsetBy(line: Int, col: Int): Int {
        return (getLineStartOffset(line) + col).coerceIn(getLineStartOffset(line), getLineEndOffset(line))
    }

    private fun ScriptReport.Severity.convertSeverity(): HighlightSeverity? {
        return when (this) {
            ScriptReport.Severity.FATAL -> ERROR
            ScriptReport.Severity.ERROR -> ERROR
            ScriptReport.Severity.WARNING -> WARNING
            ScriptReport.Severity.INFO -> INFORMATION
            ScriptReport.Severity.DEBUG -> if (ApplicationManager.getApplication().isInternal) INFORMATION else null
        }
    }

    private fun showNotification(file: KtFile, message: String) {
        UIUtil.invokeLaterIfNeeded {
            val ideFrame = WindowManager.getInstance().getIdeFrame(file.project)
            if (ideFrame != null) {
                val statusBar = ideFrame.statusBar as StatusBarEx
                statusBar.notifyProgressByBalloon(
                    MessageType.WARNING,
                    message,
                    null,
                    null
                )
            }
        }
    }

    class Factory(registrar: TextEditorHighlightingPassRegistrar) : ProjectComponent,
        TextEditorHighlightingPassFactory {
        init {
            registrar.registerTextEditorHighlightingPass(
                this,
                TextEditorHighlightingPassRegistrar.Anchor.BEFORE,
                Pass.UPDATE_FOLDING,
                false,
                false
            )
        }

        override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
            if (file !is KtFile) return null
            return ScriptExternalHighlightingPass(file, editor.document)
        }
    }
}
