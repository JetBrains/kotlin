/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.util.CommentSaver
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.PsiChildRange
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class RemoveFunctionBodyFix(element: KtFunction) : KotlinQuickFixAction<KtFunction>(element) {
    override fun getFamilyName() = "Remove function body"

    override fun getText() = familyName

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile): Boolean {
        val element = element ?: return false
        return element.hasBody()
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val bodyExpression = element.bodyExpression!!
        val equalsToken = element.equalsToken
        if (equalsToken != null) {
            val commentSaver = CommentSaver(PsiChildRange(equalsToken.nextSibling, bodyExpression.prevSibling), true)
            element.deleteChildRange(equalsToken, bodyExpression)
            commentSaver.restore(element)
        } else {
            bodyExpression.delete()
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtFunction>? {
            val function = diagnostic.psiElement.getNonStrictParentOfType<KtFunction>() ?: return null
            return RemoveFunctionBodyFix(function)
        }
    }
}
