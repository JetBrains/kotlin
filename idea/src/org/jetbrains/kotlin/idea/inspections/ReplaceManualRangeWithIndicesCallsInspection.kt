/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.idea.intentions.getArguments
import org.jetbrains.kotlin.idea.intentions.isSizeOrLength
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

class ReplaceManualRangeWithIndicesCallsInspection : AbstractKotlinInspection() {
    val rangeFunctions = setOf("until", "rangeTo")
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) = object : KtVisitorVoid() {
        override fun visitBinaryExpression(binaryExpression: KtBinaryExpression) {
            val find = rangeFunctions.find { it == binaryExpression.operationReference.text }
            if (binaryExpression.operationToken == KtTokens.RANGE || find != null) {
                val operator = find ?: "rangeTo"
                visitRange(holder, binaryExpression, binaryExpression.left ?: return, binaryExpression.right ?: return, operator)
            }
        }

        override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
            val find = rangeFunctions.find { it == expression.selectorExpression?.text }
            if (find != null) {
                val call = (expression.parent as? KtCallExpression) ?: return
                val firstArg = call.valueArguments.first().getArgumentExpression() ?: return
                visitRange(holder, expression, expression.receiverExpression, firstArg, find)
            }
        }
    }

    private fun visitRange(holder: ProblemsHolder, expression: KtExpression, left: KtExpression, right: KtExpression, method: String) {
        if ((method == "until" && left.toIntConstant() == 0 && right.receiverIfIsSizeOrLengthCall() != null) ||
            (method == "rangeTo" && left.toIntConstant() == 0 && right.receiverIfIsSizeOrLengthMinusOneCall() != null)
        ) {
            visitIndicesRange(holder, expression)
        }
    }

    private fun visitIndicesRange(holder: ProblemsHolder, range: KtExpression) {
        val parent = range.parent.parent
        if (parent is KtForExpression) {
            val paramElement = parent.loopParameter?.originalElement ?: return
            val usageElement = ReferencesSearch.search(paramElement).singleOrNull()?.element
            val arrayAccess = usageElement?.parent?.parent as? KtArrayAccessExpression
            if (arrayAccess != null && arrayAccess.indexExpressions.singleOrNull() == usageElement) {
                val arrayAccessParent = arrayAccess.parent
                if (arrayAccessParent !is KtBinaryExpression ||
                    arrayAccessParent.left != arrayAccess ||
                    arrayAccessParent.operationToken !in KtTokens.ALL_ASSIGNMENTS
                ) {
                    holder.registerProblem(
                        range,
                        "For loop over indices could be replaced with loop over elements",
                        ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                        ReplaceIndexLoopWithCollectionLoopQuickFix()
                    )
                    return
                }
            }
        }
        holder.registerProblem(
            range,
            "Range could be replaced with '.indices' call",
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            ReplaceManualRangeWithIndicesCallQuickFix()
        )
    }
}

class ReplaceManualRangeWithIndicesCallQuickFix : LocalQuickFix {
    override fun getName() = "Replace with indices"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement as KtExpression
        val args = element.getArguments() ?: return
        if (args.second is KtBinaryExpression) {
            val second = (args.second as? KtBinaryExpression) ?: return
            replaceWithIndices(element, (second.left as? KtDotQualifiedExpression)?.receiverExpression ?: return)
        } else {
            replaceWithIndices(element, (args.second as? KtDotQualifiedExpression)?.receiverExpression ?: return)
        }
    }

    private fun replaceWithIndices(toReplace: KtExpression, receiver: KtExpression) {
        toReplace.replace(KtPsiFactory(toReplace).createExpressionByPattern("$0.indices", receiver))
    }
}

class ReplaceIndexLoopWithCollectionLoopQuickFix : LocalQuickFix {
    override fun getName() = "Replace with loop over elements"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val element = descriptor.psiElement.getStrictParentOfType<KtForExpression>() ?: return
        val loopParameter = element.loopParameter ?: return
        val loopRange = element.loopRange ?: return
        val collectionParent = when (loopRange) {
            is KtDotQualifiedExpression -> (loopRange.parent as? KtCallExpression)?.valueArguments?.firstOrNull()?.getArgumentExpression()
            is KtBinaryExpression -> loopRange.right
            else -> null
        } ?: return
        val collection =
            collectionParent.receiverIfIsSizeOrLengthCall() ?: collectionParent.receiverIfIsSizeOrLengthMinusOneCall() ?: return
        val paramElement = loopParameter.originalElement ?: return
        val usageElement = ReferencesSearch.search(paramElement).singleOrNull()?.element ?: return
        val arrayAccessElement = usageElement.parent.parent as? KtArrayAccessExpression ?: return
        val factory = KtPsiFactory(project)
        val newParameter = factory.createLoopParameter("element")
        val newReferenceExpression = factory.createExpression("element")
        arrayAccessElement.replace(newReferenceExpression)
        loopParameter.replace(newParameter)
        loopRange.replace(collection)
    }
}

private fun KtExpression.toIntConstant(): Int? {
    return (this as? KtConstantExpression)?.text?.toIntOrNull()
}

fun KtExpression.receiverIfIsSizeOrLengthCall(): KtExpression? {
    if (this.isSizeOrLength()) {
        return (this as? KtDotQualifiedExpression)?.receiverExpression ?: return null
    }
    return null
}

fun KtExpression.receiverIfIsSizeOrLengthMinusOneCall(): KtExpression? {
    if (this !is KtBinaryExpression) return null
    if (this.operationToken != KtTokens.MINUS) return null
    val collection = this.left?.receiverIfIsSizeOrLengthCall() ?: return null
    val constant = this.right?.toIntConstant() ?: return null
    if (constant == 1) return collection
    return null
}
