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
import org.jetbrains.kotlin.syntax.BasicSyntaxElementTypeIds
import org.jetbrains.kotlin.syntax.SyntaxId
import org.jetbrains.kotlin.syntax.KotlinSyntaxNodeTypeIds
import org.jetbrains.kotlin.syntax.KotlinSyntaxTokenIds
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

    private fun Node.getModifierList(): Node? = getChildNodeByTokenId(KotlinSyntaxNodeTypeIds.MODIFIER_LIST)

    private fun Node.getVarargKeyword(): Node? = getChildNodeByTokenId(KotlinSyntaxTokenIds.VARARG_MODIFIER)

    override val Node.isVararg: Boolean
        get() = getModifierList()?.getVarargKeyword() != null

    override fun Node.isArrayAccessExpression(): Boolean = toTokenId() == KotlinSyntaxNodeTypeIds.ARRAY_ACCESS_EXPRESSION

    override fun Node.isSafeAccessExpression(): Boolean = toTokenId() == KotlinSyntaxNodeTypeIds.SAFE_ACCESS_EXPRESSION

    override fun Node.isStringInterpolationPrefixOrQuote(): Boolean = when (toTokenId()) {
        KotlinSyntaxNodeTypeIds.STRING_INTERPOLATION_PREFIX, KotlinSyntaxTokenIds.OPEN_QUOTE, KotlinSyntaxTokenIds.CLOSING_QUOTE -> true
        else -> false
    }

    override fun Node.isLiteralStringTemplateEntry(): Boolean =
        toTokenId() == KotlinSyntaxNodeTypeIds.LITERAL_STRING_TEMPLATE_ENTRY

    override fun Node.isEscapeStringTemplateEntry(): Boolean =
        toTokenId() == KotlinSyntaxNodeTypeIds.ESCAPE_STRING_TEMPLATE_ENTRY

    override fun Node.isShortOrLongStringTemplateEntry(): Boolean = when (toTokenId()) {
        KotlinSyntaxNodeTypeIds.SHORT_STRING_TEMPLATE_ENTRY, KotlinSyntaxNodeTypeIds.LONG_STRING_TEMPLATE_ENTRY -> true
        else -> false
    }

    override fun Type.toConstantValueKind(): ConstantValueKind? {
        return when (typeToTokenId()) {
            KotlinSyntaxNodeTypeIds.INTEGER_CONSTANT -> ConstantValueKind.Int
            KotlinSyntaxNodeTypeIds.FLOAT_CONSTANT -> ConstantValueKind.Float
            KotlinSyntaxNodeTypeIds.BOOLEAN_CONSTANT -> ConstantValueKind.Boolean
            KotlinSyntaxNodeTypeIds.CHARACTER_CONSTANT -> ConstantValueKind.Char
            KotlinSyntaxNodeTypeIds.NULL -> ConstantValueKind.Null
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
                KotlinSyntaxNodeTypeIds.PARENTHESIZED -> unwrapped.getExpressionInParentheses()
                KotlinSyntaxNodeTypeIds.LABELED_EXPRESSION -> unwrapped.getLabeledExpression()
                KotlinSyntaxNodeTypeIds.ANNOTATED_EXPRESSION -> unwrapped.getAnnotatedExpression()
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

    fun Node.toTokenId(): SyntaxId = elementType.typeToTokenId()

    abstract fun Type.typeToTokenId(): SyntaxId

    fun Node.getOperationTokenId(): SyntaxId {
        return getChildren().first().toTokenId()
    }

    fun Node.getFirstChild(): Node? = getChildren().firstOrNull()

    fun Node.getFirstChildExpressionUnwrapped(): Node? {
        val expression = getFirstChildExpression() ?: return null
        return if (expression.toTokenId() == KotlinSyntaxNodeTypeIds.PARENTHESIZED) {
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
        if (toTokenId() == KotlinSyntaxNodeTypeIds.FUNCTION) {
            return getParent()?.getLabelName()
        }
        this.forEachChildren {
            when (it.toTokenId()) {
                KotlinSyntaxNodeTypeIds.LABEL_QUALIFIER -> return it.asText.replaceFirst("@", "").let(::unquoteIdentifier)
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

    fun Node.getChildNodeByTokenId(tokenId: SyntaxId): Node? {
        return getChildrenAsArray().firstOrNull { it?.toTokenId() == tokenId }
    }

    fun Node?.getChildNodesByTokenId(tokenId: SyntaxId): List<Node> {
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
        val ignoredTokensId: HashSet<SyntaxId> = hashSetOf(
            KotlinSyntaxTokenIds.EOL_COMMENT, KotlinSyntaxTokenIds.BLOCK_COMMENT, KotlinSyntaxTokenIds.DOC_COMMENT, KotlinSyntaxTokenIds.SHEBANG_COMMENT,
            BasicSyntaxElementTypeIds.WHITE_SPACE, BasicSyntaxElementTypeIds.BAD_CHARACTER, BasicSyntaxElementTypeIds.ERROR_ELEMENT,
            KotlinSyntaxTokenIds.SEMICOLON,
            BasicSyntaxElementTypeIds.NO_ID, // TODO: it should be removed, KT-91283
        )
    }
}
