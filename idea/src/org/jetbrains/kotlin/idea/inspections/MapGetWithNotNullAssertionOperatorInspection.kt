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
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.intentions.callExpression
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MapGetWithNotNullAssertionOperatorInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean) =
        postfixExpressionVisitor(fun(expression: KtPostfixExpression) {
            if (expression.operationToken != KtTokens.EXCLEXCL) return
            if (expression.getReplacementData() == null) return
            if (expression.baseExpression?.resolveToCall()?.resultingDescriptor?.fqNameSafe != FqName("kotlin.collections.Map.get")) return
            holder.registerProblem(
                expression.operationReference,
                "map.get() with not-null assertion operator (!!)",
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                ReplaceWithGetValueCallFix(),
                ReplaceWithGetOrElseFix(),
                ReplaceWithElvisErrorFix()
            )
        })

    private class ReplaceWithGetValueCallFix : LocalQuickFix {
        override fun getName() = "Replace with 'getValue' call"
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement.parent as? KtPostfixExpression ?: return
            val (reference, index) = expression.getReplacementData() ?: return
            val replaced = expression.replaced(KtPsiFactory(expression).createExpressionByPattern("$0.getValue($1)", reference, index))
            replaced.findExistingEditor()?.caretModel?.moveToOffset(replaced.endOffset)
        }
    }

    private class ReplaceWithGetOrElseFix : LocalQuickFix {
        override fun getName() = "Replace with 'getOrElse' call"
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement.parent as? KtPostfixExpression ?: return
            val (reference, index) = expression.getReplacementData() ?: return
            val replaced = expression.replaced(KtPsiFactory(expression).createExpressionByPattern("$0.getOrElse($1){}", reference, index))

            val editor = replaced.findExistingEditor() ?: return
            val offset = (replaced as KtQualifiedExpression).callExpression?.lambdaArguments?.firstOrNull()?.startOffset ?: return
            val document = editor.document
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
            document.insertString(offset + 1, "  ")
            editor.caretModel.moveToOffset(offset + 2)
        }
    }

    private class ReplaceWithElvisErrorFix : LocalQuickFix {
        override fun getName() = "Replace with '?: error(\"\")'"
        override fun getFamilyName() = name
        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val expression = descriptor.psiElement.parent as? KtPostfixExpression ?: return
            val (reference, index) = expression.getReplacementData() ?: return
            val replaced = expression.replace(KtPsiFactory(expression).createExpressionByPattern("$0[$1] ?: error(\"\")", reference, index))

            val editor = replaced.findExistingEditor() ?: return
            val offset = (replaced as? KtBinaryExpression)?.right?.endOffset ?: return
            editor.caretModel.moveToOffset(offset - 2)
        }
    }
}

private fun KtPostfixExpression.getReplacementData(): Pair<KtExpression, KtExpression>? {
    val base = baseExpression
    when (base) {
        is KtQualifiedExpression -> {
            if (base.callExpression?.calleeExpression?.text != "get") return null
            val reference = base.receiverExpression
            val index = base.callExpression?.valueArguments?.firstOrNull()?.getArgumentExpression() ?: return null
            return reference to index
        }
        is KtArrayAccessExpression -> {
            val reference = base.arrayExpression ?: return null
            val index = base.indexExpressions.firstOrNull() ?: return null
            return reference to index
        }
        else -> return null
    }
}