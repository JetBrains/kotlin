/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.substring

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.evaluatesTo
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

class ReplaceSubstringWithDropLastInspection : ReplaceSubstringInspection() {
    override fun inspectionText(element: KtDotQualifiedExpression): String = "Replace 'substring' call with 'dropLast' call"
    override val defaultFixText: String = "Replace 'substring' call with 'dropLast' call"

    override fun applyTo(element: KtDotQualifiedExpression, project: Project, editor: Editor?) {
        val argument = element.callExpression?.valueArguments?.elementAtOrNull(1)?.getArgumentExpression() ?: return
        val rightExpression = (argument as? KtBinaryExpression)?.right ?: return

        element.replaceWith("$0.dropLast($1)", rightExpression)
    }

    override fun isApplicableInner(element: KtDotQualifiedExpression): Boolean {
        val arguments = element.callExpression?.valueArguments ?: return false
        if (arguments.size != 2 || !element.isFirstArgumentZero()) return false

        val secondArgumentExpression = arguments[1].getArgumentExpression() as? KtBinaryExpression ?: return false

        if (secondArgumentExpression.operationReference.getReferencedNameElementType() != KtTokens.MINUS) return false
        return isLengthAccess(secondArgumentExpression.left, element.receiverExpression)
    }

    private fun isLengthAccess(expression: KtExpression?, expectedReceiver: KtExpression): Boolean =
        expression is KtDotQualifiedExpression
                && expression.selectorExpression.let { it is KtNameReferenceExpression && it.getReferencedName() == "length" }
                && expression.receiverExpression.evaluatesTo(expectedReceiver)
}
