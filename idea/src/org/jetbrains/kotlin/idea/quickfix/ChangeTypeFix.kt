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

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.core.quickfix.QuickFixUtil
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.core.ShortenReferences
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
