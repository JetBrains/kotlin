/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.ListPopupStep
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.analysis.analyzeAsReplacement
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForReceiver
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class SpecifySuperTypeFix(
    superExpression: KtSuperExpression,
    private val superTypes: List<String>
) : KotlinQuickFixAction<KtSuperExpression>(superExpression) {

    override fun getText() = "Specify supertype"

    override fun getFamilyName() = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        if (editor == null) return
        val superExpression = element ?: return
        CommandProcessor.getInstance().runUndoTransparentAction {
            if (superTypes.size == 1) {
                superExpression.specifySuperType(superTypes.first())
            } else {
                JBPopupFactory
                    .getInstance()
                    .createListPopup(createListPopupStep(superExpression, superTypes))
                    .showInBestPositionFor(editor)
            }
        }
    }

    private fun KtSuperExpression.specifySuperType(superType: String) {
        project.executeWriteCommand("Specify supertype") {
            replace(KtPsiFactory(this).createExpression("super<$superType>"))
        }
    }

    private fun createListPopupStep(superExpression: KtSuperExpression, superTypes: List<String>): ListPopupStep<*> {
        return object : BaseListPopupStep<String>("Choose supertype", superTypes) {
            override fun isAutoSelectionEnabled() = false

            override fun onChosen(selectedValue: String, finalChoice: Boolean): PopupStep<*>? {
                if (finalChoice) {
                    superExpression.specifySuperType(selectedValue)
                }
                return PopupStep.FINAL_CHOICE
            }
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtSuperExpression>? {
            val superExpression = diagnostic.psiElement as? KtSuperExpression ?: return null
            val qualifiedExpression = superExpression.getQualifiedExpressionForReceiver() ?: return null
            val selectorExpression = qualifiedExpression.selectorExpression ?: return null

            val containingClassOrObject = superExpression.getStrictParentOfType<KtClassOrObject>() ?: return null
            val allSuperTypes = containingClassOrObject.superTypeListEntries.mapNotNull { it.typeReference?.text }
            if (allSuperTypes.isEmpty()) return null

            val context = superExpression.analyze(BodyResolveMode.PARTIAL)
            val psiFactory = KtPsiFactory(superExpression)
            val superTypesForSuperExpression = allSuperTypes.filter {
                val newQualifiedExpression = psiFactory.createExpressionByPattern("super<$it>.$0", selectorExpression)
                val newContext = newQualifiedExpression.analyzeAsReplacement(qualifiedExpression, context)
                newQualifiedExpression.getResolvedCall(newContext)?.resultingDescriptor != null
            }
            if (superTypesForSuperExpression.isEmpty()) return null

            return SpecifySuperTypeFix(superExpression, superTypesForSuperExpression)
        }
    }
}
