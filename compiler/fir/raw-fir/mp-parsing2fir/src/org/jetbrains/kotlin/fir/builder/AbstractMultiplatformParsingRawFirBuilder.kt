/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.builder

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.SyntaxElementTypeSet
import com.intellij.platform.syntax.element.SyntaxTokenTypes.BAD_CHARACTER
import com.intellij.platform.syntax.element.SyntaxTokenTypes.ERROR_ELEMENT
import com.intellij.platform.syntax.syntaxElementTypeSetOf
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.KtLightSourceElement
import org.jetbrains.kotlin.KtRealSourceElementKind
import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.isExpression
import org.jetbrains.kotlin.fir.lightTree.converter.AbstractTreeRawFirBuilder
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.lexer.KtTokens.COMMENTS
import org.jetbrains.kotlin.kmp.lexer.KtTokens.DOT
import org.jetbrains.kotlin.kmp.lexer.KtTokens.SAFE_ACCESS
import org.jetbrains.kotlin.kmp.lexer.KtTokens.SEMICOLON
import org.jetbrains.kotlin.kmp.lexer.KtTokens.WHITE_SPACE
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.kmp.tree.LightNode
import org.jetbrains.kotlin.kmp.tree.LightSyntaxTree

@Suppress("UnstableApiUsage")
abstract class AbstractMultiplatformParsingRawFirBuilder(
    baseSession: FirSession,
    val treeStructure: KotlinLightTreeStructure,
    context: Context<LightNode> = Context(),
) : AbstractTreeRawFirBuilder<LightNode, SyntaxElementType>(baseSession, context) {
    companion object {
        protected val ignoredTokens: SyntaxElementTypeSet = COMMENTS + syntaxElementTypeSetOf(
            WHITE_SPACE, SEMICOLON, ERROR_ELEMENT, BAD_CHARACTER,
        )
    }

    protected val tree: LightSyntaxTree
        get() = treeStructure.tree

    override val LightNode.elementType: SyntaxElementType
        get() = tree.getType(this)

    override fun SyntaxElementType.typeToTokenId(): Int {
        val nodeTypeId = KtNodeTypes.getElementTypeId(this)
        if (nodeTypeId != 0) return nodeTypeId
        return KtTokens.getElementTypeId(this)
    }

    override fun KtSourceElement.isChildInParentheses(): Boolean =
        (treeStructure.getParent(lighterASTNode) as? KotlinLightAstNode)?.node?.tokenType == KtNodeTypes.PARENTHESIZED

    override fun LightNode.toFirSourceElement(kind: KtFakeSourceElementKind?): KtSourceElement {
        val startOffset = tree.getStartOffset(this)
        val endOffset = tree.getEndOffset(this)
        val lighterNode = KotlinLightAstNode(tree, this)
        return KtLightSourceElement(lighterNode, startOffset, endOffset, treeStructure, kind ?: KtRealSourceElementKind)
    }

    override fun KtSourceElement.toNode(): LightNode {
        return ((this as KtLightSourceElement).lighterASTNode as KotlinLightAstNode).node
    }

    val LightNode.tokenType: SyntaxElementType
        get() = tree.getType(this)

    override val LightNode.asText: String
        get() = tree.getText(this).toString()

    override fun LightNode.getChildNodeByType(type: SyntaxElementType): LightNode? {
        return tree.getChildren(this).firstOrNull { it.tokenType == type }
    }

    override val LightNode?.receiverExpression: LightNode?
        get() {
            var candidate: LightNode? = null
            var result: LightNode? = null
            this?.forEachChildren {
                if (result != null) return@forEachChildren
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> result = if (candidate?.tokenType != ERROR_ELEMENT) candidate else null
                    else -> candidate = it
                }
            }
            return result
        }

    override val LightNode?.selectorExpression: LightNode?
        get() {
            var isSelector = false
            var result: LightNode? = null
            this?.forEachChildren {
                if (result != null) return@forEachChildren
                when (it.tokenType) {
                    DOT, SAFE_ACCESS -> isSelector = true
                    else -> if (isSelector) {
                        result = if (it.tokenType != ERROR_ELEMENT) it else null
                    }
                }
            }
            return result
        }

    override val LightNode?.indexExpressions: List<LightNode>?
        get() = this?.getLastChildExpression()?.let {
            tree.getChildren(it).filter { it.toTokenId().isExpression() }
        }

    override fun LightNode.getParent(): LightNode? {
        return tree.getParent(this)
    }

    override fun LightNode.getChildren(): List<LightNode> {
        return tree.getChildren(this)
    }

    override fun LightNode.getFirstChildExpression(): LightNode? {
        return tree.getChildren(this).firstOrNull { it.toTokenId().isExpression() }
    }

    override fun LightNode.forEachChildren(f: (LightNode) -> Unit) {
        val kids = tree.getChildren(this)
        for (kid in kids) {
            if (ignoredTokens.contains(kid.tokenType)) continue
            f(kid)
        }
    }

    override fun <T> LightNode.forEachChildrenReturnList(f: (LightNode, MutableList<T>) -> Unit): MutableList<T> {
        val kids = tree.getChildren(this)

        val container = mutableListOf<T>()
        for (kid in kids) {
            if (ignoredTokens.contains(kid.tokenType)) continue
            f(kid, container)
        }

        return container
    }
}
