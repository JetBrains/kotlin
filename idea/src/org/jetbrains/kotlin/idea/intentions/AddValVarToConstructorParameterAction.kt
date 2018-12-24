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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.template.TemplateBuilderImpl
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.quickfix.KotlinQuickFixAction
import org.jetbrains.kotlin.idea.quickfix.KotlinSingleIntentionActionFactory
import org.jetbrains.kotlin.idea.refactoring.ValVarExpression
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

interface AddValVarToConstructorParameterAction {
    companion object {
        val actionFamily = "Add val/var to primary constructor parameter"
    }

    fun getActionText(element: KtParameter) = "Add val/var to parameter '${element.name ?: ""}'"

    fun canInvoke(element: KtParameter): Boolean {
        return element.valOrVarKeyword == null && (element.parent as? KtParameterList)?.parent is KtPrimaryConstructor
    }

    fun invoke(element: KtParameter, editor: Editor?) {
        val project = element.project

        element.addBefore(KtPsiFactory(project).createValKeyword(), element.nameIdentifier)

        if (editor == null) return

        val parameter = element.createSmartPointer().let {
            PsiDocumentManager.getInstance(project).commitAllDocuments()
            PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)
            it.element
        } ?: return

        editor.caretModel.moveToOffset(parameter.startOffset)

        TemplateBuilderImpl(parameter)
            .apply { replaceElement(parameter.valOrVarKeyword ?: return@apply, ValVarExpression) }
            .buildInlineTemplate()
            .let { TemplateManager.getInstance(project).startTemplate(editor, it) }
    }

    class Intention :
            SelfTargetingRangeIntention<KtParameter>(KtParameter::class.java, actionFamily),
            AddValVarToConstructorParameterAction {
        override fun applicabilityRange(element: KtParameter): TextRange? {
            if (!canInvoke(element)) return null
            if (element.getStrictParentOfType<KtClass>()?.isData() ?: false) return null
            text = getActionText(element)
            return element.nameIdentifier?.textRange
        }

        override fun applyTo(element: KtParameter, editor: Editor?) = invoke(element, editor)
    }

    class QuickFix(parameter: KtParameter) :
            KotlinQuickFixAction<KtParameter>(parameter),
            AddValVarToConstructorParameterAction {
        override fun getText() = element?.let { getActionText(it) } ?: ""

        override fun getFamilyName() = actionFamily

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            invoke(element ?: return, editor)
        }
    }

    object QuickFixFactory : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic) = QuickFix(Errors.DATA_CLASS_NOT_PROPERTY_PARAMETER.cast(diagnostic).psiElement)
    }
}