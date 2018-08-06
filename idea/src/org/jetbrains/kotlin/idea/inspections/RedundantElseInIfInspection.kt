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
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isElseIf
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.isNothing

class RedundantElseInIfInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        ifExpressionVisitor(fun(ifExpression) {
            if (ifExpression.isElseIf()) return
            val elseKeyword = ifExpression.singleElseKeyword(true) ?: return
            val rangeInElement = elseKeyword.textRange?.shiftRight(-ifExpression.startOffset) ?: return
            holder.registerProblem(
                holder.manager.createProblemDescriptor(
                    ifExpression,
                    rangeInElement,
                    "Redundant 'else'",
                    ProblemHighlightType.LIKE_UNUSED_SYMBOL,
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
        val elseKeyword = ifExpression.singleElseKeyword(false) ?: return
        val elseExpression = elseKeyword.getStrictParentOfType<KtIfExpression>()?.`else` ?: return

        val copy = elseExpression.copy()
        if (copy is KtBlockExpression) {
            copy.lBrace?.delete()
            copy.rBrace?.delete()
        }
        val added = ifExpression.parent.addAfter(copy, ifExpression)
        elseExpression.delete()
        elseKeyword.delete()
        val nextSibling = added.nextSibling

        if (nextSibling != null) {
            val editor = ifExpression.findExistingEditor()
            if (editor != null) {
                val document = editor.document
                val documentManager = PsiDocumentManager.getInstance(project)
                val psiFile = documentManager.getPsiFile(document)
                if (psiFile != null) {
                    documentManager.commitDocument(document)
                    documentManager.doPostponedOperationsAndUnblockDocument(document)
                    CodeStyleManager.getInstance(project).adjustLineIndent(
                        psiFile, TextRange(ifExpression.endOffset, nextSibling.endOffset)
                    )
                }
            }
        }
    }
}

private fun KtIfExpression.singleElseKeyword(checkRedundant: Boolean): PsiElement? {
    var ifExpression = this
    val thenExpressions = mutableListOf<KtExpression?>()
    while (true) {
        thenExpressions.add(ifExpression.then)
        ifExpression = ifExpression.`else` as? KtIfExpression ?: break
    }
    val elseKeyword = ifExpression.elseKeyword ?: return null

    if (checkRedundant) {
        val context = analyze()
        if (context[BindingContext.USED_AS_EXPRESSION, this] == true) return null
        if (thenExpressions.any { !it.isReturnOrNothing(context) }) return null
    }

    return elseKeyword
}

private fun KtExpression?.isReturnOrNothing(context: BindingContext): Boolean {
    if (this == null) return false
    val lastExpression = (this as? KtBlockExpression)?.statements?.lastOrNull() ?: this
    return lastExpression is KtReturnExpression || context.getType(lastExpression)?.isNothing() == true
}