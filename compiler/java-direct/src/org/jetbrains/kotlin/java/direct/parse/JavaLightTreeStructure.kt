/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.java.direct.parse

import com.intellij.lang.Language
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

/**
 * Single shared placeholder [IElementType] used by [JavaLightAstNode.getTokenType].
 *
 * java-direct source elements only need correct offsets and text (see the source-element design);
 * faithful `SyntaxElementType` -> `IElementType` mapping is intentionally out of scope. The `register = false`
 * (non-registering) [IElementType] constructor is `protected`, so it is reached via a private subclass; this
 * keeps the placeholder out of the global [IElementType] registry.
 */
private object JavaDirectPlaceholderElementType : IElementType("JAVA_DIRECT_PLACEHOLDER", Language.ANY, false)

val JAVA_DIRECT_PLACEHOLDER_TYPE: IElementType = JavaDirectPlaceholderElementType

/**
 * Exposes a [JavaLightNode] of a [JavaLightTree] through the [LighterASTNode] interface.
 *
 * Only [getStartOffset]/[getEndOffset] and (via [JavaLightTreeStructure]) the node text are
 * meaningful; [getTokenType] returns the shared [JAVA_DIRECT_PLACEHOLDER_TYPE].
 */
class JavaLightAstNode(
    val tree: JavaLightTree,
    val node: JavaLightNode,
) : LighterASTNode {
    override fun getTokenType(): IElementType = JAVA_DIRECT_PLACEHOLDER_TYPE

    override fun getStartOffset(): Int = tree.getStartOffset(node)

    override fun getEndOffset(): Int = tree.getEndOffset(node)

    // mirrors [org.jetbrains.kotlin.java.direct.model.JavaElementOverAst]
    // semantics, so that distinct java-direct nodes yield distinct source elements.
    override fun equals(other: Any?): Boolean =
        other is JavaLightAstNode && node == other.node && tree === other.tree

    override fun hashCode(): Int = node.hashCode()
}

/**
 * Adapts a whole [JavaLightTree] to [FlyweightCapableTreeStructure] over [LighterASTNode], delegating
 * navigation/offsets/text to the underlying tree. One instance per [JavaLightTree] is memoized via
 * [JavaLightTree.lightSourceTreeStructure] so every source element from the same file shares a single
 * [tree] (required for sane [org.jetbrains.kotlin.KtLightSourceElement] equality/identity).
 */
class JavaLightTreeStructure(val tree: JavaLightTree) : FlyweightCapableTreeStructure<LighterASTNode> {
    private fun unwrap(node: LighterASTNode): JavaLightNode = (node as JavaLightAstNode).node

    private fun wrap(node: JavaLightNode): LighterASTNode = JavaLightAstNode(tree, node)

    override fun getRoot(): LighterASTNode = wrap(tree.getRoot())

    override fun getParent(node: LighterASTNode): LighterASTNode? =
        tree.getParent(unwrap(node))?.let(::wrap)

    override fun getChildren(parent: LighterASTNode, into: Ref<Array<LighterASTNode>>): Int {
        val children = tree.getChildren(unwrap(parent))
        if (children.isEmpty()) {
            into.set(LighterASTNode.EMPTY_ARRAY)
            return 0
        }
        into.set(Array(children.size) { wrap(children[it]) })
        return children.size
    }

    override fun disposeChildren(nodes: Array<out LighterASTNode>?, count: Int) {
    }

    override fun toString(node: LighterASTNode): CharSequence = tree.getText(unwrap(node))

    override fun getStartOffset(node: LighterASTNode): Int = tree.getStartOffset(unwrap(node))

    override fun getEndOffset(node: LighterASTNode): Int = tree.getEndOffset(unwrap(node))
}
