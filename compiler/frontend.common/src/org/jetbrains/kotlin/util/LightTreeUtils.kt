/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.util.diff.FlyweightCapableTreeStructure

fun LighterASTNode.getChildren(tree: FlyweightCapableTreeStructure<LighterASTNode>): List<LighterASTNode> {
    val children = Ref<Array<LighterASTNode?>>()
    val count = tree.getChildren(this, children)
    @Suppress("UNCHECKED_CAST")
    return if (count > 0) children.get().take(count) as List<LighterASTNode> else emptyList()
}

fun LighterASTNode.getSingleChildOrNull(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
    val children = Ref<Array<LighterASTNode?>>(arrayOf(null))
    val count = tree.getChildren(this, children)
    return if (count == 1) children.get()[0] else null
}

fun LighterASTNode.getPreviousSibling(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode? {
    val parent = tree.getParent(this) ?: return null
    val children = parent.getChildren(tree)
    val index = children.indexOf(this)
    return children.elementAtOrNull(index - 1)
}