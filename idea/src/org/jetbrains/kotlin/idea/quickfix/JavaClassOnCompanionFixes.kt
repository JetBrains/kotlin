/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createExpressionByPattern

object JavaClassOnCompanionFixes : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val expression = diagnostic.psiElement.parent as? KtDotQualifiedExpression ?: return emptyList()
        return listOf(
            ReplaceWithCompanionClassJavaFix(expression),
            ReplaceWithClassJavaFix(expression)
        )
    }
}

class ReplaceWithCompanionClassJavaFix(
    expression: KtDotQualifiedExpression
) : KotlinQuickFixAction<KtDotQualifiedExpression>(expression) {
    override fun getText(): String = KotlinBundle.message("replace.with.0", "Companion::class.java")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.replace("$0.Companion::class.java")
    }
}

class ReplaceWithClassJavaFix(
    expression: KtDotQualifiedExpression
) : KotlinQuickFixAction<KtDotQualifiedExpression>(expression), LowPriorityAction {
    override fun getText(): String = KotlinBundle.message("replace.with.0", "::class.java")

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        element?.replace("$0::class.java")
    }
}

private fun KtDotQualifiedExpression.replace(pattern: String) {
    replace(KtPsiFactory(this).createExpressionByPattern(pattern, receiverExpression))
}
