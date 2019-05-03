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

package org.jetbrains.kotlin.idea.refactoring.introduce.extractionEngine

import org.jetbrains.kotlin.idea.util.psi.patternMatching.KotlinPsiRange
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.project.Project
import com.intellij.openapi.editor.Editor
import java.util.ArrayList
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.editor.ScrollType
import org.jetbrains.kotlin.idea.refactoring.KotlinRefactoringBundle
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.codeInsight.folding.CodeFoldingManager
import com.intellij.ui.ReplacePromptDialog
import com.intellij.find.FindManager
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import com.intellij.refactoring.util.duplicates.MethodDuplicatesHandler
import com.intellij.refactoring.RefactoringBundle
import org.jetbrains.kotlin.idea.refactoring.introduce.getPhysicalTextRange

fun KotlinPsiRange.highlight(project: Project, editor: Editor): RangeHighlighter? {
    val textRange = getPhysicalTextRange()
    val highlighters = ArrayList<RangeHighlighter>()
    val attributes = EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)!!
    HighlightManager.getInstance(project).addRangeHighlight(
        editor, textRange.startOffset, textRange.endOffset, attributes, true, highlighters
    )
    return highlighters.firstOrNull()
}

fun KotlinPsiRange.preview(project: Project, editor: Editor): RangeHighlighter? {
    val highlight = highlight(project, editor) ?: return null

    val startOffset = getPhysicalTextRange().startOffset
    val foldedRegions = CodeFoldingManager.getInstance(project)
        .getFoldRegionsAtOffset(editor, startOffset)
        .filter { !it.isExpanded }

    if (!foldedRegions.isEmpty()) {
        editor.foldingModel.runBatchFoldingOperation {
            foldedRegions.forEach { it.isExpanded = true }
        }
    }

    editor.scrollingModel.scrollTo(editor.offsetToLogicalPosition(startOffset), ScrollType.MAKE_VISIBLE)
    return highlight
}

fun processDuplicates(
    duplicateReplacers: Map<KotlinPsiRange, () -> Unit>,
    project: Project,
    editor: Editor,
    scopeDescription: String = "this file",
    usageDescription: String = "a usage of extracted declaration"
) {
    val size = duplicateReplacers.size
    if (size == 0) return

    if (size == 1) {
        duplicateReplacers.keys.first().preview(project, editor)
    }

    val answer = if (ApplicationManager.getApplication()!!.isUnitTestMode) {
        Messages.YES
    } else {
        Messages.showYesNoDialog(
            project,
            KotlinRefactoringBundle.message(
                "0.has.detected.1.code.fragments.in.2.that.can.be.replaced.with.3",
                ApplicationNamesInfo.getInstance().productName,
                duplicateReplacers.size,
                scopeDescription,
                usageDescription
            ),
            "Process Duplicates",
            Messages.getQuestionIcon()
        )
    }

    if (answer != Messages.YES) {
        return
    }

    var showAll = false

    duplicateReplacersLoop@
    for ((i, entry) in duplicateReplacers.entries.withIndex()) {
        val (pattern, replacer) = entry
        if (!pattern.isValid()) continue

        val highlighter = pattern.preview(project, editor)
        if (!ApplicationManager.getApplication()!!.isUnitTestMode) {
            if (size > 1 && !showAll) {
                val promptDialog = ReplacePromptDialog(false, RefactoringBundle.message("process.duplicates.title", i + 1, size), project)
                promptDialog.show()
                when (promptDialog.exitCode) {
                    FindManager.PromptResult.ALL -> showAll = true
                    FindManager.PromptResult.SKIP -> continue@duplicateReplacersLoop
                    FindManager.PromptResult.CANCEL -> return
                }
            }
        }

        highlighter?.let { HighlightManager.getInstance(project).removeSegmentHighlighter(editor, it) }
        project.executeWriteCommand(MethodDuplicatesHandler.REFACTORING_NAME, replacer)
    }
}

fun processDuplicatesSilently(duplicateReplacers: Map<KotlinPsiRange, () -> Unit>, project: Project) {
    project.executeWriteCommand(MethodDuplicatesHandler.REFACTORING_NAME) {
        duplicateReplacers.values.forEach { it() }
    }
}