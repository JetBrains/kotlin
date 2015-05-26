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
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespaceAndComments
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.resolve.BindingContext

public class ReplaceObsoleteLabelSyntaxFix(element: JetAnnotationEntry?) : JetIntentionAction<JetAnnotationEntry>(element), CleanupFix {
    override fun getFamilyName(): String = "Update obsolete label syntax"
    override fun getText(): String = "Replace with label ${element.getCalleeExpression()?.getText() ?: ""}@"

    override fun invoke(project: Project, editor: Editor?, file: JetFile) = replaceWithLabel(element)

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val annotationEntry = diagnostic.getPsiElement().getNonStrictParentOfType<JetAnnotationEntry>() ?: return null

            if (!annotationEntry.looksLikeObsoleteLabel()) return null

            return ReplaceObsoleteLabelSyntaxFix(annotationEntry)
        }

        public fun createWholeProjectFixFactory(): JetSingleIntentionActionFactory = createIntentionFactory factory@ {
            diagnostic ->

            if (!(diagnostic.getPsiElement().getNonStrictParentOfType<JetAnnotationEntry>()?.looksLikeObsoleteLabelWithReferencesInCode()
                ?: false)) return@factory null

            JetWholeProjectForEachElementOfTypeFix.createForMultiTask<JetAnnotatedExpression, JetAnnotationEntry>(
                    tasksFactory = { collectTasks(it) },
                    tasksProcessor ={ it.forEach { ann -> replaceWithLabel(ann) } },
                    modalTitle = "Replacing labels with obsolete syntax",
                    name = "Update obsolete label syntax in whole project",
                    familyName = "Update obsolete label syntax in whole project"
            )
        }

        private fun collectTasks(expression: JetAnnotatedExpression) =
                expression.getAnnotationEntries().filter { it.looksLikeObsoleteLabelWithReferencesInCode() }

        private fun JetAnnotationEntry.looksLikeObsoleteLabelWithReferencesInCode(): Boolean {
            if (!looksLikeObsoleteLabel()) return false

            val baseExpression = (getParent() as? JetAnnotatedExpression)?.getBaseExpression() ?: return false

            val nameExpression = getCalleeExpression().getConstructorReferenceExpression() ?: return false
            val labelName = nameExpression.getReferencedName()

            return baseExpression.anyDescendantOfType<JetExpressionWithLabel> {
                (it is JetBreakExpression || it is JetContinueExpression || it is JetReturnExpression) &&
                it.getLabelName() == labelName &&
                it.getTargetLabel()?.analyze()?.get(BindingContext.LABEL_TARGET, it.getTargetLabel()) == null
            } && analyze().getDiagnostics().forElement(nameExpression).any { it.getFactory() == Errors.UNRESOLVED_REFERENCE }
        }

        private fun replaceWithLabel(annotation: JetAnnotationEntry) {
            val labelName = annotation.getCalleeExpression()?.getConstructorReferenceExpression()?.getReferencedName() ?: return
            val annotatedExpression = annotation.getParent() as? JetAnnotatedExpression ?: return
            val expression = annotatedExpression.getBaseExpression() ?: return

            if (annotatedExpression.getAnnotationEntries().size() != 1) return

            val baseExpressionStart = expression.getTextRange().getStartOffset()

            val textRangeToRetain = TextRange(annotation.getTextRange().getEndOffset(), baseExpressionStart)
            val textToRetain = textRangeToRetain.substring(annotation.getContainingFile().getText())

            val labeledExpression = JetPsiFactory(annotation).createExpressionByPattern("$labelName@$0$1", textToRetain, expression)

            annotatedExpression.replace(labeledExpression)
        }
    }
}

public fun JetAnnotationEntry.looksLikeObsoleteLabel(): Boolean =
        getAtSymbol() != null &&
        getParent() is JetAnnotatedExpression &&
        (getParent() as JetAnnotatedExpression).getAnnotationEntries().size() == 1 &&
        getValueArgumentList() == null &&
        getCalleeExpression()?.getConstructorReferenceExpression()?.getIdentifier() != null
