/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInspection.IntentionWrapper
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.inspections.AbstractKotlinInspection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.callExpressionVisitor
import org.jetbrains.kotlin.psi.createExpressionByPattern
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ConvertPairConstructorToToFunctionIntention : SelfTargetingIntention<KtCallExpression>(
    KtCallExpression::class.java, "Convert to 'to'"
) {
    override fun isApplicableTo(element: KtCallExpression, caretOffset: Int): Boolean {
        return element.isPairConstructorCall()
    }

    override fun applyTo(element: KtCallExpression, editor: Editor?) {
        val args = element.valueArguments.mapNotNull { it.getArgumentExpression() }.toTypedArray()
        element.replace(KtPsiFactory(element).createExpressionByPattern("$0 to $1", *args))
    }
}

class ConvertPairConstructorToToFunctionInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return callExpressionVisitor { expression ->
            if (expression.isPairConstructorCall()) {
                holder.registerProblem(
                    expression,
                    "Convert to 'to'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    IntentionWrapper(ConvertPairConstructorToToFunctionIntention(), expression.containingKtFile)
                )
            }
        }
    }
}

private fun KtCallExpression.isPairConstructorCall(): Boolean {
    if (valueArguments.size != 2) return false
    if (valueArguments.mapNotNull { it.getArgumentExpression() }.size != 2) return false
    return getCallableDescriptor()?.containingDeclaration?.fqNameSafe == FqName("kotlin.Pair")
}
