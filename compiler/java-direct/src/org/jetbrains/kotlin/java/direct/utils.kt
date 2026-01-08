/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder

import com.intellij.platform.syntax.parser.prepareProduction

class DirectSyntaxNode(
    val type: SyntaxElementType,
    val children: List<DirectSyntaxNode>,
    val source: CharSequence,
    val startOffset: Int,
    val endOffset: Int,
    var parent: DirectSyntaxNode? = null
) {
    val text: String get() = source.subSequence(startOffset, endOffset).toString()

    fun dump(indent: String = ""): String {
        val sb = StringBuilder()
        sb.append(indent).append(type).append(": ").append(text.replace("\n", "\\n")).append("\n")
        for (child in children) {
            sb.append(child.dump(indent + "  "))
        }
        return sb.toString()
    }
}

fun buildDirectSyntaxTree(builder: SyntaxTreeBuilder, source: CharSequence): DirectSyntaxNode {
    val productionMarkers = prepareProduction(builder).productionMarkers
    val tokens = builder.tokens
    val childrenStack = mutableListOf<MutableList<DirectSyntaxNode>>()
    childrenStack.add(mutableListOf())

    var prevTokenIndex = 0

    fun MutableList<DirectSyntaxNode>.appendTokens(lastTokenIndex: Int) {
        for (i in prevTokenIndex until lastTokenIndex) {
            val tokenType = tokens.getTokenType(i) ?: continue
            val start = tokens.getTokenStart(i)
            val end = tokens.getTokenEnd(i)
            if (start == end) continue
            add(DirectSyntaxNode(tokenType, emptyList(), source, start, end))
        }
        prevTokenIndex = lastTokenIndex
    }

    for (i in 0 until productionMarkers.size) {
        val production = productionMarkers.getMarker(i)
        if (productionMarkers.isDoneMarker(i)) {
            val lastChildren = childrenStack.removeAt(childrenStack.size - 1)
            lastChildren.appendTokens(production.getEndTokenIndex())
            val node = DirectSyntaxNode(production.getNodeType(), lastChildren, source, production.getStartOffset(), production.getEndOffset())
            for (child in lastChildren) {
                child.parent = node
            }
            childrenStack.last().add(node)
        } else {
            childrenStack.peek().appendTokens(production.getStartTokenIndex())
            childrenStack.add(mutableListOf())
        }
    }

    val rootNodeType = productionMarkers.getMarker(productionMarkers.size - 1).getNodeType()
    val rootChildren = childrenStack.single()
    val root = DirectSyntaxNode(rootNodeType, rootChildren, source, 0, source.length)
    for (child in rootChildren) {
        child.parent = root
    }
    return root
}

private fun <T> MutableList<T>.peek(): T = last()

fun DirectSyntaxNode.findChildByType(typeName: String): DirectSyntaxNode? {
    return children.find { it.type.toString() == typeName }
}

fun DirectSyntaxNode.getChildrenByType(typeName: String): List<DirectSyntaxNode> {
    return children.filter { it.type.toString() == typeName }
}

fun DirectSyntaxNode.findChildByType(type: SyntaxElementType): DirectSyntaxNode? {
    return children.find { it.type == type }
}

fun DirectSyntaxNode.getChildrenByType(type: SyntaxElementType): List<DirectSyntaxNode> {
    return children.filter { it.type == type }
}
