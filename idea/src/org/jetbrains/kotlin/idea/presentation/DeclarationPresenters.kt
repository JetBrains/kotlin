/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.presentation

import com.intellij.ide.IconProvider
import com.intellij.navigation.ColoredItemPresentation
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProvider
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.util.Iconable
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.KotlinIconProviderBase
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import javax.swing.Icon

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
            return KotlinBundle.message("presentation.text.in.container.paren", containerName.toString())
        }

        val name = declaration.fqName
        val parent = declaration.parent
        val containerText = if (name != null) {
            val qualifiedContainer = name.parent().toString()
            if (parent is KtFile && declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)) {
                KotlinBundle.message("presentation.text.in.container", parent.name, qualifiedContainer)
            } else {
                qualifiedContainer
            }
        } else {
            getNameForContainingObjectLiteral() ?: return null
        }

        val receiverTypeRef = (declaration as? KtCallableDeclaration)?.receiverTypeReference
        return when {
            receiverTypeRef != null -> {
                KotlinBundle.message("presentation.text.for.receiver.in.container.paren", receiverTypeRef.text, containerText)
            }
            parent is KtFile -> KotlinBundle.message("presentation.text.paren", containerText)
            else -> KotlinBundle.message("presentation.text.in.container.paren", containerText)
        }
    }

    private fun getNameForContainingObjectLiteral(): String? {
        val objectLiteral = declaration.getStrictParentOfType<KtObjectLiteralExpression>() ?: return null
        val container = objectLiteral.getStrictParentOfType<KtNamedDeclaration>() ?: return null
        val containerFqName = container.fqName?.asString() ?: return null
        return KotlinBundle.message("presentation.text.object.in.container", containerFqName)
    }

    override fun getIcon(unused: Boolean): Icon? {
        val instance = IconProvider.EXTENSION_POINT_NAME.findFirstSafe { it is KotlinIconProviderBase }
        return instance?.getIcon(declaration, Iconable.ICON_FLAG_VISIBILITY or Iconable.ICON_FLAG_READ_STATUS)
    }
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
                    return KotlinBundle.message("presentation.text.in.container.paren", name)
                }

                return super.getLocationString()
            }
        }
    }
}
