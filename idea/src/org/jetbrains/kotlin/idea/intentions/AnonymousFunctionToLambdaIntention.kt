/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.core.getLastLambdaExpression
import org.jetbrains.kotlin.idea.core.moveFunctionLiteralOutsideParenthesesIfPossible
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.contentRange
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypeAndBranch
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.resolve.calls.callUtil.getCalleeExpressionIfAny
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

class AnonymousFunctionToLambdaIntention : SelfTargetingRangeIntention<KtNamedFunction>(
    KtNamedFunction::class.java,
    "Convert to lambda expression",
    "Convert anonymous function to lambda expression"
) {
    override fun applicabilityRange(element: KtNamedFunction): TextRange? {
        if (element.name != null) return null

        if (!element.hasBody()) return null

        val callElement = element.getParentOfTypeAndBranch<KtCallElement> { valueArgumentList } ?: return null
        if (callElement.getCalleeExpressionIfAny() !is KtNameReferenceExpression) {
            return null
        }

        return element.funKeyword?.textRange
    }

    override fun applyTo(element: KtNamedFunction, editor: Editor?) {
        val commentSaver = CommentSaver(element)
        val returnSaver = ReturnSaver(element)

        val body = element.bodyExpression!!

        val newExpression = KtPsiFactory(element).buildExpression {
            appendFixedText("{")

            val parameters = element.valueParameters

            val needParameters =
                parameters.count() > 1 || parameters.any { parameter -> ReferencesSearch.search(parameter, LocalSearchScope(body)).any() }
            if (needParameters) {
                parameters.forEachIndexed { index, parameter ->
                    if (index > 0) {
                        appendFixedText(",")
                    }
                    appendName(parameter.nameAsSafeName)
                }

                appendFixedText("->")
            }

            if (element.hasBlockBody()) {
                appendChildRange((body as KtBlockExpression).contentRange())
            } else {
                appendExpression(body)
            }

            appendFixedText("}")
        }

        val replaced = element.replaced(newExpression) as KtLambdaExpression
        commentSaver.restore(replaced, forceAdjustIndent = true/* by some reason lambda body is sometimes not properly indented */)

        val callExpression = replaced.parents.firstIsInstance<KtCallExpression>()
        val callee = callExpression.getCalleeExpressionIfAny()!! as KtNameReferenceExpression

        val returnLabel = callee.getReferencedNameAsName()
        returnSaver.restore(replaced, returnLabel)

        callExpression.getLastLambdaExpression()?.moveFunctionLiteralOutsideParenthesesIfPossible()
    }
}