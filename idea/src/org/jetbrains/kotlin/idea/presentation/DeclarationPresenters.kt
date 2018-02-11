/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
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

        val name = declaration.fqName
        val parent = declaration.parent
        val containerText = if (name != null) {
            val qualifiedContainer = name.parent().toString()
            if (parent is KtFile && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                "${parent.name} in $qualifiedContainer"
            } else {
                qualifiedContainer
            }
        } else {
            getNameForContainingObjectLiteral() ?: return null
        }

        val receiverTypeRef = (declaration as? KtCallableDeclaration)?.receiverTypeReference
        return when {
            receiverTypeRef != null -> "(for " + receiverTypeRef.text + " in " + containerText + ")"
            parent is KtFile -> "($containerText)"
            else -> "(in $containerText)"
        }
    }

    private fun getNameForContainingObjectLiteral(): String? {
        val objectLiteral = declaration.getStrictParentOfType<KtObjectLiteralExpression>() ?: return null
        val container = objectLiteral.getStrictParentOfType<KtNamedDeclaration>() ?: return null
        val containerFqName = container.fqName?.asString() ?: return null
        return "object in $containerFqName"
    }

    override fun getIcon(unused: Boolean) =
        KotlinIconProvider.INSTANCE.getIcon(declaration, Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS)
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
