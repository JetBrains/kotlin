/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE
import org.jetbrains.kotlin.util.getChildren

open class LightTreePositioningStrategy {
    open fun markKtDiagnostic(element: KtSourceElement, diagnostic: KtDiagnostic): List<TextRange> {
        return mark(element.lighterASTNode, element.startOffset, element.endOffset, element.treeStructure)
    }

    open fun mark(
        node: LighterASTNode,
        startOffset: Int,
        endOffset: Int,
        tree: FlyweightCapableTreeStructure<LighterASTNode>
    ): List<TextRange> {
        return markElement(node, startOffset, endOffset, tree)
    }

    open fun isValid(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
        return !hasSyntaxErrors(node, tree)
    }
}

fun markElement(
    node: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode = node,
): List<TextRange> = markRange(node, node, startOffset, endOffset, tree, originalNode)

fun markRange(
    from: LighterASTNode,
    to: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode
): List<TextRange> {
    return listOf(markSingleElement(from, to, startOffset, endOffset, tree, originalNode))
}

fun markSingleElement(
    from: LighterASTNode,
    to: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode
): TextRange {
    val betterFrom = from.nonFillerFirstChildOrSelf(tree)
    val betterTo = to.nonFillerLastChildOrSelf(tree)
    val startDelta = tree.getStartOffset(betterFrom) - tree.getStartOffset(originalNode)
    val endDelta = tree.getEndOffset(betterTo) - tree.getEndOffset(originalNode)
    return TextRange(startDelta + startOffset, endDelta + endOffset)
}

private val DOC_AND_COMMENT_TOKENS = setOf(
    WHITE_SPACE, KtTokens.IDENTIFIER,
    KtTokens.EOL_COMMENT, KtTokens.BLOCK_COMMENT, KtTokens.SHEBANG_COMMENT, KtTokens.DOC_COMMENT
)

private val FILLER_TOKENS = setOf(
    KtTokens.WHITE_SPACE,
    KtTokens.EOL_COMMENT,
    KtTokens.BLOCK_COMMENT,
    KtTokens.SHEBANG_COMMENT,
    KtTokens.DOC_COMMENT,
)

private fun LighterASTNode.nonFillerFirstChildOrSelf(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode =
    getChildren(tree).firstOrNull { !it.isFiller() } ?: this

internal fun LighterASTNode.nonFillerLastChildOrSelf(tree: FlyweightCapableTreeStructure<LighterASTNode>): LighterASTNode =
    getChildren(tree).lastOrNull { !it.isFiller() } ?: this

internal fun LighterASTNode.isFiller() = tokenType in FILLER_TOKENS

private fun hasSyntaxErrors(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    if (node.tokenType == TokenType.ERROR_ELEMENT) return true

    val children = node.getChildren(tree)
    return children.lastOrNull {
        val tokenType = it.tokenType
        tokenType !is KtSingleValueToken && tokenType !in DOC_AND_COMMENT_TOKENS
    }?.let { hasSyntaxErrors(it, tree) } == true
}

val KtLightSourceElement.startOffsetSkippingComments: Int
    get() {
        val children = lighterASTNode.getChildren(treeStructure)

        // The solution to find first non comment children will not work here. `treeStructure` can have different root
        // than original program. Because of that `startOffset` is relative and not in absolute value.
        val comments = children.takeWhile { it.tokenType in FILLER_TOKENS }
        return startOffset + comments.sumOf { it.textLength }
    }

