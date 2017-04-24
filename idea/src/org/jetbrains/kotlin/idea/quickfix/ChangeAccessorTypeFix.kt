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
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

class ChangeAccessorTypeFix(element: KtPropertyAccessor) : KotlinQuickFixAction<KtPropertyAccessor>(element) {
    private fun getType(): KotlinType? =
            (element!!.property.resolveToDescriptorIfAny() as? VariableDescriptor)?.type?.takeUnless(KotlinType::isError)

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile) = super.isAvailable(project, editor, file) && getType() != null

    override fun getFamilyName() = "Change accessor type"

    override fun getText(): String {
        val element = element ?: return ""
        val type = getType() ?: return familyName
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(type)
        val target = if (element.isGetter) "getter" else "setter parameter"
        return "Change $target type to $renderedType"
    }

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val type = getType()!!
        val newTypeReference = KtPsiFactory(file).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))

        val typeReference = if (element.isGetter) element.returnTypeReference else element.parameter!!.typeReference

        val insertedTypeRef = typeReference!!.replaced(newTypeReference)
        ShortenReferences.DEFAULT.process(insertedTypeRef)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        public override fun createAction(diagnostic: Diagnostic): ChangeAccessorTypeFix? {
            return diagnostic.psiElement.getNonStrictParentOfType<KtPropertyAccessor>()?.let(::ChangeAccessorTypeFix)
        }
    }
}
