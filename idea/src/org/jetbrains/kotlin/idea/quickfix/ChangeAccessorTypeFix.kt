/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
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
    private fun getType(): KotlinType? = element!!.property.resolveToDescriptorIfAny()?.type?.takeUnless(KotlinType::isError)

    override fun isAvailable(project: Project, editor: Editor?, file: KtFile) = getType() != null

    override fun getFamilyName() = "Change accessor type"

    override fun getText(): String {
        val element = element ?: return ""
        val type = getType() ?: return familyName
        val renderedType = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
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
