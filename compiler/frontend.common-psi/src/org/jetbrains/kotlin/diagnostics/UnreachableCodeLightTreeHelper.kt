/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.util.getChildren

typealias Node = LighterASTNode

class UnreachableCodeLightTreeHelper(val tree: FlyweightCapableTreeStructure<Node>) {

    fun Node.hasChildrenInSet(set: Set<Node>): Boolean {
        var result = false
        tree.traverseDescendants(this) {
            if (!result && it != this && it in set) {
                result = true
            }
            !result
        }
        return result
    }

    fun Node.getLeavesOrReachableChildren(reachable: Set<Node>, unreachable: Set<Node>): List<Node> {
        val result = mutableListOf<Node>()
        tree.traverseDescendants(this) { element ->
            val isReachable = element in reachable && !element.hasChildrenInSet(unreachable)
            if (isReachable || element.getChildren(tree).isEmpty()) {
                result.add(element)
                false
            } else {
                true
            }
        }
        return result
    }

    fun List<Node>.removeReachableElementsWithMeaninglessSiblings(reachableElements: Set<Node>): List<Node> {
        val childrenToRemove = mutableSetOf<Node>()
        fun collectSiblingsIfMeaningless(elementIndex: Int, direction: Int) {
            val index = elementIndex + direction
            if (index !in 0 until size) return

            val element = this[index]
            if (element.isFiller() || element.tokenType == KtTokens.COMMA) {
                childrenToRemove.add(element)
                collectSiblingsIfMeaningless(index, direction)
            }
        }
        for ((index, element) in this.withIndex()) {
            if (reachableElements.contains(element)) {
                childrenToRemove.add(element)
                collectSiblingsIfMeaningless(index, -1)
                collectSiblingsIfMeaningless(index, 1)
            }
        }
        return filter { it !in childrenToRemove }
    }

    fun List<TextRange>.mergeAdjacentTextRanges(): List<TextRange> {
        val result = ArrayList<TextRange>()
        val lastRange = fold(null as TextRange?) { currentTextRange, elementRange ->
            when {
                currentTextRange == null -> {
                    elementRange
                }
                currentTextRange.endOffset == elementRange.startOffset -> {
                    currentTextRange.union(elementRange)
                }
                else -> {
                    result.add(currentTextRange)
                    elementRange
                }
            }
        }
        if (lastRange != null) {
            result.add(lastRange)
        }
        return result
    }
}
