/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.application.options.CodeStyle
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.findInvalidCommas
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.needComma
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.trailingCommaAllowedInModule
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.trailingCommaOrLastElement
import org.jetbrains.kotlin.idea.formatter.TrailingCommaVisitor
import org.jetbrains.kotlin.idea.formatter.isComma
import org.jetbrains.kotlin.idea.formatter.leafIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.idea.quickfix.ReformatQuickFix
import org.jetbrains.kotlin.idea.util.isLineBreak
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.prevLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import javax.swing.JComponent

class TrailingCommaInspection(
    @JvmField
    var addCommaWarning: Boolean = false,
) : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : TrailingCommaVisitor() {
        override val recursively: Boolean = false

        override fun process(commaOwner: KtElement) {
            val action = TrailingCommaAction.create(commaOwner)
            if (action != TrailingCommaAction.REMOVE) {
                checkCommaPosition(commaOwner)
                checkLineBreaks(commaOwner)
            }
            checkTrailingComma(commaOwner, action)
        }

        private fun checkLineBreaks(commaOwner: KtElement) {
            val first = TrailingCommaPostFormatProcessor.elementBeforeFirstElement(commaOwner)
            if (first?.nextLeaf(true)?.isLineBreak() == false) {
                first.nextSibling?.let {
                    registerProblemForLineBreak(
                        commaOwner,
                        it,
                        if (ApplicationManager.getApplication().isUnitTestMode)
                            ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                        else
                            ProblemHighlightType.INFORMATION,
                    )
                }

            }

            val last = TrailingCommaPostFormatProcessor.elementAfterLastElement(commaOwner)
            if (last?.prevLeaf(true)?.isLineBreak() == false) {
                registerProblemForLineBreak(
                    commaOwner,
                    last,
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                )
            }
        }

        private fun checkCommaPosition(commaOwner: KtElement) {
            for (invalidComma in findInvalidCommas(commaOwner)) {
                reportProblem(invalidComma, "Comma loses the advantages in this position", "Fix comma position")
            }
        }

        private fun checkTrailingComma(commaOwner: KtElement, action: TrailingCommaAction) {
            val trailingCommaOrLastElement = trailingCommaOrLastElement(commaOwner) ?: return
            if (action == TrailingCommaAction.ADD) {
                if (!trailingCommaAllowedInModule(commaOwner) || trailingCommaOrLastElement.isComma) return
                reportProblem(
                    trailingCommaOrLastElement,
                    "Missing trailing comma",
                    "Add trailing comma",
                    if (addCommaWarning || ApplicationManager.getApplication().isUnitTestMode)
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING
                    else
                        ProblemHighlightType.INFORMATION,
                )
            } else if (action == TrailingCommaAction.REMOVE) {
                if (!trailingCommaOrLastElement.isComma) return
                reportProblem(trailingCommaOrLastElement, "Useless trailing comma", "Remove trailing comma")
            }
        }

        private fun reportProblem(
            commaOrElement: PsiElement,
            message: String,
            fixMessage: String,
            highlightType: ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
        ) {
            val commaOwner = commaOrElement.parent
            // case for KtFunctionLiteral, where PsiWhiteSpace after KtTypeParameterList isn't included in this list
            val problemOwner = commaOwner.parent
            holder.registerProblem(
                problemOwner,
                message,
                highlightType,
                commaOrElement.textRangeOfCommaOrSymbolAfter.shiftLeft(problemOwner.startOffset),
                ReformatQuickFix(fixMessage, commaOwner),
            )
        }

        private fun registerProblemForLineBreak(
            commaOwner: KtElement,
            elementForTextRange: PsiElement,
            highlightType: ProblemHighlightType,
        ) {
            val problemElement = commaOwner.parent
            holder.registerProblem(
                problemElement,
                "Missing line break",
                highlightType,
                TextRange.from(elementForTextRange.startOffset, 1).shiftLeft(problemElement.startOffset),
                ReformatQuickFix("Add line break", commaOwner),
            )
        }

        private val PsiElement.textRangeOfCommaOrSymbolAfter: TextRange
            get() {
                val textRange = textRange
                if (isComma) return textRange

                val resultRange = nextLeaf()?.leafIgnoringWhitespaceAndComments(false)?.endOffset?.takeIf { it > 0 }?.let {
                    TextRange.create(it - 1, it).intersection(parent.textRange)
                } ?: TextRange.create(textRange.endOffset - 1, textRange.endOffset)
                return resultRange.shiftRight(1)
            }
    }

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox("Report also a missing comma", "addCommaWarning")
        return panel
    }
}

private enum class TrailingCommaAction {
    ADD, REFORMAT, REMOVE;

    companion object {
        fun create(commaOwner: KtElement): TrailingCommaAction {
            val settings = CodeStyle.getSettings(commaOwner.project)
            return when {
                needComma(commaOwner, settings, checkExistingTrailingComma = false) -> ADD
                needComma(commaOwner, settings, checkExistingTrailingComma = true) -> REFORMAT
                else -> REMOVE
            }
        }
    }
}