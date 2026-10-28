/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this| source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.lightTree.converter

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.NO_ID
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.isExpression
import org.jetbrains.kotlin.fir.builder.AbstractRawFirBuilder
import org.jetbrains.kotlin.fir.builder.Context
import org.jetbrains.kotlin.fir.declarations.FirReplSnippet
import org.jetbrains.kotlin.fir.declarations.builder.FirReplSnippetBuilder
import org.jetbrains.kotlin.fir.expressions.builder.FirBlockBuilder
import org.jetbrains.kotlin.KtNodeTypeId
import org.jetbrains.kotlin.KtTokenId
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled

abstract class AbstractTreeRawFirBuilder<Node : Any, Type : Any>(
    baseSession: FirSession,
    context: Context<Node>,
) : AbstractRawFirBuilder<Node, Type>(baseSession, context) {
    protected fun Node.getAsStringWithoutBacktick(): String {
        return this.asText.replace("`", "")
    }

    abstract fun Node.getParent(): Node?

    private fun Node.getModifierList(): Node? = getChildNodeByTokenId(KtNodeTypeId.MODIFIER_LIST)

    private fun Node.getVarargKeyword(): Node? = getChildNodeByTokenId(KtTokenId.VARARG_MODIFIER)

    override val Node.isVararg: Boolean
        get() = getModifierList()?.getVarargKeyword() != null

    override fun Node.isArrayAccessExpression(): Boolean = toTokenId() == KtNodeTypeId.ARRAY_ACCESS_EXPRESSION

    override fun Node.isSafeAccessExpression(): Boolean = toTokenId() == KtNodeTypeId.SAFE_ACCESS_EXPRESSION

    override fun Node.isStringInterpolationPrefixOrQuote(): Boolean = when (toTokenId()) {
        KtNodeTypeId.STRING_INTERPOLATION_PREFIX, KtTokenId.OPEN_QUOTE, KtTokenId.CLOSING_QUOTE -> true
        else -> false
    }

    override fun Node.isLiteralStringTemplateEntry(): Boolean =
        toTokenId() == KtNodeTypeId.LITERAL_STRING_TEMPLATE_ENTRY

    override fun Node.isEscapeStringTemplateEntry(): Boolean =
        toTokenId() == KtNodeTypeId.ESCAPE_STRING_TEMPLATE_ENTRY

    override fun Node.isShortOrLongStringTemplateEntry(): Boolean = when (toTokenId()) {
        KtNodeTypeId.SHORT_STRING_TEMPLATE_ENTRY, KtNodeTypeId.LONG_STRING_TEMPLATE_ENTRY -> true
        else -> false
    }

    override fun Type.toConstantValueKind(): ConstantValueKind? {
        return when (typeToTokenId()) {
            KtNodeTypeId.INTEGER_CONSTANT -> ConstantValueKind.Int
            KtNodeTypeId.FLOAT_CONSTANT -> ConstantValueKind.Float
            KtNodeTypeId.BOOLEAN_CONSTANT -> ConstantValueKind.Boolean
            KtNodeTypeId.CHARACTER_CONSTANT -> ConstantValueKind.Char
            KtNodeTypeId.NULL -> ConstantValueKind.Null
            else -> null
        }
    }

    /**
     * See [UNWRAPPABLE_TOKEN_TYPES][org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES]
     */
    override fun Node.unwrap(): Node? {
        // NOTE: By removing surrounding parentheses and labels, FirLabels will NOT be created for those labels.
        // This should be fine since the label is meaningless and unusable for a ++/-- argument or assignment LHS.
        var unwrapped: Node? = this
        while (true) {
            val tokenId = unwrapped?.toTokenId()
            unwrapped = when (tokenId) {
                KtNodeTypeId.PARENTHESIZED -> unwrapped.getExpressionInParentheses()
                KtNodeTypeId.LABELED_EXPRESSION -> unwrapped.getLabeledExpression()
                KtNodeTypeId.ANNOTATED_EXPRESSION -> unwrapped.getAnnotatedExpression()
                else -> return unwrapped
            }
        }
    }

    abstract fun KtSourceElement.toNode(): Node

    abstract fun Node.getChildren(): List<Node>
    abstract fun Node.getChildrenAsArray(): Array<out Node?>

    inline fun Node.forEachChildren(f: (Node) -> Unit) {
        val kidsArray = getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) break
            if (ignoredTokensId.contains(kid.toTokenId())) continue
            f(kid)
        }
    }

    inline fun <T> Node.forEachChildrenReturnList(f: (Node, MutableList<T>) -> Unit): MutableList<T> {
        val kidsArray = getChildrenAsArray()

        val container = mutableListOf<T>()
        for (kid in kidsArray) {
            if (kid == null) break
            if (ignoredTokensId.contains(kid.toTokenId())) continue
            f(kid, container)
        }

        return container
    }

    fun Node.toTokenId(): Int = elementType.typeToTokenId()

    abstract fun Type.typeToTokenId(): Int

    fun Node.getOperationTokenId(): Int {
        return getChildren().first().toTokenId()
    }

    fun Node.getFirstChild(): Node? = getChildren().firstOrNull()

    fun Node.getFirstChildExpressionUnwrapped(): Node? {
        val expression = getFirstChildExpression() ?: return null
        return if (expression.toTokenId() == KtNodeTypeId.PARENTHESIZED) {
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

    override fun Node.getLabelName(): String? {
        if (toTokenId() == KtNodeTypeId.FUNCTION) {
            return getParent()?.getLabelName()
        }
        this.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypeId.LABEL_QUALIFIER -> return it.asText.replaceFirst("@", "").let(::unquoteIdentifier)
            }
        }
        return null
    }

    private fun unquoteIdentifier(quoted: String): String {
        if (quoted.indexOf('`') < 0) {
            return quoted
        }

        if (quoted.startsWith('`') && quoted.endsWith('`') && quoted.length >= 2) {
            return quoted.substring(1, quoted.length - 1)
        } else {
            return quoted
        }
    }

    override val Node.arrayExpression: Node?
        get() = getFirstChildExpression()

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
        shouldNotBeCalled("REPL snippets are converted by TreeRawFirDeclarationBuilderProxy")
    }

    companion object {
        val ignoredTokensId: HashSet<Int> = hashSetOf(
            KtTokenId.EOL_COMMENT, KtTokenId.BLOCK_COMMENT, KtTokenId.DOC_COMMENT, KtTokenId.SHEBANG_COMMENT,
            KtTokenId.WHITE_SPACE, KtTokenId.SEMICOLON, NO_ID,
        )
    }
}
