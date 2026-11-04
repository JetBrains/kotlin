/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * This is a temporary shim to simplify migration to platform-independent parse trees.
 * Later it should be relocated to a corresponding package with platform dependencies
 * And the functions should be called via a syntax component.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtNodeTypes.ANNOTATION_ENTRY
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.SyntaxId
import org.jetbrains.kotlin.diagnostics.valOrVarKeyword
import org.jetbrains.kotlin.elementType
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValVarKeywordOwner
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.toKtLightSourceElement
import org.jetbrains.kotlin.toKtPsiSourceElement
import org.jetbrains.kotlin.util.getChildren
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import org.jetbrains.kotlin.utils.exceptions.withPsiEntry

class FirPsiModifierList(val modifierList: KtModifierList) : FirModifierList() {
    companion object {
        private val MODIFIER_KEYWORD_SET = TokenSet.orSet(KtTokens.SOFT_KEYWORDS, TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.FUN_KEYWORD))
    }

    override val modifiers: List<FirPsiModifier>
        get() = modifierList.node.getChildren(MODIFIER_KEYWORD_SET).map { node ->
            FirPsiModifier(node, (node.elementType as KtModifierKeywordToken).tokenId)
        }

    override fun getAnnotationSources(): List<KtSourceElement> {
        val children = modifierList.node.children()
        return children.filter { it.elementType == ANNOTATION_ENTRY }.map { KtRealPsiSourceElement(it.psi) }.toList()
    }
}

class FirLightModifierList(
    val modifierList: LighterASTNode,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val offsetDelta: Int
) : FirModifierList() {
    override val modifiers: List<FirLightModifier>
        get() {
            val modifierNodes = modifierList.getChildren(tree)
            return modifierNodes
                .filter { it.tokenType is KtModifierKeywordToken }
                .map { FirLightModifier(it, (it.tokenType as KtModifierKeywordToken).tokenId, tree, offsetDelta) }
        }

    override fun getAnnotationSources(): List<KtSourceElement> {
        val children = modifierList.getChildren(tree)
        return children.filter { it.tokenType == ANNOTATION_ENTRY }.map { it.toKtLightSourceElement(tree) }
    }
}

class FirPsiModifier(
    val node: ASTNode,
    tokenId: SyntaxId,
) : FirModifier(tokenId) {
    @OptIn(KtImplementationDetail::class)
    override val tokenName: String
        get() = tokenId.elementType().toString()

    override val source: KtSourceElement
        get() = node.psi.toKtPsiSourceElement()
}

class FirLightModifier(
    val node: LighterASTNode,
    tokenId: SyntaxId,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private val offsetDelta: Int
) : FirModifier(tokenId) {
    @OptIn(KtImplementationDetail::class)
    override val tokenName: String
        get() = tokenId.elementType().toString()

    override val source: KtSourceElement
        get() = node.toKtLightSourceElement(
            tree,
            startOffset = node.startOffset + offsetDelta,
            endOffset = node.endOffset + offsetDelta
        )
}

fun KtSourceElement?.getModifierList(): FirModifierList? {
    return when (this) {
        null -> null
        // todo this code is buggy. psi for fake declarations (e.g. ImplicitConstructor, EnumGeneratedDeclaration) means a completely different thing
        //  KT-63751
        is KtPsiSourceElement -> {
            val modifierListOwner = psi as? KtModifierListOwner

            // The check is required in the Analysis API mode as in this case property accessor
            // has the containing property as a source
            if (kind is KtFakeSourceElementKind.DelegatedPropertyAccessor && modifierListOwner is KtProperty) {
                errorWithAttachment("Don't request modifiers on fake PSI of delegated property accessors, it's not the right PSI") {
                    withPsiEntry("property", modifierListOwner)
                }
            }

            modifierListOwner?.modifierList?.let { FirPsiModifierList(it) }
        }
        is KtLightSourceElement -> {
            val modifierListNode = lighterASTNode.getChildren(treeStructure).find { it.tokenType == KtNodeTypes.MODIFIER_LIST }
                ?: return null
            val offsetDelta = startOffset - lighterASTNode.startOffset
            FirLightModifierList(modifierListNode, treeStructure, offsetDelta)
        }
    }
}

internal val KtSourceElement?.valOrVarKeyword: ValOrVarKeyword?
    get() {
        val keywordToken = when (this) {
            null -> null
            is KtPsiSourceElement -> (psi as? KtValVarKeywordOwner)?.valOrVarKeyword?.let { it.node?.elementType as? KtKeywordToken }
            is KtLightSourceElement -> treeStructure.valOrVarKeyword(lighterASTNode)?.tokenType as? KtKeywordToken
        }
        return when (keywordToken) {
            KtTokens.VAL_KEYWORD -> ValOrVarKeyword.VAL
            KtTokens.VAR_KEYWORD -> ValOrVarKeyword.VAR
            else -> null
        }
    }

