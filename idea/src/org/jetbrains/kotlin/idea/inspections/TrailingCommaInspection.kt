/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.findInvalidCommas
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.needComma
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.trailingCommaAllowedInModule
import org.jetbrains.kotlin.idea.formatter.TrailingCommaPostFormatProcessor.Companion.trailingCommaOrLastElement
import org.jetbrains.kotlin.idea.formatter.TrailingCommaVisitor
import org.jetbrains.kotlin.idea.formatter.isComma
import org.jetbrains.kotlin.idea.formatter.leafIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.idea.quickfix.ReformatQuickFix
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.nextLeaf
import org.jetbrains.kotlin.psi.psiUtil.startOffset

class TrailingCommaInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : TrailingCommaVisitor() {
        override val recursively: Boolean = false

        override fun process(commaOwner: KtElement) {
            checkCommaPosition(commaOwner)
            checkTrailingComma(commaOwner)
        }

        private fun checkCommaPosition(commaOwner: KtElement) {
            if (!needComma(commaOwner, checkExistingTrailingComma = true)) return
            for (invalidComma in findInvalidCommas(commaOwner)) {
                reportProblem(invalidComma, "Comma loses the advantages in this position", "Fix comma position")
            }
        }

        private fun checkTrailingComma(commaOwner: KtElement) {
            val trailingCommaOrLastElement = trailingCommaOrLastElement(commaOwner) ?: return
            if (needComma(commaOwner, checkExistingTrailingComma = false)) {
                if (!trailingCommaAllowedInModule(commaOwner) || trailingCommaOrLastElement.isComma) return
                reportProblem(trailingCommaOrLastElement, "Missing trailing comma", "Add trailing comma")
            } else if (!needComma(commaOwner, checkExistingTrailingComma = true)) {
                if (!trailingCommaOrLastElement.isComma) return
                reportProblem(trailingCommaOrLastElement, "Redundant trailing comma", "Remove trailing comma")
            }
        }

        private fun reportProblem(commaOrElement: PsiElement, message: String, fixMessage: String) {
            val commaOwner = commaOrElement.parent
            holder.registerProblem(
                commaOwner,
                commaOrElement.textRangeOfLastSymbol.shiftLeft(commaOwner.startOffset),
                message,
                ReformatQuickFix(fixMessage)
            )
        }

        private val PsiElement.textRangeOfLastSymbol: TextRange
            get() {
                val textRange = textRange
                if (textRange.length <= 1) return textRange

                return nextLeaf()?.leafIgnoringWhitespaceAndComments(false)?.endOffset?.takeIf { it > 0 }?.let {
                    TextRange.create(it - 1, it).intersection(textRange)
                } ?: TextRange.create(textRange.endOffset - 1, textRange.endOffset)
            }
    }

}
