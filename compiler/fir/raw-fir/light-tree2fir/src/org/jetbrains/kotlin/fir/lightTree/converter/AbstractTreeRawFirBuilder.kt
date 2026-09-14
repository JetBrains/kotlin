/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this| source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.isExpression
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.KtNodeTypes
import org.jetbrains.kotlin.name.Name

abstract class AbstractTreeRawFirBuilder<Node : Any, Type : Any>(
    baseSession: FirSession,
    context: Context<Node>,
) : AbstractRawFirBuilder<Node, Type>(baseSession, context) {
    override fun Node.getReferencedNameAsName(): Name {
        return asText.nameAsSafeName()
    }

    private fun Node.getModifierList(): Node? = getChildNodeByTokenId(KtNodeTypes.MODIFIER_LIST_ID)

    private fun Node.getVarargKeyword(): Node? = getChildNodeByTokenId(KtTokens.VARARG_MODIFIER_ID)

    override val Node.isVararg: Boolean
        get() = getModifierList()?.getVarargKeyword() != null

    abstract fun KtSourceElement.toNode(): Node

    abstract fun Node.getChildren(): List<Node>

    fun Node.getOperationTokenId(): Int {
        return getChildren().first().toTokenId()
    }

    fun Node.getFirstChild(): Node? = getChildren().firstOrNull()

    fun Node.getFirstChildExpressionUnwrapped(): Node? {
        val expression = getFirstChildExpression() ?: return null
        return if (expression.toTokenId() == KtNodeTypes.PARENTHESIZED_ID) {
            expression.getFirstChildExpressionUnwrapped()
        } else {
            expression
        }
    }

    fun Node.getFirstChildExpression(): Node? {
        return getChildren().firstOrNull { it.toTokenId().isExpression() }
    }

    fun Node.getLastChildExpression(): Node? {
        return getChildren().lastOrNull { it.toTokenId().isExpression() }
    }

    override fun Node.getExpressionInParentheses(): Node? {
        return getFirstChildExpression()
    }

    override fun Node.getAnnotatedExpression(): Node? = getFirstChildExpression()

    override fun Node.getLabeledExpression(): Node? = getLastChildExpression()

    override val Node?.arrayExpression: Node?
        get() = this?.getFirstChildExpression()

    fun Node.getChildNodeByTokenId(tokenId: Int): Node? {
        return getChildrenAsArray().firstOrNull { it?.toTokenId() == tokenId }
    }

    fun Node?.getChildNodesByTokenId(tokenId: Int): List<Node> {
        return this?.forEachChildrenReturnList { node, container ->
            when (node.toTokenId()) {
                tokenId -> container += node
            }
        } ?: emptyList()
    }

    override fun convertReplSnippet(
        script: Node,
        scriptSource: KtSourceElement,
        fileName: String,
        snippetSetup: FirReplSnippetBuilder.() -> Unit,
        functionBodySetup: FirBlockBuilder.() -> Unit,
        statementsSetup: MutableList<FirElement>.() -> Unit,
    ): FirReplSnippet {
        TODO("KT-77583")
    }
}
