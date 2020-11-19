/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtModifierList
import org.jetbrains.kotlin.psi.KtModifierListOwner

sealed class FirModifierList {
    abstract val modifiers: List<FirModifier<*>>
}

fun FirSourceElement?.getModifierList(): FirModifierList? {
    return when (this) {
        null -> null
        is FirPsiSourceElement<*> -> (psi as? KtModifierListOwner)?.modifierList?.let { FirPsiModifierList(it) }
        is FirLightSourceElement -> {
            val kidsRef = Ref<Array<LighterASTNode?>>()
            treeStructure.getChildren(lighterASTNode, kidsRef)
            val modifierListNode = kidsRef.get().find { it?.tokenType == KtNodeTypes.MODIFIER_LIST } ?: return null
            FirLightModifierList(modifierListNode, treeStructure)
        }
    }
}

private val MODIFIER_KEYWORD_SET = TokenSet.orSet(KtTokens.SOFT_KEYWORDS, TokenSet.create(KtTokens.IN_KEYWORD, KtTokens.FUN_KEYWORD))

class FirPsiModifierList(val modifierList: KtModifierList) : FirModifierList() {
    override val modifiers: List<FirPsiModifier>
        get() = modifierList.node.getChildren(MODIFIER_KEYWORD_SET).map { node ->
            FirPsiModifier(node, node.elementType as KtModifierKeywordToken)
        }

}

class FirLightModifierList(val modifierList: LighterASTNode, val tree: FlyweightCapableTreeStructure<LighterASTNode>) : FirModifierList() {
    override val modifiers: List<FirLightModifier>
        get() {
            val kidsRef = Ref<Array<LighterASTNode?>>()
            tree.getChildren(modifierList, kidsRef)
            val modifierNodes = kidsRef.get()
            return modifierNodes.filterNotNull()
                .filter { it.tokenType is KtModifierKeywordToken }
                .map { FirLightModifier(it, it.tokenType as KtModifierKeywordToken, tree) }
        }
}

sealed class FirModifier<Node : Any>(val node: Node, val token: KtModifierKeywordToken)

class FirPsiModifier(
    node: ASTNode,
    token: KtModifierKeywordToken
) : FirModifier<ASTNode>(node, token)

class FirLightModifier(
    node: LighterASTNode,
    token: KtModifierKeywordToken,
    val tree: FlyweightCapableTreeStructure<LighterASTNode>
) : FirModifier<LighterASTNode>(node, token)

val FirModifier<*>.psi: PsiElement? get() = (this as? FirPsiModifier)?.node?.psi

val FirModifier<*>.lightNode: LighterASTNode? get() = (this as? FirLightModifier)?.node

val FirModifier<*>.source: FirSourceElement?
    get() = when (this) {
        is FirPsiModifier -> psi?.toFirPsiSourceElement()
        is FirLightModifier -> node.toFirLightSourceElement(tree)
    }
