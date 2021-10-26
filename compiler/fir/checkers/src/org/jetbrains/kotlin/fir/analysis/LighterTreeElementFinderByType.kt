/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure

class LighterTreeElementFinderByType(
    private val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    private var types: Collection<IElementType>,
    private var index: Int,
    private val depth: Int,
    private val reverse: Boolean,
) {
    fun find(node: LighterASTNode?): LighterASTNode? {
        if (node == null) return null
        return visitNode(node, 0)
    }

    private fun visitNode(node: LighterASTNode, currentDepth: Int): LighterASTNode? {
        if (currentDepth != 0) {
            if (node.tokenType in types) {
                if (index == 0) {
                    return node
                }
                index--
            }
        }

        if (currentDepth == depth) return null

        val children = if (reverse) node.getChildren().asReversed() else node.getChildren()
        for (child in children) {
            val result = visitNode(child, currentDepth + 1)
            if (result != null) return result
        }

        return null
    }

    private fun LighterASTNode.getChildren(): List<LighterASTNode> {
        val ref = Ref<Array<LighterASTNode?>>()
        tree.getChildren(this, ref)
        return ref.get()?.filterNotNull() ?: emptyList()
    }

}
