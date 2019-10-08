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
import org.jetbrains.kotlin.idea.script.ScriptDiagnosticFixProvider
import org.jetbrains.kotlin.psi.KtFile
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.SourceCode

class ScriptExternalHighlightingPass(
    private val file: KtFile,
    document: Document
) : TextEditorHighlightingPass(file.project, document), DumbAware {
    override fun doCollectInformation(progress: ProgressIndicator) = Unit

    override fun doApplyInformationToEditor() {
        val document = document ?: return

        if (!file.isScript()) return

        val reports = IdeScriptReportSink.getReports(file)

        val annotations = reports.mapNotNull { scriptDiagnostic ->
            val (startOffset, endOffset) = scriptDiagnostic.location?.let { computeOffsets(document, it) } ?: 0 to 0
            val exception = scriptDiagnostic.exception
            val exceptionMessage = if (exception != null) " ($exception)" else ""
            val message = scriptDiagnostic.message + exceptionMessage
            val annotation = Annotation(
                startOffset,
                endOffset,
                scriptDiagnostic.severity.convertSeverity() ?: return@mapNotNull null,
                message,
                message
            )

            // if range is empty, show notification panel in editor
            annotation.isFileLevelAnnotation = startOffset == endOffset

            for (provider in ScriptDiagnosticFixProvider.EP_NAME.extensions) {
                provider.provideFixes(scriptDiagnostic).forEach {
                    annotation.registerFix(it)
                }
            }

            annotation
        }

        val infos = annotations.map { HighlightInfo.fromAnnotation(it) }
        UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument!!, 0, file.textLength, infos, colorsScheme, id)
    }

    private fun computeOffsets(document: Document, position: SourceCode.Location): Pair<Int, Int> {
        val startLine = position.start.line.coerceLineIn(document)
        val startOffset = document.offsetBy(startLine, position.start.col)

        val endLine = position.end?.line?.coerceAtLeast(startLine)?.coerceLineIn(document) ?: startLine
        val endOffset = document.offsetBy(
            endLine,
            position.end?.col ?: document.getLineEndOffset(endLine)
        ).coerceAtLeast(startOffset)

        return startOffset to endOffset
    }

    private fun Int.coerceLineIn(document: Document) = coerceIn(0, document.lineCount - 1)

    private fun Document.offsetBy(line: Int, col: Int): Int {
        return (getLineStartOffset(line) + col).coerceIn(getLineStartOffset(line), getLineEndOffset(line))
    }

    private fun ScriptDiagnostic.Severity.convertSeverity(): HighlightSeverity? {
        return when (this) {
            ScriptDiagnostic.Severity.FATAL -> ERROR
            ScriptDiagnostic.Severity.ERROR -> ERROR
            ScriptDiagnostic.Severity.WARNING -> WARNING
            ScriptDiagnostic.Severity.INFO -> INFORMATION
            ScriptDiagnostic.Severity.DEBUG -> if (ApplicationManager.getApplication().isInternal) INFORMATION else null
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
