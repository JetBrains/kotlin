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
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProvider
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.Iconable
import org.jetbrains.kotlin.idea.KotlinIconProvider
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType

open class KotlinDefaultNamedDeclarationPresentation(private val declaration: KtNamedDeclaration) : ColoredItemPresentation {

    override fun getTextAttributesKey(): TextAttributesKey? {
        if (KtPsiUtil.isDeprecated(declaration)) {
            return CodeInsightColors.DEPRECATED_ATTRIBUTES
        }
        return null
    }

    override fun getPresentableText() = declaration.name

    override fun getLocationString(): String? {
        if ((declaration is KtFunction && declaration.isLocal) || (declaration is KtClassOrObject && declaration.isLocal)) {
            val containingDeclaration = declaration.getStrictParentOfType<KtNamedDeclaration>() ?: return null
            val containerName = containingDeclaration.fqName ?: containingDeclaration.name
            return "(in $containerName)"
        }
        val name = declaration.fqName ?: return null
        val qualifiedContainer = name.parent().toString()
        val parent = declaration.parent
        val containerText = if (parent is KtFile && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
            "${parent.name} in $qualifiedContainer"
        }
        else {
            qualifiedContainer
        }
        val receiverTypeRef = (declaration as? KtCallableDeclaration)?.receiverTypeReference
        return when {
            receiverTypeRef != null -> "(for " + receiverTypeRef.text + " in " + containerText + ")"
            parent is KtFile -> "($containerText)"
            else -> "(in $containerText)"
        }
    }

    override fun getIcon(unused: Boolean)
            = KotlinIconProvider.INSTANCE.getIcon(declaration, Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS)
}

class KtDefaultDeclarationPresenter : ItemPresentationProvider<KtNamedDeclaration> {
    override fun getPresentation(item: KtNamedDeclaration) = KotlinDefaultNamedDeclarationPresentation(item)
}

class KtFunctionPresenter : ItemPresentationProvider<KtFunction> {
    override fun getPresentation(function: KtFunction): ItemPresentation? {
        if (function is KtFunctionLiteral) return null

        return object : KotlinDefaultNamedDeclarationPresentation(function) {
            override fun getPresentableText(): String {
                return buildString {
                    function.name?.let { append(it) }

                    append("(")
                    append(function.valueParameters.joinToString {
                        (if (it.isVarArg) "vararg " else "") + (it.typeReference?.text ?: "")
                    })
                    append(")")
                }
            }

            override fun getLocationString(): String? {
                if (function is KtConstructor<*>) {
                    val name = function.getContainingClassOrObject().fqName ?: return null
                    return "(in $name)"
                }

                return super.getLocationString()
            }
        }
    }
}
