/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

class ReconstructTypeInCastOrIsIntention :
    SelfTargetingOffsetIndependentIntention<KtTypeReference>(KtTypeReference::class.java, "Replace by reconstructed type"),
    LowPriorityAction {
    override fun isApplicableTo(element: KtTypeReference): Boolean {
        // Only user types (like Foo) are interesting
        val typeElement = element.typeElement as? KtUserType ?: return false

        // If there are generic arguments already, there's nothing to reconstruct
        if (typeElement.typeArguments.isNotEmpty()) return false

        // We must be on the RHS of as/as?/is/!is or inside an is/!is-condition in when()
        val expression = element.getParentOfType<KtExpression>(true)
        if (expression !is KtBinaryExpressionWithTypeRHS && element.getParentOfType<KtWhenConditionIsPattern>(true) == null) return false

        val type = getReconstructedType(element)
        if (type == null || type.isError) return false

        // No type parameters expected => nothing to reconstruct
        if (type.constructor.parameters.isEmpty()) return false

        val typePresentation = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.renderType(type)
        text = "Replace by '$typePresentation'"

        return true
    }

    override fun applyTo(element: KtTypeReference, editor: Editor?) {
        val type = getReconstructedType(element)!!
        val newType = KtPsiFactory(element).createType(IdeDescriptorRenderers.SOURCE_CODE.renderType(type))
        ShortenReferences.DEFAULT.process(element.replaced(newType))
    }

    private fun getReconstructedType(typeRef: KtTypeReference): KotlinType? {
        return typeRef.analyze().get(BindingContext.TYPE, typeRef)
    }
}
