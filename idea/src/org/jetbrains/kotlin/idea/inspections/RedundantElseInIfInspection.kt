/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.formatter.adjustLineIndent
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isElseIf
import org.jetbrains.kotlin.idea.refactoring.getLineNumber
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.isNothing

class RedundantElseInIfInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        ifExpressionVisitor(fun(ifExpression) {
            if (ifExpression.elseKeyword == null || ifExpression.isElseIf()) return
            val elseKeyword = ifExpression.lastSingleElseKeyword() ?: return
            if (!ifExpression.hasRedundantElse()) return
            val rangeInElement = elseKeyword.textRange?.shiftRight(-ifExpression.startOffset) ?: return
            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    ifExpression,
                    rangeInElement,
                    "Redundant 'else'",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    isOnTheFly,
                    RemoveRedundantElseFix()
                )
            )
        })
}

private class RemoveRedundantElseFix : LocalQuickFix {
    override fun getName() = "Remove redundant 'else'"

    override fun getFamilyName() = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val ifExpression = descriptor.psiElement as? KtIfExpression ?: return
        val elseKeyword = ifExpression.lastSingleElseKeyword() ?: return
        val elseExpression = elseKeyword.getStrictParentOfType<KtIfExpression>()?.`else` ?: return

        val copy = elseExpression.copy()
        if (copy is KtBlockExpression) {
            copy.lBrace?.delete()
            copy.rBrace?.delete()
        }
        val parent = ifExpression.parent
        val added = parent.addAfter(copy, ifExpression)
        val elseKeywordLineNumber = elseKeyword.getLineNumber()
        val lastThenEndLine = elseKeyword.getPrevSiblingIgnoringWhitespaceAndComments()?.takeIf {
            it is KtContainerNodeForControlStructureBody && it.node.elementType == KtNodeTypes.THEN
        }?.getLineNumber(start = false)
        val elseStartLine = ((elseExpression as? KtBlockExpression)?.statements?.firstOrNull() ?: elseExpression).getLineNumber()
        if (elseKeywordLineNumber == lastThenEndLine && elseKeywordLineNumber == elseStartLine) {
            parent.addAfter(KtPsiFactory(ifExpression).createNewLine(), ifExpression)
        }
        elseExpression.delete()
        elseKeyword.delete()

        ifExpression.containingFile.adjustLineIndent(
            ifExpression.endOffset,
            (added.getNextSiblingIgnoringWhitespace() ?: added.parent).endOffset
        )
    }
}

private fun KtIfExpression.lastSingleElseKeyword(): PsiElement? {
    var ifExpression = this
    while (true) {
        ifExpression = ifExpression.`else` as? KtIfExpression ?: break
    }
    return ifExpression.elseKeyword
}

private fun KtIfExpression.hasRedundantElse(): Boolean {
    val context = analyze()
    if (context[BindingContext.USED_AS_EXPRESSION, this] == true) return false
    var ifExpression = this
    while (true) {
        if ((ifExpression.then)?.isReturnOrNothing(context) != true) return false
        ifExpression = ifExpression.`else` as? KtIfExpression ?: break
    }
    return true
}

private fun KtExpression.isReturnOrNothing(context: BindingContext): Boolean {
    val lastExpression = (this as? KtBlockExpression)?.statements?.lastOrNull() ?: this
    return lastExpression is KtReturnExpression || context.getType(lastExpression)?.isNothing() == true
}