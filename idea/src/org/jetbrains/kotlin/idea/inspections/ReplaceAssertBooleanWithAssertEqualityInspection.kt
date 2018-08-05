/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.intentions.getCallableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReplaceAssertBooleanWithAssertEqualityInspection : AbstractApplicabilityBasedInspection<KtCallExpression>(KtCallExpression::class.java) {

    override fun inspectionText(element: KtCallExpression) = "Replace assert boolean with assert equality"

    override val defaultFixText = "Replace assert boolean with assert equality"

    override fun fixText(element: KtCallExpression): String {
        val assertion = element.replaceableAssertion() ?: return defaultFixText
        return "Replace with '$assertion'"
    }

    override fun isApplicable(element: KtCallExpression): Boolean {
        return (element.replaceableAssertion() != null)
    }

    override fun applyTo(element: PsiElement, project: Project, editor: Editor?) {
        val expression = element as? KtCallExpression ?: return
        val condition = expression.valueArguments.first().getArgumentExpression() as? KtBinaryExpression ?: return
        val left = condition.left ?: return
        val right = condition.right ?: return
        val assertion = expression.replaceableAssertion() ?: return
        val factory = KtPsiFactory(project)

        if (expression.valueArguments.size == 1) {
            expression.replace(factory.createExpressionByPattern("$assertion($0, $1)", left, right))
        } else if (expression.valueArguments.size == 2) {
            val message = expression.valueArguments[1].getArgumentExpression() ?: return
            expression.replace(factory.createExpressionByPattern("$assertion($0, $1, $2)", left, right, message))
        }
    }

    private fun KtCallExpression.replaceableAssertion(): String? {
        val referencedName = (calleeExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return null
        if (referencedName !in assertions) {
            return null
        }

        if (getCallableDescriptor()?.containingDeclaration?.fqNameSafe != FqName("kotlin.test")) {
            return null
        }

        if (valueArguments.size != 1 && valueArguments.size != 2) return null
        val binaryExpression = valueArguments.first().getArgumentExpression() as? KtBinaryExpression ?: return null
        val operationToken = binaryExpression.operationToken

        return assertionMap[Pair(referencedName, operationToken)]
    }

    companion object {
        private val assertions = setOf("assertTrue", "assertFalse")

        private val assertionMap = mapOf(
            Pair("assertTrue", KtTokens.EQEQ) to "assertEquals",
            Pair("assertTrue", KtTokens.EQEQEQ) to "assertSame",
            Pair("assertFalse", KtTokens.EQEQ) to "assertNotEquals",
            Pair("assertFalse", KtTokens.EQEQEQ) to "assertNotSame"
        )
    }
}