/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.parameterInfo

import com.intellij.codeInsight.hints.InlayInfo
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.intentions.SpecifyTypeExplicitlyIntention
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.RenderingFormat
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError

//hack to separate type presentation from param info presentation
const val TYPE_INFO_PREFIX = "@TYPE@"
private val typeRenderer = DescriptorRenderer.COMPACT_WITH_SHORT_TYPES.withOptions {
    textFormat = RenderingFormat.PLAIN
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
    val type = SpecifyTypeExplicitlyIntention.getTypeForDeclaration(element)
    return if (!type.isError && isUnclearType(type, element)) {
        val settings = CodeStyleSettingsManager.getInstance(element.project).currentSettings
                .getCustomSettings(KotlinCodeStyleSettings::class.java)

        val declString = buildString {
            append(TYPE_INFO_PREFIX)
            if (settings.SPACE_BEFORE_TYPE_COLON)
                append(" ")
            append(":")
            if (settings.SPACE_AFTER_TYPE_COLON)
                append(" ")
            append(typeRenderer.renderType(type))
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
        if (initializer is KtCallExpression) {
            val bindingContext = element.analyze()
            val resolvedCall = initializer.getResolvedCall(bindingContext)
            val constructorDescriptor = resolvedCall?.candidateDescriptor as? ConstructorDescriptor
            if (constructorDescriptor != null && constructorDescriptor.constructedClass.declaredTypeParameters.isEmpty()) {
                return false
            }
        }
    }
    return true
}