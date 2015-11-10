/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.quickfix.quickfixUtil.createIntentionFactory
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

public class ReplaceObsoleteLabelSyntaxFix(element: KtAnnotationEntry) : KotlinQuickFixAction<KtAnnotationEntry>(element), CleanupFix {
    override fun getFamilyName(): String = "Update obsolete label syntax"
    override fun getText(): String = "Replace with label ${element.getCalleeExpression()?.getText() ?: ""}@"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) = replaceWithLabel(element)

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val annotationEntry = diagnostic.getPsiElement().getNonStrictParentOfType<KtAnnotationEntry>() ?: return null

            if (!looksLikeObsoleteLabel(annotationEntry)) return null

            return ReplaceObsoleteLabelSyntaxFix(annotationEntry)
        }

        public fun createWholeProjectFixFactory(): KotlinSingleIntentionActionFactory = createIntentionFactory factory@ {
            diagnostic ->

            if (!(diagnostic.getPsiElement().getNonStrictParentOfType<KtAnnotationEntry>()?.looksLikeObsoleteLabelWithReferencesInCode()
                ?: false)) return@factory null

            WholeProjectForEachElementOfTypeFix.createForMultiTaskOnElement<KtAnnotatedExpression, KtAnnotationEntry>(
                    tasksFactory = { collectTasks(it) },
                    tasksProcessor ={ it.forEach { ann -> replaceWithLabel(ann) } },
                    name = "Update obsolete label syntax in whole project"
            )
        }

        private fun collectTasks(expression: KtAnnotatedExpression) =
                expression.getAnnotationEntries().filter { it.looksLikeObsoleteLabelWithReferencesInCode() }

        private fun KtAnnotationEntry.looksLikeObsoleteLabelWithReferencesInCode(): Boolean {
            if (!looksLikeObsoleteLabel(this)) return false

            val baseExpression = (getParent() as? KtAnnotatedExpression)?.getBaseExpression() ?: return false

            val nameExpression = getCalleeExpression()?.getConstructorReferenceExpression() ?: return false
            val labelName = nameExpression.getReferencedName()

            return baseExpression.anyDescendantOfType<KtExpressionWithLabel> {
                (it is KtBreakExpression || it is KtContinueExpression || it is KtReturnExpression) &&
                it.getLabelName() == labelName &&
                it.getTargetLabel()?.analyze()?.get(BindingContext.LABEL_TARGET, it.getTargetLabel()) == null
            } && analyze().getDiagnostics().forElement(nameExpression).any { it.getFactory() == Errors.UNRESOLVED_REFERENCE }
        }

        public fun looksLikeObsoleteLabel(entry: KtAnnotationEntry): Boolean =
                entry.getAtSymbol() != null &&
                entry.getParent() is KtAnnotatedExpression &&
                (entry.getParent() as KtAnnotatedExpression).getAnnotationEntries().size() == 1 &&
                entry.getValueArgumentList() == null &&
                entry.getCalleeExpression()?.getConstructorReferenceExpression()?.getIdentifier() != null

        private fun replaceWithLabel(annotation: KtAnnotationEntry) {
            val labelName = annotation.getCalleeExpression()?.getConstructorReferenceExpression()?.getReferencedName() ?: return
            val annotatedExpression = annotation.getParent() as? KtAnnotatedExpression ?: return
            val expression = annotatedExpression.getBaseExpression() ?: return

            if (annotatedExpression.getAnnotationEntries().size() != 1) return

            val baseExpressionStart = expression.getTextRange().getStartOffset()

            val textRangeToRetain = TextRange(annotation.getTextRange().getEndOffset(), baseExpressionStart)
            val textToRetain = textRangeToRetain.substring(annotation.getContainingFile().getText())

            val labeledExpression = KtPsiFactory(annotation).createExpressionByPattern("$0@$1$2", labelName, textToRetain, expression)

            annotatedExpression.replace(labeledExpression)
        }
    }
}
