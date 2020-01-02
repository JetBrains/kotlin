/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class ReplaceObsoleteLabelSyntaxFix(element: KtAnnotationEntry) : KotlinQuickFixAction<KtAnnotationEntry>(element), CleanupFix {
    override fun getFamilyName(): String = "Update obsolete label syntax"
    override fun getText(): String = element?.let { "Replace with label ${it.calleeExpression?.text ?: ""}@" } ?: ""

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        replaceWithLabel(element ?: return)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val annotationEntry = diagnostic.psiElement.getNonStrictParentOfType<KtAnnotationEntry>() ?: return null

            if (!looksLikeObsoleteLabel(annotationEntry)) return null

            return ReplaceObsoleteLabelSyntaxFix(annotationEntry)
        }

        fun looksLikeObsoleteLabel(entry: KtAnnotationEntry): Boolean = entry.atSymbol != null &&
                entry.parent is KtAnnotatedExpression &&
                (entry.parent as KtAnnotatedExpression).annotationEntries.size == 1 &&
                entry.valueArgumentList == null &&
                entry.calleeExpression?.constructorReferenceExpression?.getIdentifier() != null

        private fun replaceWithLabel(annotation: KtAnnotationEntry) {
            val labelName = annotation.calleeExpression?.constructorReferenceExpression?.getReferencedName() ?: return
            val annotatedExpression = annotation.parent as? KtAnnotatedExpression ?: return
            val expression = annotatedExpression.baseExpression ?: return

            if (annotatedExpression.annotationEntries.size != 1) return

            val baseExpressionStart = expression.textRange.startOffset

            val textRangeToRetain = TextRange(annotation.textRange.endOffset, baseExpressionStart)
            val textToRetain = textRangeToRetain.substring(annotation.containingFile.text)

            val labeledExpression = KtPsiFactory(annotation).createExpressionByPattern("$0@$1$2", labelName, textToRetain, expression)

            annotatedExpression.replace(labeledExpression)
        }
    }
}
