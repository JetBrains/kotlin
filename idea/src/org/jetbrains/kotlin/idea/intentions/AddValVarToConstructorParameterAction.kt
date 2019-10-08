/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.util.isExpectDeclaration
import org.jetbrains.kotlin.idea.util.mustHaveValOrVar
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.startOffset

interface AddValVarToConstructorParameterAction {
    companion object {
        const val actionFamily = "Add val/var to primary constructor parameter"
    }

    fun getActionText(element: KtParameter) = "Add val/var to parameter '${element.name ?: ""}'"

    fun canInvoke(element: KtParameter): Boolean {
        return element.valOrVarKeyword == null && ((element.parent as? KtParameterList)?.parent as? KtPrimaryConstructor)
            ?.takeIf { it.mustHaveValOrVar() || !it.isExpectDeclaration() } != null
    }

    operator fun invoke(element: KtParameter, editor: Editor?) {
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
            if (element.getStrictParentOfType<KtClass>()?.isData() == true) return null
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