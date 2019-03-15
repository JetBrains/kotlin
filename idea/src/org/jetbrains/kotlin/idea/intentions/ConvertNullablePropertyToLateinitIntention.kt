/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.setType
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.isInlineClassType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

class ConvertNullablePropertyToLateinitIntention : SelfTargetingIntention<KtProperty>(
    KtProperty::class.java, "Convert to lateinit var"
) {
    override fun isApplicableTo(element: KtProperty, caretOffset: Int): Boolean {
        if (element.hasModifier(KtTokens.LATEINIT_KEYWORD) || element.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return false
        if (!element.isVar) return false
        val languageVersionSettings = element.languageVersionSettings
        if (!languageVersionSettings.supportsFeature(LanguageFeature.LateinitLocalVariables) && element.isLocal) return false
        if (!languageVersionSettings.supportsFeature(LanguageFeature.LateinitTopLevelProperties) && element.isTopLevel) return false
        if (element.getter?.hasBody() != null || element.setter?.hasBody() != null) return false
        if (!element.initializer.isNullExpression()) return false

        val typeReference = element.typeReference
        if (typeReference?.typeElement !is KtNullableType) return false
        val context = element.analyze(BodyResolveMode.PARTIAL)
        val type = context[BindingContext.TYPE, typeReference]?.makeNotNullable() ?: return false
        if (KotlinBuiltIns.isPrimitiveType(type) || type.isInlineClassType() || TypeUtils.isNullableType(type)) return false

        val descriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, element] as? VariableDescriptor ?: return false
        if (descriptor is PropertyDescriptor && context[BindingContext.BACKING_FIELD_REQUIRED, descriptor] == false) return false
        if (descriptor.extensionReceiverParameter != null) return false

        return true
    }

    override fun applyTo(element: KtProperty, editor: Editor?) {
        val typeReference: KtTypeReference = element.typeReference ?: return
        val notNullableType = element.analyze(BodyResolveMode.PARTIAL)[BindingContext.TYPE, typeReference]?.makeNotNullable() ?: return
        element.addModifier(KtTokens.LATEINIT_KEYWORD)
        element.setType(notNullableType)
        element.initializer = null
    }
}
