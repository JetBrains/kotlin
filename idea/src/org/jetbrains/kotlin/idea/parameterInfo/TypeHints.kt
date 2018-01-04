/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.containsError
import org.jetbrains.kotlin.types.typeUtil.immediateSupertypes

//hack to separate type presentation from param info presentation
const val TYPE_INFO_PREFIX = "@TYPE@"
val inlayHintsTypeRenderer = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
    textFormat = RenderingFormat.PLAIN
    renderUnabbreviatedType = false
}

fun providePropertyTypeHint(elem: PsiElement): List<InlayInfo> {
    (elem as? KtCallableDeclaration)?.let { property ->
        property.nameIdentifier?.let { ident ->
            return provideTypeHint(property, ident.endOffset)
        }
    }
    return emptyList()
}

fun provideTypeHint(element: KtCallableDeclaration, offset: Int): List<InlayInfo> {
    var type: KotlinType = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element).unwrap()
    if (type.containsError()) return emptyList()
    val name = type.constructor.declarationDescriptor?.name
    if (name == SpecialNames.NO_NAME_PROVIDED) {
        if (element is KtProperty && element.isLocal) {
            // for local variables, an anonymous object type is not collapsed to its supertype,
            // so showing the supertype will be misleading
            return emptyList()
        }
        type = type.immediateSupertypes().singleOrNull() ?: return emptyList()
    }
    else if (name?.isSpecial == true) {
        return emptyList()
    }

    return if (isUnclearType(type, element)) {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
                .getCustomSettings(KotlinCodeStyleSettings::class.java)

        val declString = buildString {
            append(TYPE_INFO_PREFIX)
            if (settings.SPACE_BEFORE_TYPE_COLON)
                append(" ")
            append(":")
            if (settings.SPACE_AFTER_TYPE_COLON)
                append(" ")
            append(inlayHintsTypeRenderer.renderType(type))
        }
        listOf(InlayInfo(declString, offset))
    }
    else {
        emptyList()
    }
}

private fun isUnclearType(type: KotlinType, element: KtCallableDeclaration): Boolean {
    if (element is KtProperty) {
        val initializer = element.initializer ?: return true
        if (initializer is KtConstantExpression || initializer is KtStringTemplateExpression) return false
        if (initializer is KtUnaryExpression && initializer.baseExpression is KtConstantExpression) return false
        if (initializer is KtCallExpression) {
            val bindingContext = element.analyze()
            val resolvedCall = initializer.getResolvedCall(bindingContext)
            val constructorDescriptor = resolvedCall?.candidateDescriptor as? ConstructorDescriptor
            if (constructorDescriptor != null &&
                (constructorDescriptor.constructedClass.declaredTypeParameters.isEmpty() || initializer.typeArgumentList != null)) {
                return false
            }
        }
    }
    return true
}