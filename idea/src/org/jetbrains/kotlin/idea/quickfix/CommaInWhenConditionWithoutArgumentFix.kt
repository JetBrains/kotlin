/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.combineWhenConditions
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import java.util.*


class CommaInWhenConditionWithoutArgumentFix(element: PsiElement) : KotlinQuickFixAction<PsiElement>(element), CleanupFix {
    override fun getFamilyName(): String = text
    override fun getText(): String = "Replace ',' with '||' in when"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val whenExpression = element as? KtWhenExpression ?: return
        replaceCommasWithOrsInWhenExpression(whenExpression)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? =
            diagnostic.psiElement.parent?.parent?.let(::CommaInWhenConditionWithoutArgumentFix)

        private class WhenEntryConditionsData(
            val conditions: Array<KtWhenCondition>,
            val first: PsiElement,
            val last: PsiElement,
            val arrow: PsiElement
        )

        private fun replaceCommasWithOrsInWhenExpression(whenExpression: KtWhenExpression) {
            for (whenEntry in whenExpression.entries) {
                if (whenEntry.conditions.size > 1) {
                    val conditionsData = getConditionsDataOrNull(whenEntry) ?: return
                    val replacement = KtPsiFactory(whenEntry).combineWhenConditions(conditionsData.conditions, null) ?: return
                    whenEntry.deleteChildRange(conditionsData.first, conditionsData.last)
                    whenEntry.addBefore(replacement, conditionsData.arrow)
                }
            }
        }

        private fun getConditionsDataOrNull(whenEntry: KtWhenEntry): WhenEntryConditionsData? {
            val conditions = ArrayList<KtWhenCondition>()

            var arrow: PsiElement? = null

            var child = whenEntry.firstChild
            whenEntryChildren@ while (child != null) {
                when {
                    child is KtWhenConditionWithExpression -> {
                        conditions.add(child)
                    }
                    child.node.elementType == KtTokens.ARROW -> {
                        arrow = child
                        break@whenEntryChildren
                    }
                }
                child = child.nextSibling
            }

            val last = child?.prevSibling

            return if (arrow != null && last != null)
                WhenEntryConditionsData(conditions.toTypedArray(), whenEntry.firstChild, last, arrow)
            else
                null
        }

    }

}