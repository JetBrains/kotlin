/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.canDropBraces
import org.jetbrains.kotlin.idea.core.dropBraces
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry

class RemoveCurlyBracesFromTemplateInspection(@JvmField var reportWithoutWhitespace: Boolean = false) :
    AbstractApplicabilityBasedInspection<KtBlockStringTemplateEntry>(KtBlockStringTemplateEntry::class.java) {
    override fun inspectionText(element: KtBlockStringTemplateEntry): String = "Redundant curly braces in string template"

    override fun inspectionHighlightType(element: KtBlockStringTemplateEntry) =
        if (reportWithoutWhitespace || element.hasWhitespaceAround()) ProblemHighlightType.GENERIC_ERROR_OR_WARNING
        else ProblemHighlightType.INFORMATION

    override val defaultFixText: String = "Remove curly braces"

    override fun isApplicable(element: KtBlockStringTemplateEntry): Boolean = element.canDropBraces()

    override fun applyTo(element: KtBlockStringTemplateEntry, project: Project, editor: Editor?) {
        element.dropBraces()
    }

    override fun createOptionsPanel() = MultipleCheckboxOptionsPanel(this).apply {
        addCheckbox("Report also for a variables without a whitespace around", "reportWithoutWhitespace")
    }
}

private fun KtBlockStringTemplateEntry.hasWhitespaceAround(): Boolean =
    prevSibling?.isWhitespaceOrQuote(true) == true && nextSibling?.isWhitespaceOrQuote(false) == true

private fun PsiElement.isWhitespaceOrQuote(prev: Boolean): Boolean {
    val char = if (prev) text.lastOrNull() else text.firstOrNull()
    return char != null && (char.isWhitespace() || char == '"')
}