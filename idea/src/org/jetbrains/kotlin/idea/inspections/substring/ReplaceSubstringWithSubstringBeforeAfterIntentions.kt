/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.substring

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression

class ReplaceSubstringWithSubstringAfterInspection : ReplaceSubstringInspection() {
    override fun inspectionText(element: KtDotQualifiedExpression): String = "Replace 'substring' call with 'substringAfter' call"

    override val defaultFixText: String = "Replace 'substring' call with 'substringAfter' call"

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        if (element !is KtDotQualifiedExpression) return
        element.replaceWith(
            "$0.substringAfter($1)",
            (element.getArgumentExpression(0) as KtDotQualifiedExpression).getArgumentExpression(0)
        )
    }

    override fun isApplicableInner(element: KtDotQualifiedExpression): Boolean {
        val arguments = element.callExpression?.valueArguments ?: return false
        return arguments.size == 1 && isIndexOfCall(arguments[0].getArgumentExpression(), element.receiverExpression)
    }
}

class ReplaceSubstringWithSubstringBeforeInspection : ReplaceSubstringInspection() {
    override fun inspectionText(element: KtDotQualifiedExpression): String = "Replace 'substring' call with 'substringBefore' call"

    override val defaultFixText: String = "Replace 'substring' call with 'substringBefore' call"

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        if (element !is KtDotQualifiedExpression) return
        element.replaceWith(
            "$0.substringBefore($1)",
            (element.getArgumentExpression(1) as KtDotQualifiedExpression).getArgumentExpression(0)
        )
    }

    override fun isApplicableInner(element: KtDotQualifiedExpression): Boolean {
        val arguments = element.callExpression?.valueArguments ?: return false

        return arguments.size == 2
                && element.isFirstArgumentZero()
                && isIndexOfCall(arguments[1].getArgumentExpression(), element.receiverExpression)
    }
}

private fun KtDotQualifiedExpression.getArgumentExpression(index: Int): KtExpression {
    return callExpression!!.valueArguments[index].getArgumentExpression()!!
}
