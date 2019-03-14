/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class AddReturnToUnusedLastExpressionInFunctionFix(element: KtElement) : KotlinQuickFixAction<KtElement>(element) {
    override fun getText() = "Add 'return' before the expression"
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val expr = element as? KtExpression ?: return false
        val context = expr.analyze(BodyResolveMode.PARTIAL)
        if (!expr.isLastStatementInFunctionBody()) return false

        val exprType = expr.getType(context) ?: return false
        if (exprType.isError) return false

        val function = expr.parent.parent as? KtNamedFunction ?: return false
        val functionReturnType = function.resolveToDescriptorIfAny()?.returnType ?: return false
        if (functionReturnType.isError || !exprType.isSubtypeOf(functionReturnType)) return false

        return true
    }

    private fun KtExpression.isLastStatementInFunctionBody(): Boolean {
        val body = this.parent as? KtBlockExpression ?: return false
        val last = body.statements.lastOrNull() ?: return false
        return last === this
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        element.replace(KtPsiFactory(project).createExpression("return ${element.text}"))
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction {
            val casted = Errors.UNUSED_EXPRESSION.cast(diagnostic)
            return AddReturnToUnusedLastExpressionInFunctionFix(casted.psiElement)
        }
    }
}
