/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

class ReplaceJavaClassWithClassJavaFix(expression: KtDotQualifiedExpression) : KotlinQuickFixAction<KtDotQualifiedExpression>(expression) {
    override fun getText(): String = KotlinBundle.message("replace.with.0", "::class.java")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val expression = element ?: return
        element?.replace(KtPsiFactory(expression).createExpressionByPattern("$0::class.java", expression.receiverExpression))
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtDotQualifiedExpression>? {
            val expression = diagnostic.psiElement.parent as? KtDotQualifiedExpression ?: return null
            return ReplaceJavaClassWithClassJavaFix(expression)
        }
    }
}
