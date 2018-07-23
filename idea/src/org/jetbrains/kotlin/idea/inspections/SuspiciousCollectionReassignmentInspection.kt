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
import com.intellij.psi.PsiElementVisitor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.refactoring.introduce.introduceVariable.KotlinIntroduceVariableHandler
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfTypesAndPredicate
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SuspiciousCollectionReassignmentInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor =
        binaryExpressionVisitor(fun(binaryExpression) {
            if (binaryExpression.right == null) return
            if (binaryExpression.operationToken !in listOf(KtTokens.PLUSEQ, KtTokens.MINUSEQ)) return
            val left = binaryExpression.left ?: return
            if ((left.mainReference?.resolve() as? KtProperty)?.isVar != true) return

            val context = binaryExpression.analyze(BodyResolveMode.PARTIAL)
            val type = left.getType(context)?.constructor?.declarationDescriptor?.defaultType ?: return
            val builtIns = binaryExpression.builtIns
            if (type != builtIns.list.defaultType && type != builtIns.set.defaultType && type != builtIns.map.defaultType) return

            holder.registerProblem(
                binaryExpression,
                "'${left.text}' is reassigned by augmented assignment",
                ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                IntroduceLocalVariableFix()
            )
        })

    private class IntroduceLocalVariableFix : LocalQuickFix {
        override fun getName() = "Assign to local variable"

        override fun getFamilyName() = name

        override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
            val binaryExpression = descriptor.psiElement as? KtBinaryExpression ?: return
            val left = binaryExpression.left ?: return
            val right = binaryExpression.right ?: return
            val newOperation = when (binaryExpression.operationToken) {
                KtTokens.PLUSEQ -> KtTokens.PLUS.value
                KtTokens.MINUSEQ -> KtTokens.MINUS.value
                else -> return
            }
            val editor = binaryExpression.findExistingEditor() ?: return
            val replaced = binaryExpression.replaced(
                KtPsiFactory(binaryExpression).createExpressionByPattern("$0 $1 $2", left, newOperation, right)
            )
            KotlinIntroduceVariableHandler.doRefactoring(
                binaryExpression.project, editor, replaced, isVar = false, occurrencesToReplace = null, onNonInteractiveFinish = null
            )
        }
    }
}

