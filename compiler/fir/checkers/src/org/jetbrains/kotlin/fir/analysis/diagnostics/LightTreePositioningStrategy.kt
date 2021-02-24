/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.diagnostics

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.TextRange
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.WHITE_SPACE

open class LightTreePositioningStrategy {
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
): List<TextRange> {
    if (node === originalNode) return listOf(TextRange(startOffset, endOffset))
    val startDelta = tree.getStartOffset(node) - tree.getStartOffset(originalNode)
    val endDelta = tree.getEndOffset(node) - tree.getEndOffset(originalNode)
    return listOf(TextRange(startDelta + startOffset, endDelta + endOffset))
}

fun markRange(
    from: LighterASTNode,
    to: LighterASTNode,
    startOffset: Int,
    endOffset: Int,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    originalNode: LighterASTNode
): List<TextRange> {
    val startDelta = tree.getStartOffset(from) - tree.getStartOffset(originalNode)
    val endDelta = tree.getEndOffset(to) - tree.getEndOffset(originalNode)
    return listOf(TextRange(startDelta + startOffset, endDelta + endOffset))
}

private val DOC_AND_COMMENT_TOKENS = setOf(
    WHITE_SPACE, KtTokens.IDENTIFIER,
    KtTokens.EOL_COMMENT, KtTokens.BLOCK_COMMENT, KtTokens.SHEBANG_COMMENT, KtTokens.DOC_COMMENT
)

private fun hasSyntaxErrors(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): Boolean {
    if (node.tokenType == TokenType.ERROR_ELEMENT) return true

    val childrenRef = Ref<Array<LighterASTNode?>?>()
    tree.getChildren(node, childrenRef)
    val children = childrenRef.get() ?: return false
    return children.filterNotNull().lastOrNull {
        val tokenType = it.tokenType
        tokenType !is KtSingleValueToken && tokenType !in DOC_AND_COMMENT_TOKENS
    }?.let { hasSyntaxErrors(it, tree) } == true
}

