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

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.codeInspection.ex.ProblemDescriptorImpl
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.formatter.FormattingChange
import org.jetbrains.kotlin.idea.formatter.FormattingChange.ReplaceWhiteSpace
import org.jetbrains.kotlin.idea.formatter.FormattingChange.ShiftIndentInsideRange
import org.jetbrains.kotlin.idea.formatter.collectFormattingChanges
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.psi.KtFile

class ReformatInspection : LocalInspectionTool() {
    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<out ProblemDescriptor>? {
        if (file !is KtFile || !ProjectRootsUtil.isInProjectSource(file)) {
            return null
        }

        val changes = collectFormattingChanges(file)
        if (changes.isEmpty()) return null

        val elements = changes.map {
            val rangeOffset = when (it) {
                is ShiftIndentInsideRange -> it.range.startOffset
                is ReplaceWhiteSpace -> it.textRange.startOffset
            }

            val leaf = file.findElementAt(rangeOffset) ?: return@map null
            if (!leaf.isValid) return@map null
            if (leaf is PsiWhiteSpace && isEmptyLineReformat(leaf, it)) return@map null

            leaf
        }.filterNotNull()

        return elements.map {
            ProblemDescriptorImpl(it, it,
                                  "File is not properly formatted",
                                  arrayOf(ReformatQuickFix),
                                  ProblemHighlightType.WEAK_WARNING, false, null,
                                  isOnTheFly)
        }.toTypedArray()
    }

    private fun isEmptyLineReformat(whitespace: PsiWhiteSpace, change: FormattingChange): Boolean {
        if (change !is FormattingChange.ReplaceWhiteSpace) return false

        val beforeText = whitespace.text
        val afterText = change.whiteSpace

        return beforeText.count { it == '\n' } == afterText.count { it == '\n' } &&
               beforeText.substringAfterLast('\n') == afterText.substringAfterLast('\n')
    }

    private object ReformatQuickFix : LocalQuickFix {
        override fun getFamilyName(): String = "Reformat File"
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            CodeStyleManager.getInstance(project).reformat(descriptor.psiElement.containingFile)
        }
    }
}