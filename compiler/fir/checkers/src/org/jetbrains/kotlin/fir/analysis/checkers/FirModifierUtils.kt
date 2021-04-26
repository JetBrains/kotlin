/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner

// DO
// - use this to retrieve modifiers on the source and confirm a certain modifier indeed appears
// DON'T
// - don't use this to report an error or warning *on* that specific modifier. Use positioning strategies instead.
internal sealed class FirModifierList {
    abstract val modifiers: List<FirModifier<*>>

    class FirPsiModifierList(val modifierList: KtModifierList) : FirModifierList() {
        override val modifiers: List<FirModifier.FirPsiModifier>
            get() = modifierList.node.getChildren(MODIFIER_KEYWORD_SET).map { node ->
                FirModifier.FirPsiModifier(node, node.elementType as KtModifierKeywordToken)
            }
    }

    class FirLightModifierList(
        val modifierList: LighterASTNode,
        val tree: FlyweightCapableTreeStructure<LighterASTNode>,
        private val offsetDelta: Int
    ) : FirModifierList() {
        override val modifiers: List<FirModifier.FirLightModifier>
            get() {
                val modifierNodes = modifierList.getChildren(tree)
                return modifierNodes.filterNotNull()
                    .filter { it.tokenType is KtModifierKeywordToken }
                    .map { FirModifier.FirLightModifier(it, it.tokenType as KtModifierKeywordToken, tree, offsetDelta) }
            }
    }

    operator fun get(token: KtModifierKeywordToken): FirModifier<*>? = modifiers.firstOrNull { it.token == token }

    operator fun contains(token: KtModifierKeywordToken): Boolean = modifiers.any { it.token == token }
}

private val MODIFIER_KEYWORD_SET = TokenSet.orSet(KtTokens.SOFT_KEYWORDS, TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.FUN_KEYWORD))

internal sealed class FirModifier<Node : Any>(val node: Node, val token: KtModifierKeywordToken) {

    class FirPsiModifier(
        node: ASTNode,
        token: KtModifierKeywordToken
    ) : FirModifier<ASTNode>(node, token) {
        override val source: FirSourceElement
            get() = node.psi.toFirPsiSourceElement()
    }

    class FirLightModifier(
        node: LighterASTNode,
        token: KtModifierKeywordToken,
        val tree: FlyweightCapableTreeStructure<LighterASTNode>,
        private val offsetDelta: Int
    ) : FirModifier<LighterASTNode>(node, token) {
        override val source: FirSourceElement
            get() = node.toFirLightSourceElement(
                tree,
                startOffset = node.startOffset + offsetDelta,
                endOffset = node.endOffset + offsetDelta
            )
    }

    abstract val source: FirSourceElement
}

internal fun FirSourceElement?.getModifierList(): FirModifierList? {
    return when (this) {
        null -> null
        is FirPsiSourceElement<*> -> (psi as? KtModifierListOwner)?.modifierList?.let { FirModifierList.FirPsiModifierList(it) }
        is FirLightSourceElement -> {
            val modifierListNode = lighterASTNode.getChildren(treeStructure).find { it?.tokenType == KtNodeTypes.MODIFIER_LIST }
                ?: return null
            val offsetDelta = startOffset - lighterASTNode.startOffset
            FirModifierList.FirLightModifierList(modifierListNode, treeStructure, offsetDelta)
        }
    }
}

internal operator fun FirModifierList?.contains(token: KtModifierKeywordToken): Boolean = this?.contains(token) == true

internal fun FirElement.getModifier(token: KtModifierKeywordToken): FirModifier<*>? = source.getModifierList()?.get(token)

internal fun FirElement.hasModifier(token: KtModifierKeywordToken): Boolean = token in source.getModifierList()
