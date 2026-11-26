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
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtNodeTypes.PREFIX_EXPRESSION
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtImplementationDetail
import org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES
import org.jetbrains.kotlin.util.getChildren

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

/**
 * This function should only be called for a source element corresponding to
 * an assignment/assignment operator call/increment or a decrement operator.
 */
fun LighterASTNode.getAssignmentLhsIfUnwrappable(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? =
    when {
        // In `++(x)` the LHS source `(x)` is the last child
        tokenType == PREFIX_EXPRESSION -> getChildren(tree).lastOrNull()
        // In `(x)++` or `(x) = ...` the LHS source is the first child
        else -> getChildren(tree).firstOrNull()
    }.takeIf {
        it?.tokenType in @OptIn(KtImplementationDetail::class) UNWRAPPABLE_TOKEN_TYPES
    }

/**
 * Returns the direct children of [node], or an empty array if it has none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenArray(node: LighterASTNode): Array<LighterASTNode?> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get() ?: emptyArray()
}

/**
 * Returns the modifier list among the direct children of [node], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.modifierList(node: LighterASTNode): LighterASTNode? =
    findChildByType(node, KtNodeTypes.MODIFIER_LIST)

/**
 * Returns the annotation entries in the modifier list of the type reference of [node],
 * or `null` if [node] has no type reference or the type reference has no modifier list.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.annotations(node: LighterASTNode): List<LighterASTNode>? {
    val typeReference = findChildByType(node, KtNodeTypes.TYPE_REFERENCE) ?: return null
    val modifiers = modifierList(typeReference) ?: return null
    return collectDescendantsOfType(modifiers, KtNodeTypes.ANNOTATION_ENTRY)
}

/**
 * Returns the user type of the type reference of [node], or `null` if there is none.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.userType(node: LighterASTNode): LighterASTNode? {
    val typeReference = findChildByType(node, KtNodeTypes.TYPE_REFERENCE) ?: return null
    return findChildByType(typeReference, KtNodeTypes.USER_TYPE)
}

/**
 * Returns all descendants of [node] with the given [type] that satisfy [predicate], in pre-order.
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.collectDescendantsOfType(
    node: LighterASTNode, type: IElementType,
    predicate: (LighterASTNode) -> Boolean = { true }
): List<LighterASTNode> {
    val result = mutableListOf<LighterASTNode>()

    fun FlyweightCapableTreeStructure<LighterASTNode>.collectDescendantByType(node: LighterASTNode) {
        val childrenRef = Ref<Array<LighterASTNode?>>()
        getChildren(node, childrenRef)

        val childrenRefGet = childrenRef.get()
        if (childrenRefGet != null) {
            for (child in childrenRefGet) {
                if (child?.tokenType == type && predicate(child)) {
                    result.add(child)
                }

                if (child != null) {
                    collectDescendantByType(child)
                }
            }
        }
    }

    collectDescendantByType(node)

    return result
}
