/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType


class AddInlineToFunctionFix(function: KtFunction) :
    KotlinQuickFixAction<KtFunction>(function) {
    override fun getFamilyName(): String = "Add 'inline' to function"

    override fun getText(): String = "Add 'inline' to function '${element?.name}'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.addModifier(KtTokens.INLINE_KEYWORD)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val function = diagnostic.psiElement.getParentOfType<KtFunction>(true) ?: return null
            if (function.isLocal) {
                return null
            }

            return AddInlineToFunctionFix(function)
        }
    }
}
