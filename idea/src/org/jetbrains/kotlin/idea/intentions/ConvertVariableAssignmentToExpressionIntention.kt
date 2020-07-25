/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.references.KtSimpleReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*

class ConvertVariableAssignmentToExpressionIntention : SelfTargetingIntention<KtBinaryExpression>(
    KtBinaryExpression::class.java,
    KotlinBundle.lazyMessage("convert.to.assignment.expression"),
) {
    override fun isApplicableTo(element: KtBinaryExpression, caretOffset: Int): Boolean {
        if (element.operationToken == KtTokens.EQ) return true
        return false
    }

    override fun applyTo(element: KtBinaryExpression, editor: Editor?) {
        val left = element.left ?: return
        val right = element.right ?: return
        val newElement = KtPsiFactory(element.project).createExpressionByPattern("$0.also { $1 = it }", right, left)
        element.replace(newElement)
    }
}
