/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.isToString
import org.jetbrains.kotlin.psi.KtBlockStringTemplateEntry
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

class ReplaceToStringWithStringTemplateInspection : AbstractApplicabilityBasedInspection<KtDotQualifiedExpression>(
    KtDotQualifiedExpression::class.java
) {
    override fun isApplicable(element: KtDotQualifiedExpression): Boolean {
        if (element.receiverExpression !is KtReferenceExpression) return false
        if (element.parent is KtBlockStringTemplateEntry) return false
        return element.isToString()
    }

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val expression = element.getParentOfType<KtDotQualifiedExpression>(strict = false) ?: return
        val variable = expression.receiverExpression.text
        element.replace(KtPsiFactory(element).createExpression("\"$$variable\""))
    }

    override fun inspectionText(element: KtDotQualifiedExpression) = "Call of 'toString' could be replaced with string template"

    override fun inspectionTarget(element: KtDotQualifiedExpression) = element

    override val defaultFixText = "Replace 'toString' with string template"
}