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

package org.jetbrains.kotlin.idea.presentation

import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.ItemPresentationProvider
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.Iconable
import org.jetbrains.kotlin.idea.KotlinIconProvider
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPsiUtil

open class KotlinDefaultNamedDeclarationPresentation(private val declaration: KtNamedDeclaration) : ColoredItemPresentation {

    override fun getTextAttributesKey(): TextAttributesKey? {
        if (KtPsiUtil.isDeprecated(declaration)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES
        }
        return null
    }

    override fun getPresentableText() = declaration.name

    override fun getLocationString(): String? {
        val name = declaration.fqName ?: return null
        val receiverTypeRef = (declaration as? KtCallableDeclaration)?.receiverTypeReference
        if (receiverTypeRef != null) {
            return "(for " + receiverTypeRef.text + " in " + name.parent() + ")"
        }
        else if (declaration.parent is KtFile) {
            return "(" + name.parent() + ")"
        }
        else {
            return "(in " + name.parent() + ")"
        }
    }

    override fun getIcon(unused: Boolean)
            = KotlinIconProvider.INSTANCE.getIcon(declaration, Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS)
}

class KtDefaultDeclarationPresenter : ItemPresentationProvider<KtNamedDeclaration> {
    override fun getPresentation(item: KtNamedDeclaration) = KotlinDefaultNamedDeclarationPresentation(item)
}
