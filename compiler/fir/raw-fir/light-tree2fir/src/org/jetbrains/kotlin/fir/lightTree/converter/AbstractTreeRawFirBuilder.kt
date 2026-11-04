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
import org.jetbrains.kotlin.syntax.NO_ID
import org.jetbrains.kotlin.syntax.SyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.SyntaxTokenIds
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

    private fun Node.getModifierList(): Node? = getChildNodeByTokenId(SyntaxNodeTypeIds.MODIFIER_LIST)

    private fun Node.getVarargKeyword(): Node? = getChildNodeByTokenId(SyntaxTokenIds.VARARG_MODIFIER)

    override val Node.isVararg: Boolean
        get() = getModifierList()?.getVarargKeyword() != null

    override fun Node.isArrayAccessExpression(): Boolean = toTokenId() == SyntaxNodeTypeIds.ARRAY_ACCESS_EXPRESSION

    override fun Node.isSafeAccessExpression(): Boolean = toTokenId() == SyntaxNodeTypeIds.SAFE_ACCESS_EXPRESSION

    override fun Node.isStringInterpolationPrefixOrQuote(): Boolean = when (toTokenId()) {
        SyntaxNodeTypeIds.STRING_INTERPOLATION_PREFIX, SyntaxTokenIds.OPEN_QUOTE, SyntaxTokenIds.CLOSING_QUOTE -> true
        else -> false
    }

    override fun Node.isLiteralStringTemplateEntry(): Boolean =
        toTokenId() == SyntaxNodeTypeIds.LITERAL_STRING_TEMPLATE_ENTRY

    override fun Node.isEscapeStringTemplateEntry(): Boolean =
        toTokenId() == SyntaxNodeTypeIds.ESCAPE_STRING_TEMPLATE_ENTRY

    override fun Node.isShortOrLongStringTemplateEntry(): Boolean = when (toTokenId()) {
        SyntaxNodeTypeIds.SHORT_STRING_TEMPLATE_ENTRY, SyntaxNodeTypeIds.LONG_STRING_TEMPLATE_ENTRY -> true
        else -> false
    }

    override fun Type.toConstantValueKind(): ConstantValueKind? {
        return when (typeToTokenId()) {
            SyntaxNodeTypeIds.INTEGER_CONSTANT -> ConstantValueKind.Int
            SyntaxNodeTypeIds.FLOAT_CONSTANT -> ConstantValueKind.Float
            SyntaxNodeTypeIds.BOOLEAN_CONSTANT -> ConstantValueKind.Boolean
            SyntaxNodeTypeIds.CHARACTER_CONSTANT -> ConstantValueKind.Char
            SyntaxNodeTypeIds.NULL -> ConstantValueKind.Null
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
                SyntaxNodeTypeIds.PARENTHESIZED -> unwrapped.getExpressionInParentheses()
                SyntaxNodeTypeIds.LABELED_EXPRESSION -> unwrapped.getLabeledExpression()
                SyntaxNodeTypeIds.ANNOTATED_EXPRESSION -> unwrapped.getAnnotatedExpression()
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
        return if (expression.toTokenId() == SyntaxNodeTypeIds.PARENTHESIZED) {
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
        if (toTokenId() == SyntaxNodeTypeIds.FUNCTION) {
            return getParent()?.getLabelName()
        }
        this.forEachChildren {
            when (it.toTokenId()) {
                SyntaxNodeTypeIds.LABEL_QUALIFIER -> return it.asText.replaceFirst("@", "").let(::unquoteIdentifier)
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
            SyntaxTokenIds.EOL_COMMENT, SyntaxTokenIds.BLOCK_COMMENT, SyntaxTokenIds.DOC_COMMENT, SyntaxTokenIds.SHEBANG_COMMENT,
            SyntaxTokenIds.WHITE_SPACE, SyntaxTokenIds.SEMICOLON, NO_ID,
        )
    }
}
