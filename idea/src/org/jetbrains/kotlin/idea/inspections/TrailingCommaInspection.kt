/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.application.options.CodeStyle
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.formatter.TrailingCommaVisitor
import org.jetbrains.kotlin.idea.formatter.kotlinCustomSettings
import org.jetbrains.kotlin.idea.formatter.trailingComma.*
import org.jetbrains.kotlin.idea.formatter.trailingCommaAllowedInModule
import org.jetbrains.kotlin.idea.util.isComma
import org.jetbrains.kotlin.idea.util.isLineBreak
import org.jetbrains.kotlin.idea.util.leafIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.psiUtil.*
import javax.swing.JComponent
import kotlin.properties.Delegates

class TrailingCommaInspection(
    @JvmField
    var addCommaWarning: Boolean = false
) : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor = object : TrailingCommaVisitor() {
        override val recursively: Boolean = false
        private var useTrailingComma by Delegates.notNull<Boolean>()

        override fun process(trailingCommaContext: TrailingCommaContext) {
            val element = trailingCommaContext.ktElement
            val kotlinCustomSettings = CodeStyle.getSettings(element.project).kotlinCustomSettings
            useTrailingComma = kotlinCustomSettings.addTrailingCommaIsAllowedFor(element)
            when (trailingCommaContext.state) {
                TrailingCommaState.MISSING, TrailingCommaState.EXISTS -> {
                    checkCommaPosition(element)
                    checkLineBreaks(element)
                }
                else -> Unit
            }

            checkTrailingComma(trailingCommaContext)
        }

        private fun checkLineBreaks(commaOwner: KtElement) {
            val first = TrailingCommaHelper.elementBeforeFirstElement(commaOwner)
            if (first?.nextLeaf(true)?.isLineBreak() == false) {
                first.nextSibling?.let {
                    registerProblemForLineBreak(commaOwner, it, ProblemHighlightType.INFORMATION)
                }
            }

            val last = TrailingCommaHelper.elementAfterLastElement(commaOwner)
            val whenEntryWithoutTrailingComma =
                commaOwner is KtWhenEntry && TrailingCommaHelper.trailingCommaOrLastElement(commaOwner)?.isComma != true
            if (last?.prevLeaf(true)?.isLineBreak() == false && !whenEntryWithoutTrailingComma) {
                registerProblemForLineBreak(
                    commaOwner,
                    last,
                    if (addCommaWarning) ProblemHighlightType.GENERIC_ERROR_OR_WARNING else ProblemHighlightType.INFORMATION,
                )
            }
        }

        private fun checkCommaPosition(commaOwner: KtElement) {
            for (invalidComma in TrailingCommaHelper.findInvalidCommas(commaOwner)) {
                reportProblem(
                    invalidComma,
                    KotlinBundle.message("inspection.trailing.comma.comma.loses.the.advantages.in.this.position"),
                    KotlinBundle.message("inspection.trailing.comma.fix.comma.position")
                )
            }
        }

        private fun checkTrailingComma(trailingCommaContext: TrailingCommaContext) {
            val commaOwner = trailingCommaContext.ktElement
            val trailingCommaOrLastElement = TrailingCommaHelper.trailingCommaOrLastElement(commaOwner) ?: return
            when (trailingCommaContext.state) {
                TrailingCommaState.MISSING -> {
                    if (!trailingCommaAllowedInModule(commaOwner)) return
                    reportProblem(
                        trailingCommaOrLastElement,
                        KotlinBundle.message("inspection.trailing.comma.missing.trailing.comma"),
                        KotlinBundle.message("inspection.trailing.comma.add.trailing.comma"),
                        if (addCommaWarning) ProblemHighlightType.GENERIC_ERROR_OR_WARNING else ProblemHighlightType.INFORMATION,
                    )
                }
                TrailingCommaState.REDUNDANT -> {
                    reportProblem(
                        trailingCommaOrLastElement,
                        KotlinBundle.message("inspection.trailing.comma.useless.trailing.comma"),
                        KotlinBundle.message("inspection.trailing.comma.remove.trailing.comma"),
                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                        checkTrailingCommaSettings = false,
                    )
                }
                else -> Unit
            }
        }

        private fun reportProblem(
            commaOrElement: PsiElement,
            message: String,
            fixMessage: String,
            highlightType: ProblemHighlightType = ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            checkTrailingCommaSettings: Boolean = true,
        ) {
            val commaOwner = commaOrElement.parent as KtElement
            // case for KtFunctionLiteral, where PsiWhiteSpace after KtTypeParameterList isn't included in this list
            val problemOwner = commaOwner.parent
            holder.registerProblem(
                problemOwner,
                message,
                highlightType.applyCondition(!checkTrailingCommaSettings || useTrailingComma),
                commaOrElement.textRangeOfCommaOrSymbolAfter.shiftLeft(problemOwner.startOffset),
                createQuickFix(fixMessage, commaOwner),
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
                KotlinBundle.message("inspection.trailing.comma.missing.line.break"),
                highlightType.applyCondition(useTrailingComma),
                TextRange.from(elementForTextRange.startOffset, 1).shiftLeft(problemElement.startOffset),
                createQuickFix(KotlinBundle.message("inspection.trailing.comma.add.line.break"), commaOwner),
            )
        }

        private fun ProblemHighlightType.applyCondition(condition: Boolean): ProblemHighlightType = when {
            ApplicationManager.getApplication().isUnitTestMode -> ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            condition -> this
            else -> ProblemHighlightType.INFORMATION
        }

        private fun createQuickFix(
            fixMessage: String,
            commaOwner: KtElement,
        ): LocalQuickFix = object : LocalQuickFix {
            val commaOwnerPointer = commaOwner.createSmartPointer()

            override fun getFamilyName(): String = fixMessage

            override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
                val element = commaOwnerPointer.element ?: return
                val range = createFormatterTextRange(element)
                val settings = CodeStyle.getSettings(project).clone()
                settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA = true
                settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA_ON_CALL_SITE = true
                CodeStyle.doWithTemporarySettings(project, settings) {
                    CodeStyleManager.getInstance(project).reformatRange(element, range.startOffset, range.endOffset)
                }
            }
        }

        private fun createFormatterTextRange(commaOwner: KtElement): TextRange {
            val startElement = TrailingCommaHelper.elementBeforeFirstElement(commaOwner) ?: commaOwner
            val endElement = TrailingCommaHelper.elementAfterLastElement(commaOwner) ?: commaOwner
            return TextRange.create(startElement.startOffset, endElement.endOffset)
        }

        private val PsiElement.textRangeOfCommaOrSymbolAfter: TextRange
            get() {
                val textRange = textRange
                if (isComma) return textRange

                return nextLeaf()?.leafIgnoringWhitespaceAndComments(false)?.endOffset?.takeIf { it > 0 }?.let {
                    TextRange.create(it - 1, it).intersection(textRange)
                } ?: TextRange.create(textRange.endOffset - 1, textRange.endOffset)
            }
    }

    override fun createOptionsPanel(): JComponent? {
        val panel = MultipleCheckboxOptionsPanel(this)
        panel.addCheckbox(KotlinBundle.message("inspection.trailing.comma.report.also.a.missing.comma"), "addCommaWarning")
        return panel
    }
}