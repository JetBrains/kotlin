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
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.isSubtypeOf

class AddReturnToLastExpressionInFunctionFix(element: KtDeclarationWithBody) : KotlinQuickFixAction<KtDeclarationWithBody>(element) {
    override fun getText() = "Add 'return' to last expression"
    override fun getFamilyName() = text

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element as? KtNamedFunction ?: return false
        val block = element.bodyBlockExpression ?: return false
        val last = block.statements.lastOrNull() ?: return false

        val context = last.analyze(BodyResolveMode.PARTIAL)
        val lastType = last.getType(context) ?: return false
        if (lastType.isError) return false
        val expectedType = element.resolveToDescriptorIfAny()?.returnType ?: return false
        if (expectedType.isError || !lastType.isSubtypeOf(expectedType)) return false

        return true
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element as? KtNamedFunction ?: return
        val last = element.bodyBlockExpression?.statements?.lastOrNull() ?: return
        last.replace(KtPsiFactory(project).createExpression("return ${last.text}"))
    }

    companion object Factory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction {
            val casted = Errors.NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY.cast(diagnostic)
            return AddReturnToLastExpressionInFunctionFix(casted.psiElement)
        }
    }
}
