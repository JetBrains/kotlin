/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.lexer.KtTokens

/**
 * Returns the first direct child of [node] with the given [type], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType == type }
}

/**
 * Returns the first direct child of [node] whose type is in [type], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(node: LighterASTNode, type: TokenSet): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.firstOrNull { it?.tokenType in type }
}

/**
 * Returns all direct children of [node] with the given [type].
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.findChildrenByType(node: LighterASTNode, type: IElementType): List<LighterASTNode> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.filter { it?.tokenType == type }?.filterNotNull().orEmpty()
}

/**
 * Returns the last direct child of [node] with the given [type], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.findLastChildByType(node: LighterASTNode, type: IElementType): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get()?.lastOrNull { it?.tokenType == type }
}

/**
 * Returns the `val` or `var` keyword among the direct children of [node], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.valOrVarKeyword(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.VAL_VAR)

/**
 * Returns the name identifier among the direct children of [node], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.nameIdentifier(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtTokens.IDENTIFIER)
