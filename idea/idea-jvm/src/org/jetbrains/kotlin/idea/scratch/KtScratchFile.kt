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

package org.jetbrains.kotlin.idea.scratch

import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.caches.resolve.analyzeWithContent
import org.jetbrains.kotlin.idea.codeInsight.CodeInsightUtils
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.idea.refactoring.getLineStartOffset
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.AnalyzingUtils

class KtScratchFile(project: Project, editor: TextEditor) : ScratchFile(project, editor) {
    override fun getExpressions(psiFile: PsiFile): List<ScratchExpression> {
        // todo multiple expressions at one line
        val doc = PsiDocumentManager.getInstance(psiFile.project).getDocument(psiFile) ?: return emptyList()
        var line = 0
        val result = arrayListOf<ScratchExpression>()
        while (line < doc.lineCount) {
            val start = psiFile.getLineStartOffset(line) ?: continue
            val elementAtOffset = CodeInsightUtils.getTopmostElementAtOffset(
                PsiUtil.getElementAtOffset(psiFile, start),
                start,
                KtImportDirective::class.java,
                KtDeclaration::class.java
            )

            if (elementAtOffset is PsiWhiteSpace) {
                line = doc.getLineNumber(elementAtOffset.endOffset) + 1
                continue
            }

            if (elementAtOffset == null) {
                line += 1
                continue
            }

            result.add(ScratchExpression(elementAtOffset, elementAtOffset.getLineNumber(true), elementAtOffset.getLineNumber(false)))
            line = elementAtOffset.getLineNumber(false) + 1
        }

        return result
    }

    override fun hasErrors(): Boolean {
        val psiFile = getPsiFile() as? KtFile ?: return false
        try {
            AnalyzingUtils.checkForSyntacticErrors(psiFile)
        } catch (e: IllegalArgumentException) {
            return true
        }
        return psiFile.analyzeWithContent().diagnostics.any { it.severity == Severity.ERROR }
    }
}