/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticWithParameters3
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.intentions.SamConversionToAnonymousObjectIntention
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.calls.tower.WrongResolutionToClassifier
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

class ConvertToAnonymousObjectFix(element: KtNameReferenceExpression) : KotlinQuickFixAction<KtNameReferenceExpression>(element) {
    override fun getFamilyName() = "Convert to anonymous object"

    override fun getText() = familyName

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val nameReference = element ?: return
        val call = nameReference.parent as? KtCallExpression ?: return
        val functionDescriptor = nameReference.analyze().diagnostics.forElement(nameReference).firstNotNullResult {
            if (it.factory == Errors.RESOLUTION_TO_CLASSIFIER) getFunctionDescriptor(Errors.RESOLUTION_TO_CLASSIFIER.cast(it)) else null
        } ?: return
        val functionName = functionDescriptor.name.asString()
        SamConversionToAnonymousObjectIntention.convertToAnonymousObject(call, functionDescriptor, functionName)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtNameReferenceExpression>? {
            val casted = Errors.RESOLUTION_TO_CLASSIFIER.cast(diagnostic)
            if (casted.b != WrongResolutionToClassifier.INTERFACE_AS_FUNCTION) return null
            val nameReference = casted.psiElement as? KtNameReferenceExpression ?: return null
            if (nameReference.parent as? KtCallExpression == null) return null
            if (getFunctionDescriptor(casted) == null) return null
            return ConvertToAnonymousObjectFix(nameReference)
        }

        private fun getFunctionDescriptor(
            d: DiagnosticWithParameters3<KtReferenceExpression, ClassifierDescriptor, WrongResolutionToClassifier, String>
        ) = (d.a as? LazyClassDescriptor)?.declaredCallableMembers?.singleOrNull() as? SimpleFunctionDescriptor
    }
}