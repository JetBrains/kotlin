/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.Variance

class RemoveTypeVarianceFix(
    typeParameter: KtTypeParameter,
    private val variance: Variance,
    private val type: String
) : KotlinQuickFixAction<KtTypeParameter>(typeParameter) {

    override fun getText(): String = "Remove '${variance.label}' variance from '$type'"

    override fun getFamilyName(): String = text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val typeParameter = element ?: return
        when (variance) {
            Variance.IN_VARIANCE -> KtTokens.IN_KEYWORD
            Variance.OUT_VARIANCE -> KtTokens.OUT_KEYWORD
            else -> null
        }?.let {
            typeParameter.removeModifier(it)
        }
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): KotlinQuickFixAction<KtTypeParameter>? {
            val typeReference = diagnostic.psiElement.parent as? KtTypeReference ?: return null
            val type = typeReference.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference] ?: return null
            val descriptor = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return null
            val variance = descriptor.variance
            if (variance == Variance.INVARIANT) return null
            val typeParameter = DescriptorToSourceUtils.getSourceFromDescriptor(descriptor) as? KtTypeParameter ?: return null
            return RemoveTypeVarianceFix(typeParameter, variance, IdeDescriptorRenderers.SOURCE_CODE_TYPES.renderType(type))
        }
    }

}