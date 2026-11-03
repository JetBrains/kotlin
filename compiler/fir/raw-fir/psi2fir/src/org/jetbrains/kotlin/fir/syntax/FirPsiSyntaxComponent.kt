/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.syntax

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.config.ParserMode
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType
import org.jetbrains.kotlin.psi.psiUtil.getExplicitReceiverOfDotQualified
import org.jetbrains.kotlin.syntax.NO_ID
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.syntaxId
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

@NoMutableState
object FirPsiSyntaxComponent : FirSyntaxComponent<KtPsiSourceElement>() {
    override val parserMode: ParserMode
        get() = Psi

    override fun KtSourceElement.unwrap(): KtPsiSourceElement {
        return this as? KtPsiSourceElement ?: unwrapError(this@unwrap)
    }

    @OptIn(KtImplementationDetail::class)
    override fun KtSourceElement.forEachChildMatching(
        depth: Int,
        reverse: Boolean,
        isTargetType: (SyntaxId) -> Boolean,
        processChild: (KtSourceElement) -> Boolean,
    ) {
        forEachChildMatching(
            unwrap().psi, depth, reverse, isTargetType,
            getSyntaxId = { it.elementType?.syntaxId() ?: NO_ID },
            getChildren = { it.allChildren.toList() },
        ) {
            processChild(it.toKtPsiSourceElement())
        }
    }

    override fun KtSourceElement.findModifierList(): FirModifierList? {
        val source = unwrap()
        // todo this code is buggy. psi for fake declarations (e.g. ImplicitConstructor, EnumGeneratedDeclaration) means a completely different thing
        //  KT-63751
        val modifierListOwner = source.psi as? KtModifierListOwner

        // The check is required in the Analysis API mode as in this case property accessor
        // has the containing property as a source
        if (source.kind is KtFakeSourceElementKind.DelegatedPropertyAccessor && modifierListOwner is KtProperty) {
            errorWithAttachment("Don't request modifiers on fake PSI of delegated property accessors, it's not the right PSI") {
                withPsiEntry("property", modifierListOwner)
            }
        }

        val modifierList = modifierListOwner?.modifierList ?: return null
        val modifiers = mutableListOf<FirModifier>()
        val annotationSources = mutableListOf<KtSourceElement>()
        for (node in modifierList.node.children()) {
            when (val elementType = node.elementType) {
                in MODIFIER_KEYWORD_SET -> {
                    val token = elementType as KtModifierKeywordToken
                    modifiers += FirModifier(token.tokenId, token.value, node.psi.toKtPsiSourceElement())
                }
                KtNodeTypes.ANNOTATION_ENTRY -> annotationSources += KtRealPsiSourceElement(node.psi)
            }
        }
        return FirModifierList(modifiers, annotationSources)
    }

    private val MODIFIER_KEYWORD_SET = TokenSet.orSet(KtTokens.SOFT_KEYWORDS, TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.FUN_KEYWORD))

    override fun KtSourceElement.findContextParameterListSources(): List<KtSourceElement> {
        return unwrap().psi.getChildOfType<KtModifierList>()?.contextParameterLists?.map { it.toKtPsiSourceElement() }.orEmpty()
    }

    override fun KtSourceElement.findFunctionTypeContextParameterListSource(): KtSourceElement? {
        fun PsiElement.findContextParameterList(): KtContextParameterList? {
            return when (this) {
                is KtTypeReference -> typeElement?.findContextParameterList()
                is KtNullableType -> innerType?.findContextParameterList()
                is KtFunctionType -> contextParameterList
                else -> null
            }
        }

        return unwrap().psi.findContextParameterList()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findDefaultValueSource(): KtSourceElement? {
        return (unwrap().psi as? KtParameter)?.defaultValue?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findValOrVarKeywordSource(): KtSourceElement? {
        return (unwrap().psi as? KtValVarKeywordOwner)?.valOrVarKeyword?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findExplicitReceiverOfDotQualifiedSource(): KtSourceElement? {
        return unwrap().psi.getExplicitReceiverOfDotQualified()?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.findParameterNameSource(name: Name): KtSourceElement? {
        return unwrap().psi.findDescendantOfType<KtParameter> { it.nameAsName == name }?.nameIdentifier?.toKtPsiSourceElement()
    }

    override fun KtSourceElement.getContainingStringTemplateEntriesCount(): Int? {
        var parent = unwrap().psi.parent
        while (parent != null) {
            if (parent is KtStringTemplateExpression) return parent.children.size
            parent = parent.parent
        }
        return null
    }

    override fun KtSourceElement.isTrailingLambdaOnNewLine(): Boolean {
        return (unwrap().psi as? KtLambdaExpression)?.isTrailingLambdaOnNewLine == true
    }
}
