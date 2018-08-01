/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClass
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

object RestrictedRetentionForExpressionAnnotationFactory : KotlinIntentionActionsFactory() {

    private val sourceRetention = "${KotlinBuiltIns.FQ_NAMES.annotationRetention.asString()}.${AnnotationRetention.SOURCE.name}"
    private val sourceRetentionAnnotation = "@${KotlinBuiltIns.FQ_NAMES.retention.asString()}($sourceRetention)"

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val annotationEntry = diagnostic.psiElement as? KtAnnotationEntry ?: return emptyList()
        val containingClass = annotationEntry.containingClass() ?: return emptyList()
        val retentionAnnotation = containingClass.annotation(KotlinBuiltIns.FQ_NAMES.retention)
        return listOf(
            RemoveExpressionTargetFix(containingClass),
            if (retentionAnnotation == null) AddSourceRetentionFix(containingClass) else ChangeRetentionToSourceFix(containingClass)
        )
    }

    private fun KtClass.annotation(fqName: FqName): KtAnnotationEntry? {
        return annotationEntries.firstOrNull {
            it.typeReference?.text?.endsWith(fqName.shortName().asString()) == true
                    && analyze()[BindingContext.TYPE, it.typeReference]?.constructor?.declarationDescriptor?.fqNameSafe == fqName
        }
    }

    private class AddSourceRetentionFix(element: KtClass) : KotlinQuickFixAction<KtClass>(element) {
        override fun getText() = "Add SOURCE retention"

        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val element = element ?: return
            val added = element.addAnnotationEntry(KtPsiFactory(element).createAnnotationEntry(sourceRetentionAnnotation))
            ShortenReferences.DEFAULT.process(added)
        }
    }

    private class ChangeRetentionToSourceFix(element: KtClass) : KotlinQuickFixAction<KtClass>(element) {
        override fun getText() = "Change existent retention to SOURCE"

        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            val retentionAnnotation = element?.annotation(KotlinBuiltIns.FQ_NAMES.retention) ?: return
            val psiFactory = KtPsiFactory(retentionAnnotation)
            val added = if (retentionAnnotation.valueArgumentList == null) {
                retentionAnnotation.add(psiFactory.createCallArguments("($sourceRetention)")) as KtValueArgumentList
            } else {
                if (retentionAnnotation.valueArguments.isNotEmpty()) {
                    retentionAnnotation.valueArgumentList?.removeArgument(0)
                }
                retentionAnnotation.valueArgumentList?.addArgument(psiFactory.createArgument(sourceRetention))
            }
            if (added != null) {
                ShortenReferences.DEFAULT.process(added)
            }
        }
    }

    private class RemoveExpressionTargetFix(element: KtClass) : KotlinQuickFixAction<KtClass>(element) {
        override fun getText() = "Remove EXPRESSION target"

        override fun getFamilyName() = text

        override fun invoke(project: Project, editor: Editor?, file: KtFile) {
            element?.annotation(KotlinBuiltIns.FQ_NAMES.target)?.delete()
        }
    }
}