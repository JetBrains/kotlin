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
import org.jetbrains.kotlin.kmp.utils.SyntaxElementTypesWithIds
import org.jetbrains.kotlin.types.ConstantValueKind

abstract class AbstractTreeRawFirBuilder<Node : Any, Type : Any>(
    baseSession: FirSession,
    context: Context<Node>,
) : AbstractRawFirBuilder<Node, Type>(baseSession, context) {
    protected fun Node.getAsStringWithoutBacktick(): String {
        return this.asText.replace("`", "")
    }

    abstract fun Node.getParent(): Node?

    private fun Node.getModifierList(): Node? = getChildNodeByTokenId(KtNodeTypes.MODIFIER_LIST_ID)

    private fun Node.getVarargKeyword(): Node? = getChildNodeByTokenId(KtTokens.VARARG_MODIFIER_ID)

    override val Node.isVararg: Boolean
        get() = getModifierList()?.getVarargKeyword() != null

    override fun Node.isArrayAccessExpression(): Boolean = toTokenId() == KtNodeTypes.ARRAY_ACCESS_EXPRESSION_ID

    override fun Node.isSafeAccessExpression(): Boolean = toTokenId() == KtNodeTypes.SAFE_ACCESS_EXPRESSION_ID

    override fun Node.isStringInterpolationPrefixOrQuote(): Boolean = when (toTokenId()) {
        KtNodeTypes.STRING_INTERPOLATION_PREFIX_ID, KtTokens.OPEN_QUOTE_ID, KtTokens.CLOSING_QUOTE_ID -> true
        else -> false
    }

    override fun Node.isLiteralStringTemplateEntry(): Boolean =
        toTokenId() == KtNodeTypes.LITERAL_STRING_TEMPLATE_ENTRY_ID

    override fun Node.isEscapeStringTemplateEntry(): Boolean =
        toTokenId() == KtNodeTypes.ESCAPE_STRING_TEMPLATE_ENTRY_ID

    override fun Node.isShortOrLongStringTemplateEntry(): Boolean = when (toTokenId()) {
        KtNodeTypes.SHORT_STRING_TEMPLATE_ENTRY_ID, KtNodeTypes.LONG_STRING_TEMPLATE_ENTRY_ID -> true
        else -> false
    }

    override fun Type.toConstantValueKind(): ConstantValueKind? {
        return when (typeToTokenId()) {
            KtNodeTypes.INTEGER_CONSTANT_ID -> ConstantValueKind.Int
            KtNodeTypes.FLOAT_CONSTANT_ID -> ConstantValueKind.Float
            KtNodeTypes.BOOLEAN_CONSTANT_ID -> ConstantValueKind.Boolean
            KtNodeTypes.CHARACTER_CONSTANT_ID -> ConstantValueKind.Char
            KtNodeTypes.NULL_ID -> ConstantValueKind.Null
            else -> null
        }
    }

    /**
     * See [UNWRAPPABLE_TOKEN_TYPES][org.jetbrains.kotlin.psi.psiUtil.UNWRAPPABLE_TOKEN_TYPES]
     */
    override fun Node?.unwrap(): Node? {
        // NOTE: By removing surrounding parentheses and labels, FirLabels will NOT be created for those labels.
        // This should be fine since the label is meaningless and unusable for a ++/-- argument or assignment LHS.
        var unwrapped = this
        while (true) {
            val tokenId = unwrapped?.toTokenId()
            unwrapped = when (tokenId) {
                null -> return unwrapped
                KtNodeTypes.PARENTHESIZED_ID -> unwrapped.getExpressionInParentheses()
                KtNodeTypes.LABELED_EXPRESSION_ID -> unwrapped.getLabeledExpression()
                KtNodeTypes.ANNOTATED_EXPRESSION_ID -> unwrapped.getAnnotatedExpression()
                else -> return unwrapped
            }
        }
    }

    abstract fun KtSourceElement.toNode(): Node

    abstract fun Node.getChildren(): List<Node>
    abstract fun Node?.getChildrenAsArray(): Array<out Node?>

    inline fun Node.forEachChildren(f: (Node) -> Unit) {
        val kidsArray = this.getChildrenAsArray()
        for (kid in kidsArray) {
            if (kid == null) break
            if (ignoredTokensId.contains(kid.toTokenId())) continue
            f(kid)
        }
    }

    inline fun <T> Node.forEachChildrenReturnList(f: (Node, MutableList<T>) -> Unit): MutableList<T> {
        val kidsArray = this.getChildrenAsArray()

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

    override fun Node.getLabelName(): String? {
        if (toTokenId() == KtNodeTypes.FUNCTION_ID) {
            return getParent()?.getLabelName()
        }
        this.forEachChildren {
            when (it.toTokenId()) {
                KtNodeTypes.LABEL_QUALIFIER_ID -> return it.asText.replaceFirst("@", "").let(::unquoteIdentifier)
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

    companion object {
        val ignoredTokensId: HashSet<Int> = hashSetOf(
            KtTokens.EOL_COMMENT_ID, KtTokens.BLOCK_COMMENT_ID, KtTokens.DOC_COMMENT_ID, KtTokens.SHEBANG_COMMENT_ID,
            KtTokens.WHITE_SPACE_ID, KtTokens.SEMICOLON_ID, SyntaxElementTypesWithIds.NO_ID,
        )
    }
}
