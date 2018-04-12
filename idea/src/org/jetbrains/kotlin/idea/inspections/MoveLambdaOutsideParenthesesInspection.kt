/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.core.canMoveLambdaOutsideParentheses
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParentheses
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class MoveLambdaOutsideParenthesesInspection : AbstractApplicabilityBasedInspection<KtCallExpression>(KtCallExpression::class.java) {
    override fun isApplicable(element: KtCallExpression) = element.canMoveLambdaOutsideParentheses()

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val expression = element.getParentOfType<KtCallExpression>(strict = false) ?: return

        if (expression.canMoveLambdaOutsideParentheses()) {
            expression.moveFunctionLiteralOutsideParentheses()
        }
    }

    override fun inspectionText(element: KtCallExpression) = "Lambda argument should be moved out of parentheses"

    override fun inspectionTarget(element: KtCallExpression): KtElement {
        return element.getLastLambdaExpression()?.getStrictParentOfType<KtValueArgument>()?.asElement() ?: element
    }

    override val defaultFixText = "Move lambda argument out of parentheses"
}