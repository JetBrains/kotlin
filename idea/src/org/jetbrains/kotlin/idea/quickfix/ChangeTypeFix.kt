/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.types.KotlinType

class ChangeTypeFix(element: KtTypeReference, private val type: KotlinType) : KotlinQuickFixAction<KtTypeReference>(element) {
    override fun getFamilyName() = "Change type"

    override fun getText(): String {
        val currentTypeText = element?.text ?: return ""
        return "Change type from '$currentTypeText' to '${QuickFixUtil.renderTypeWithFqNameOnClash(type, currentTypeText)}'"
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val newTypeRef = element.replaced(KtPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type)))
        ShortenReferences.DEFAULT.process(newTypeRef)
    }

    companion object : KotlinSingleIntentionActionFactoryWithDelegate<KtTypeReference, KotlinType>() {
        override fun getElementOfInterest(diagnostic: Diagnostic) =
            Errors.EXPECTED_PARAMETER_TYPE_MISMATCH.cast(diagnostic).psiElement.typeReference

        override fun extractFixData(element: KtTypeReference, diagnostic: Diagnostic) =
            Errors.EXPECTED_PARAMETER_TYPE_MISMATCH.cast(diagnostic).a

        override fun createFix(originalElement: KtTypeReference, data: KotlinType) =
            ChangeTypeFix(originalElement, data)
    }
}
