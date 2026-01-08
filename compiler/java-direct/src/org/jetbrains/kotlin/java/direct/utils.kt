/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder

import com.intellij.platform.syntax.parser.prepareProduction

class DirectSyntaxNode(
    val production: SyntaxTreeBuilder.Production,
    val children: List<DirectSyntaxNode>,
    val source: CharSequence
) {
    val type: SyntaxElementType get() = production.getNodeType()
    val text: String get() = source.subSequence(production.getStartOffset(), production.getEndOffset()).toString()
}

fun buildDirectSyntaxTree(builder: SyntaxTreeBuilder, source: CharSequence): DirectSyntaxNode {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val childrenStack = mutableListOf<MutableList<DirectSyntaxNode>>()
    childrenStack.add(mutableListOf())

    for (i in 0 until productionMarkers.size) {
        val production = productionMarkers.getMarker(i)
        if (productionMarkers.isDoneMarker(i)) {
            val children = childrenStack.removeAt(childrenStack.size - 1)
            val node = DirectSyntaxNode(production, children, source)
            if (childrenStack.isEmpty()) {
                childrenStack.add(mutableListOf(node))
                break
            }
            childrenStack.last().add(node)
        } else {
            childrenStack.add(mutableListOf())
        }
    }

    return childrenStack.single().single()
}

fun DirectSyntaxNode.findChildByType(type: SyntaxElementType): DirectSyntaxNode? {
    return children.find { it.type == type }
}

fun DirectSyntaxNode.getChildrenByType(type: SyntaxElementType): List<DirectSyntaxNode> {
    return children.filter { it.type == type }
}
