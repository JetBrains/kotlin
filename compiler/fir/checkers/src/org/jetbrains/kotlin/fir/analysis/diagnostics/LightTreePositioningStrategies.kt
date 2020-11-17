/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtParameter.VAL_VAR_TOKEN_SET

object LightTreePositioningStrategies {
    internal val DEFAULT = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.OBJECT_DECLARATION -> {
                    val objectKeyword = tree.findChildByType(node, KtTokens.OBJECT_KEYWORD)!!
                    return markRange(
                        from = objectKeyword,
                        to = tree.findChildByType(node, KtTokens.IDENTIFIER) ?: objectKeyword,
                        tree
                    )
                }
                KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                    return SECONDARY_CONSTRUCTOR_DELEGATION_CALL.mark(node, tree)
                }
            }
            return super.mark(node, tree)
        }
    }

    val VAL_OR_VAR_NODE: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            val target = tree.findChildByType(node, VAL_VAR_TOKEN_SET) ?: node
            return markElement(target, tree)
        }
    }

    val SECONDARY_CONSTRUCTOR_DELEGATION_CALL: LightTreePositioningStrategy = object : LightTreePositioningStrategy() {
        override fun mark(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): List<TextRange> {
            when (node.tokenType) {
                KtNodeTypes.SECONDARY_CONSTRUCTOR -> {
                    val valueParameterList = tree.findChildByType(node, KtNodeTypes.VALUE_PARAMETER_LIST) ?: return markElement(node, tree)
                    return markRange(
                        tree.findChildByType(node, KtTokens.CONSTRUCTOR_KEYWORD)!!,
                        tree.lastChild(valueParameterList)!!, tree
                    )
                }
                KtNodeTypes.CONSTRUCTOR_DELEGATION_CALL -> {
                    val delegationReference = tree.findChildByType(node, KtNodeTypes.CONSTRUCTOR_DELEGATION_REFERENCE)
                    if (delegationReference != null && tree.firstChild(delegationReference) == null) {
                        val constructor = tree.findParentOfType(node, KtNodeTypes.SECONDARY_CONSTRUCTOR)!!
                        val valueParameterList = tree.findChildByType(constructor, KtNodeTypes.VALUE_PARAMETER_LIST)
                            ?: return markElement(constructor, tree)
                        return markRange(
                            tree.findChildByType(constructor, KtTokens.CONSTRUCTOR_KEYWORD)!!,
                            tree.lastChild(valueParameterList)!!, tree
                        )
                    }
                    return markElement(delegationReference ?: node, tree)
                }
                else -> error("unexpected element $node")
            }
        }
    }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it.tokenType == type }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: TokenSet): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it.tokenType in type }
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.findParentOfType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    var parent = getParent(node)
    while (parent != null) {
        if (parent.tokenType == type) return parent
        parent = getParent(parent)
    }
    return null
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.firstChild(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull()
}

private fun FlyweightCapableTreeStructure<LighterASTNode>.lastChild(node: LighterASTNode): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.lastOrNull()
}
