/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.impl.source.tree.TreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.NodeIndentStrategy.Companion.strategy
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.leaves
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings

private val QUALIFIED_OPERATION = TokenSet.create(DOT, SAFE_ACCESS)
private val QUALIFIED_EXPRESSIONS = TokenSet.create(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION)
private val ELVIS_SET = TokenSet.create(KtTokens.ELVIS)

private const val KDOC_COMMENT_INDENT = 1

private val BINARY_EXPRESSIONS = TokenSet.create(KtNodeTypes.BINARY_EXPRESSION, KtNodeTypes.BINARY_WITH_TYPE, KtNodeTypes.IS_EXPRESSION)
private val KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC, KDocElementTypes.KDOC_SECTION, KDocElementTypes.KDOC_TAG)

private val CODE_BLOCKS = TokenSet.create(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY, KtNodeTypes.FUNCTION_LITERAL)

private val ALIGN_FOR_BINARY_OPERATIONS = TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, ELVIS, LT, GT, LTEQ, GTEQ, ANDAND, OROR)
private val ANNOTATIONS = TokenSet.create(KtNodeTypes.ANNOTATION_ENTRY, KtNodeTypes.ANNOTATION)

val CodeStyleSettings.kotlinCommonSettings: KotlinCommonCodeStyleSettings
    get() = getCommonSettings(KotlinLanguage.INSTANCE) as KotlinCommonCodeStyleSettings

val CodeStyleSettings.kotlinCustomSettings: KotlinCodeStyleSettings
    get() = getCustomSettings(KotlinCodeStyleSettings::class.java)!!

typealias WrappingStrategy = (childElement: ASTNode) -> Wrap?

fun noWrapping(childElement: ASTNode): Wrap? = null

abstract class KotlinCommonBlock(
    private val node: ASTNode,
    private val settings: CodeStyleSettings,
    private val spacingBuilder: KotlinSpacingBuilder,
    private val alignmentStrategy: CommonAlignmentStrategy,
    private val overrideChildren: Sequence<ASTNode>? = null
) {
    @Volatile
    private var mySubBlocks: List<ASTBlock>? = null

    fun getTextRange(): TextRange {
        if (overrideChildren != null) {
            return TextRange(overrideChildren.first().startOffset, overrideChildren.last().textRange.endOffset)
        }
        return node.textRange
    }

    protected abstract fun createBlock(
        node: ASTNode,
        alignmentStrategy: CommonAlignmentStrategy,
        indent: Indent?,
        wrap: Wrap?,
        settings: CodeStyleSettings,
        spacingBuilder: KotlinSpacingBuilder,
        overrideChildren: Sequence<ASTNode>? = null
    ): ASTBlock

    protected abstract fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock

    protected abstract fun getSubBlocks(): List<Block>

    protected abstract fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes

    protected abstract fun isIncompleteInSuper(): Boolean

    protected abstract fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy

    protected abstract fun getAlignment(): Alignment?

    protected abstract fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy

    protected abstract fun getNullAlignmentStrategy(): CommonAlignmentStrategy

    fun isLeaf(): Boolean = node.firstChildNode == null

    fun buildChildren(): List<Block> {
        if (mySubBlocks != null) {
            return mySubBlocks!!
        }

        var nodeSubBlocks = buildSubBlocks()

        if (node.elementType in QUALIFIED_EXPRESSIONS) {
            nodeSubBlocks = splitSubBlocksOnDot(nodeSubBlocks)
        } else {
            val psi = node.psi
            if (psi is KtBinaryExpression && psi.operationToken == KtTokens.ELVIS) {
                nodeSubBlocks = splitSubBlocksOnElvis(nodeSubBlocks)
            }
        }

        mySubBlocks = nodeSubBlocks

        return nodeSubBlocks
    }

    private fun splitSubBlocksOnDot(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        val operationBlockIndex = nodeSubBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
        if (operationBlockIndex != -1) {
            // Create fake ".something" or "?.something" block here, so child indentation will be
            // relative to it when it starts from new line (see Indent javadoc).

            val isNonFirstChainedCall = operationBlockIndex > 0 && isCallBlock(nodeSubBlocks[operationBlockIndex - 1])

            // enforce indent to children when there's a line break before the dot in any call in the chain (meaning that
            // the call chain following that call is indented)
            val enforceIndentToChildren = anyCallInCallChainIsWrapped(nodeSubBlocks[operationBlockIndex - 1])

            val indentType = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS) {
                if (enforceIndentToChildren) Indent.Type.CONTINUATION else Indent.Type.CONTINUATION_WITHOUT_FIRST
            } else {
                Indent.Type.NORMAL
            }
            val indent = Indent.getIndent(
                indentType, false,
                enforceIndentToChildren
            )
            val wrap = if ((settings.kotlinCommonSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN || isNonFirstChainedCall) &&
                canWrapCallChain(node))
                Wrap.createWrap(settings.kotlinCommonSettings.METHOD_CALL_CHAIN_WRAP, true)
            else
                null

            return nodeSubBlocks.splitAtIndex(operationBlockIndex, indent, wrap)
        }
        return nodeSubBlocks
    }

    private fun List<ASTBlock>.splitAtIndex(index: Int, indent: Indent?, wrap: Wrap?): List<ASTBlock> {
        val operationBlock = this[index]
        val operationSyntheticBlock = SyntheticKotlinBlock(
            operationBlock.node,
            subList(index, size),
            null, indent, wrap, spacingBuilder
        ) { createSyntheticSpacingNodeBlock(it) }

        return subList(0, index) + operationSyntheticBlock
    }

    private fun anyCallInCallChainIsWrapped(astBlock: ASTBlock): Boolean {
        var result: ASTBlock? = astBlock
        while (true) {
            if (result == null || !isCallBlock(result)) return false
            val dot = result.node?.findChildByType(QUALIFIED_OPERATION)
            if (dot != null && hasLineBreakBefore(dot)) {
                return true
            }
            result = result.subBlocks.firstOrNull() as? ASTBlock?
        }
    }

    private fun isCallBlock(astBlock: ASTBlock): Boolean {
        val node = astBlock.node
        return node.elementType in QUALIFIED_EXPRESSIONS && node.lastChildNode?.elementType == KtNodeTypes.CALL_EXPRESSION
    }

    private fun canWrapCallChain(node: ASTNode): Boolean {
        val callChainParent = node.parents().firstOrNull { it.elementType !in QUALIFIED_EXPRESSIONS } ?: return true
        return callChainParent.elementType in CODE_BLOCKS ||
                callChainParent.elementType == KtNodeTypes.PROPERTY ||
                (callChainParent.elementType == KtNodeTypes.BINARY_EXPRESSION &&
                        (callChainParent.psi as KtBinaryExpression).operationToken in KtTokens.ALL_ASSIGNMENTS) ||
                callChainParent.elementType == KtNodeTypes.RETURN
    }

    private fun splitSubBlocksOnElvis(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        val elvisIndex = nodeSubBlocks.indexOfBlockWithType(ELVIS_SET)
        if (elvisIndex >= 0) {
            return nodeSubBlocks.splitAtIndex(
                elvisIndex,
                Indent.getContinuationIndent(),
                null
            )
        }
        return nodeSubBlocks
    }

    private fun createChildIndent(child: ASTNode): Indent? {
        val childParent = child.treeParent
        val childType = child.elementType

        if (childParent?.treeParent != null) {
            if (childParent.elementType === KtNodeTypes.BLOCK && childParent.treeParent.elementType === KtNodeTypes.SCRIPT) {
                return Indent.getNoneIndent()
            }
        }

        // do not indent child after heading comments inside declaration
        if (childParent != null && childParent.psi is KtDeclaration) {
            val prev = getPrevWithoutWhitespace(child)
            if (prev != null && COMMENTS.contains(prev.elementType) && getPrevWithoutWhitespaceAndComments(prev) == null) {
                return Indent.getNoneIndent()
            }
        }

        for (strategy in INDENT_RULES) {
            val indent = strategy.getIndent(child, settings)
            if (indent != null) {
                return indent
            }
        }

        // TODO: Try to rewrite other rules to declarative style
        if (childParent != null) {
            val parentType = childParent.elementType

            if (parentType === KtNodeTypes.VALUE_PARAMETER_LIST || parentType === KtNodeTypes.VALUE_ARGUMENT_LIST) {
                val prev = getPrevWithoutWhitespace(child)
                if (childType === RPAR && (prev == null || prev.elementType !== TokenType.ERROR_ELEMENT)) {
                    return Indent.getNoneIndent()
                }

                return if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)
                    Indent.getContinuationWithoutFirstIndent()
                else
                    Indent.getNormalIndent()
            }

            if (parentType === KtNodeTypes.TYPE_PARAMETER_LIST || parentType === KtNodeTypes.TYPE_ARGUMENT_LIST) {
                return Indent.getContinuationWithoutFirstIndent()
            }
        }

        return Indent.getNoneIndent()
    }

    fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType

        if (node.elementType == KtNodeTypes.BLOCK && node.treeParent.elementType == KtNodeTypes.SCRIPT) {
            return ChildAttributes(Indent.getNoneIndent(), null)
        }

        if (type == KtNodeTypes.IF) {
            val elseBlock = mySubBlocks?.getOrNull(newChildIndex)
            if (elseBlock != null && elseBlock.node.elementType == KtTokens.ELSE_KEYWORD) {
                return ChildAttributes.DELEGATE_TO_NEXT_CHILD
            }
        }

        if (newChildIndex > 0) {
            val prevBlock = mySubBlocks?.get(newChildIndex - 1)
            if (prevBlock?.node?.elementType == KtNodeTypes.MODIFIER_LIST) {
                return ChildAttributes(Indent.getNoneIndent(), null)
            }
        }

        return when (type) {
            in CODE_BLOCKS, KtNodeTypes.WHEN, KtNodeTypes.IF, KtNodeTypes.FOR, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE -> ChildAttributes(
                Indent.getNormalIndent(),
                null
            )

            KtNodeTypes.TRY -> ChildAttributes(Indent.getNoneIndent(), null)

            in QUALIFIED_EXPRESSIONS -> ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)

            KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST -> {
                val subBlocks = getSubBlocks()
                if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                    val block = subBlocks[newChildIndex]
                    ChildAttributes(block.indent, block.alignment)
                } else {
                    val indent =
                        if ((type == KtNodeTypes.VALUE_PARAMETER_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS) ||
                            (type == KtNodeTypes.VALUE_ARGUMENT_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)
                        ) {
                            Indent.getNormalIndent()
                        } else {
                            Indent.getContinuationIndent()
                        }
                    ChildAttributes(indent, null)
                }
            }

            DOC_COMMENT -> ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null)

            KtNodeTypes.PARENTHESIZED -> getSuperChildAttributes(newChildIndex)

            else -> {
                val blocks = getSubBlocks()
                if (newChildIndex != 0) {
                    val isIncomplete = if (newChildIndex < blocks.size) blocks[newChildIndex - 1].isIncomplete else isIncompleteInSuper()
                    if (isIncomplete) {
                        return getSuperChildAttributes(newChildIndex)
                    }
                }

                if (blocks.size > newChildIndex) {
                    val block = blocks[newChildIndex]
                    return ChildAttributes(block.indent, block.alignment)
                }

                ChildAttributes(Indent.getNoneIndent(), null)
            }
        }
    }

    private fun getChildrenAlignmentStrategy(): CommonAlignmentStrategy {
        val kotlinCommonSettings = settings.kotlinCommonSettings
        val kotlinCustomSettings = settings.kotlinCustomSettings
        val parentType = node.elementType
        return when {
            parentType === KtNodeTypes.VALUE_PARAMETER_LIST ->
                getAlignmentForChildInParenthesis(
                    kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS, KtNodeTypes.VALUE_PARAMETER, COMMA,
                    kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR
                )

            parentType === KtNodeTypes.VALUE_ARGUMENT_LIST ->
                getAlignmentForChildInParenthesis(
                    kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, KtNodeTypes.VALUE_ARGUMENT, COMMA,
                    kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR
                )

            parentType === KtNodeTypes.WHEN ->
                getAlignmentForCaseBranch(kotlinCustomSettings.ALIGN_IN_COLUMNS_CASE_BRANCH)

            parentType === KtNodeTypes.WHEN_ENTRY ->
                alignmentStrategy

            parentType in BINARY_EXPRESSIONS && getOperationType(node) in ALIGN_FOR_BINARY_OPERATIONS ->
                createAlignmentStrategy(kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, getAlignment())

            parentType === KtNodeTypes.SUPER_TYPE_LIST || parentType === KtNodeTypes.INITIALIZER_LIST ->
                createAlignmentStrategy(kotlinCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST, getAlignment())

            parentType === KtNodeTypes.PARENTHESIZED ->
                object : CommonAlignmentStrategy() {
                    private var bracketsAlignment: Alignment? =
                        if (kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION) Alignment.createAlignment() else null

                    override fun getAlignment(node: ASTNode): Alignment? {
                        val childNodeType = node.elementType
                        val prev = getPrevWithoutWhitespace(node)

                        if (prev != null && prev.elementType === TokenType.ERROR_ELEMENT || childNodeType === TokenType.ERROR_ELEMENT) {
                            return bracketsAlignment
                        }

                        if (childNodeType === LPAR || childNodeType === RPAR) {
                            return bracketsAlignment
                        }

                        return null
                    }
                }

            parentType == KtNodeTypes.TYPE_CONSTRAINT_LIST ->
                createAlignmentStrategy(true, getAlignment())

            else ->
                getNullAlignmentStrategy()
        }
    }


    private fun buildSubBlock(
        child: ASTNode,
        alignmentStrategy: CommonAlignmentStrategy,
        wrappingStrategy: WrappingStrategy,
        overrideChildren: Sequence<ASTNode>? = null
    ): ASTBlock {
        val childWrap = wrappingStrategy(child)

        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.elementType === KtNodeTypes.OPERATION_REFERENCE) {
            val operationNode = child.firstChildNode
            if (operationNode != null) {
                return createBlock(
                    operationNode,
                    alignmentStrategy,
                    createChildIndent(child),
                    childWrap,
                    settings,
                    spacingBuilder,
                    overrideChildren
                )
            }
        }

        return createBlock(child, alignmentStrategy, createChildIndent(child), childWrap, settings, spacingBuilder, overrideChildren)
    }

    private fun buildSubBlocks(): List<ASTBlock> {
        val childrenAlignmentStrategy = getChildrenAlignmentStrategy()
        val wrappingStrategy = getWrappingStrategy()

        val childNodes = when {
            overrideChildren != null -> overrideChildren.asSequence()
            node.elementType == KtNodeTypes.BINARY_EXPRESSION -> {
                val binaryExpressionChildren = mutableListOf<ASTNode>()
                collectBinaryExpressionChildren(node, binaryExpressionChildren)
                binaryExpressionChildren.asSequence()
            }
            else -> node.children()
        }

        return childNodes
            .filter { it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE }
            .flatMap { buildSubBlocksForChildNode(it, childrenAlignmentStrategy, wrappingStrategy) }
            .toList()
    }

    private fun buildSubBlocksForChildNode(
        node: ASTNode,
        childrenAlignmentStrategy: CommonAlignmentStrategy,
        wrappingStrategy: WrappingStrategy
    ): Sequence<ASTBlock> {
        if (node.elementType == KtNodeTypes.FUN && false /* TODO fix tests and restore */) {
            val filteredChildren = node.children().filter {
                it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE
            }
            val significantChildren = filteredChildren.dropWhile { it.elementType == KtTokens.EOL_COMMENT }
            val funIndent = extractIndent(significantChildren.first())
            val eolComments = filteredChildren.takeWhile {
                it.elementType == KtTokens.EOL_COMMENT && extractIndent(it) != funIndent
            }.toList()
            val remainingChildren = filteredChildren.drop(eolComments.size)

            val blocks = eolComments.map { buildSubBlock(it, childrenAlignmentStrategy, wrappingStrategy) } +
                    sequenceOf(buildSubBlock(node, childrenAlignmentStrategy, wrappingStrategy, remainingChildren))
            val blockList = blocks.toList()
            return blockList.asSequence()
        }

        return sequenceOf(buildSubBlock(node, childrenAlignmentStrategy, wrappingStrategy))
    }

    private fun collectBinaryExpressionChildren(node: ASTNode, result: MutableList<ASTNode>) {
        for (child in node.children()) {
            if (child.elementType == KtNodeTypes.BINARY_EXPRESSION) {
                collectBinaryExpressionChildren(child, result)
            } else {
                result.add(child)
            }
        }
    }

    private fun getWrappingStrategy(): WrappingStrategy {
        val commonSettings = settings.kotlinCommonSettings
        val elementType = node.elementType
        val parentElementType = node.treeParent?.elementType
        val nodePsi = node.psi

        when {
            elementType === KtNodeTypes.VALUE_ARGUMENT_LIST -> {
                val wrapSetting = commonSettings.CALL_PARAMETERS_WRAP
                if ((wrapSetting == CommonCodeStyleSettings.WRAP_AS_NEEDED || wrapSetting == CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM) &&
                    !needWrapArgumentList(nodePsi)
                ) {
                    return ::noWrapping
                }
                return getWrappingStrategyForItemList(wrapSetting, KtNodeTypes.VALUE_ARGUMENT)
            }

            elementType === KtNodeTypes.VALUE_PARAMETER_LIST -> {
                if (parentElementType === KtNodeTypes.FUN ||
                    parentElementType === KtNodeTypes.PRIMARY_CONSTRUCTOR ||
                    parentElementType === KtNodeTypes.SECONDARY_CONSTRUCTOR) {
                    val wrap = Wrap.createWrap(commonSettings.METHOD_PARAMETERS_WRAP, false)
                    return { childElement ->
                        if (childElement.elementType === KtNodeTypes.VALUE_PARAMETER && !childElement.startsWithAnnotation())
                            wrap
                        else
                            null
                    }
                }
            }

            elementType === KtNodeTypes.SUPER_TYPE_LIST -> {
                val wrap = Wrap.createWrap(commonSettings.EXTENDS_LIST_WRAP, false)
                return { childElement -> if (childElement.psi is KtSuperTypeListEntry) wrap else null }
            }

            elementType === KtNodeTypes.CLASS_BODY ->
                return getWrappingStrategyForItemList(commonSettings.ENUM_CONSTANTS_WRAP, KtNodeTypes.ENUM_ENTRY)

            elementType === KtNodeTypes.MODIFIER_LIST -> {
                val parent = node.treeParent.psi
                when (parent) {
                    is KtParameter ->
                        return getWrappingStrategyForItemList(
                            commonSettings.PARAMETER_ANNOTATION_WRAP,
                            ANNOTATIONS,
                            !node.treeParent.isFirstParameter()
                        )
                    is KtClassOrObject ->
                        return getWrappingStrategyForItemList(
                            commonSettings.CLASS_ANNOTATION_WRAP,
                            ANNOTATIONS
                        )

                    is KtNamedFunction ->
                        return getWrappingStrategyForItemList(
                            commonSettings.METHOD_ANNOTATION_WRAP,
                            ANNOTATIONS
                        )

                    is KtProperty ->
                        return getWrappingStrategyForItemList(
                            if (parent.isLocal)
                                commonSettings.VARIABLE_ANNOTATION_WRAP
                            else
                                commonSettings.FIELD_ANNOTATION_WRAP,
                            ANNOTATIONS
                        )
                }
            }

            elementType === KtNodeTypes.VALUE_PARAMETER ->
                return wrapAfterAnnotation(commonSettings.PARAMETER_ANNOTATION_WRAP)

            nodePsi is KtClassOrObject ->
                return wrapAfterAnnotation(commonSettings.CLASS_ANNOTATION_WRAP)

            nodePsi is KtNamedFunction ->
                return wrap@{ childElement ->
                    getWrapAfterAnnotation(childElement, commonSettings.METHOD_ANNOTATION_WRAP)?.let {
                        return@wrap it
                    }
                    if (getPrevWithoutWhitespaceAndComments(childElement)?.elementType == KtTokens.EQ) {
                        return@wrap Wrap.createWrap(settings.kotlinCustomSettings.WRAP_EXPRESSION_BODY_FUNCTIONS, true)
                    }
                    null
                }

            nodePsi is KtProperty ->
                return wrap@{ childElement ->
                    val wrapSetting =
                        if (nodePsi.isLocal) commonSettings.VARIABLE_ANNOTATION_WRAP else commonSettings.FIELD_ANNOTATION_WRAP
                    getWrapAfterAnnotation(childElement, wrapSetting)?.let {
                        return@wrap it
                    }
                    if (getPrevWithoutWhitespaceAndComments(childElement)?.elementType == KtTokens.EQ) {
                        return@wrap Wrap.createWrap(settings.kotlinCommonSettings.ASSIGNMENT_WRAP, true)
                    }
                    null
                }

            nodePsi is KtBinaryExpression -> {
                if (nodePsi.operationToken == KtTokens.EQ) {
                    return { childElement ->
                        if (getPrevWithoutWhitespaceAndComments(childElement)?.elementType == KtNodeTypes.OPERATION_REFERENCE) {
                            Wrap.createWrap(settings.kotlinCommonSettings.ASSIGNMENT_WRAP, true)
                        } else {
                            null
                        }
                    }
                }
                if (nodePsi.operationToken == KtTokens.ELVIS) {
                    return { childElement ->
                        if (childElement.elementType == KtNodeTypes.OPERATION_REFERENCE) {
                            Wrap.createWrap(settings.kotlinCustomSettings.WRAP_ELVIS_EXPRESSIONS, true)
                        } else {
                            null
                        }
                    }
                }
                return ::noWrapping
            }
        }

        return ::noWrapping
    }
}

private fun ASTNode.startsWithAnnotation() = firstChildNode?.firstChildNode?.elementType == KtNodeTypes.ANNOTATION_ENTRY

private fun ASTNode.isFirstParameter(): Boolean = treePrev?.elementType == KtTokens.LPAR

private fun wrapAfterAnnotation(wrapType: Int): WrappingStrategy {
    return { childElement -> getWrapAfterAnnotation(childElement, wrapType) }
}

private fun getWrapAfterAnnotation(childElement: ASTNode, wrapType: Int): Wrap? {
    if (childElement.elementType in COMMENTS) return null
    var prevLeaf = childElement.treePrev
    while (prevLeaf?.elementType == TokenType.WHITE_SPACE) {
        prevLeaf = prevLeaf.treePrev
    }
    if (prevLeaf?.elementType == KtNodeTypes.MODIFIER_LIST) {
        if (prevLeaf?.lastChildNode?.elementType in ANNOTATIONS) {
            return Wrap.createWrap(wrapType, true)
        }
    }
    return null
}

fun needWrapArgumentList(psi: PsiElement): Boolean {
    val args = (psi as? KtValueArgumentList)?.arguments
    return args?.singleOrNull()?.getArgumentExpression() !is KtObjectLiteralExpression
}

private fun hasLineBreakBefore(node: ASTNode): Boolean {
    val prevSibling = node.leaves(false)
        .dropWhile { it.psi is PsiComment }
        .firstOrNull()
    return prevSibling?.elementType == TokenType.WHITE_SPACE && prevSibling?.textContains('\n') == true
}

fun NodeIndentStrategy.PositionStrategy.continuationIf(
    option: (KotlinCodeStyleSettings) -> Boolean,
    indentFirst: Boolean = false
): NodeIndentStrategy {
    return set { settings ->
        if (option(settings.kotlinCustomSettings)) {
            if (indentFirst)
                Indent.getContinuationIndent()
            else
                Indent.getContinuationWithoutFirstIndent()
        } else
            Indent.getNormalIndent()
    }
}

private val INDENT_RULES = arrayOf(
    strategy("No indent for braces in blocks")
        .within(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY, KtNodeTypes.FUNCTION_LITERAL)
        .forType(RBRACE, LBRACE)
        .set(Indent.getNoneIndent()),

    strategy("Indent for block content")
        .within(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY, KtNodeTypes.FUNCTION_LITERAL)
        .notForType(RBRACE, LBRACE, KtNodeTypes.BLOCK)
        .set(Indent.getNormalIndent(false)),

    strategy("Indent for property accessors")
        .within(KtNodeTypes.PROPERTY).forType(KtNodeTypes.PROPERTY_ACCESSOR)
        .set(Indent.getNormalIndent()),

    strategy("For a single statement in 'for'")
        .within(KtNodeTypes.BODY).notForType(KtNodeTypes.BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("For the entry in when")
        .forType(KtNodeTypes.WHEN_ENTRY)
        .set(Indent.getNormalIndent()),

    strategy("For single statement in THEN and ELSE")
        .within(KtNodeTypes.THEN, KtNodeTypes.ELSE).notForType(KtNodeTypes.BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("Expression body")
        .within(KtNodeTypes.FUN)
        .forElement {
            it.psi is KtExpression && it.psi !is KtBlockExpression
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    strategy("If condition")
        .within(KtNodeTypes.CONDITION)
        .set { settings ->
            val indentType = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_IF_CONDITIONS)
                Indent.Type.CONTINUATION
            else
                Indent.Type.NORMAL
            Indent.getIndent(indentType, false, true)
        },

    strategy("Property accessor expression body")
        .within(KtNodeTypes.PROPERTY_ACCESSOR)
        .forElement {
            it.psi is KtExpression && it.psi !is KtBlockExpression
        }
        .set(Indent.getNormalIndent()),

    strategy("Property initializer")
        .within(KtNodeTypes.PROPERTY)
        .forElement {
            it.psi is KtExpression
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Indent for parts")
        .within(KtNodeTypes.PROPERTY, KtNodeTypes.FUN, KtNodeTypes.DESTRUCTURING_DECLARATION, KtNodeTypes.SECONDARY_CONSTRUCTOR)
        .notForType(
            KtNodeTypes.BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, CONSTRUCTOR_KEYWORD, KtTokens.RPAR,
            KtTokens.EOL_COMMENT
        )
        .set(Indent.getContinuationWithoutFirstIndent()),

    strategy("Chained calls")
        .within(QUALIFIED_EXPRESSIONS)
        .notForType(KtTokens.DOT, KtTokens.SAFE_ACCESS)
        .forElement { it.treeParent.firstChildNode != it }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS),

    strategy("Colon of delegation list")
        .within(KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION)
        .forType(KtTokens.COLON)
        .set(Indent.getNormalIndent(false)),

    strategy("Delegation list")
        .within(KtNodeTypes.SUPER_TYPE_LIST, KtNodeTypes.INITIALIZER_LIST)
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS, indentFirst = true),

    strategy("Indices")
        .within(KtNodeTypes.INDICES)
        .notForType(KtTokens.RBRACKET)
        .set(Indent.getContinuationIndent(false)),

    strategy("Binary expressions")
        .within(BINARY_EXPRESSIONS)
        .forElement { node -> !node.suppressBinaryExpressionIndent() }
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    strategy("Parenthesized expression")
        .within(KtNodeTypes.PARENTHESIZED)
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    strategy("Opening parenthesis for conditions")
        .forType(LPAR)
        .within(KtNodeTypes.IF, KtNodeTypes.WHEN_ENTRY, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE)
        .set(Indent.getContinuationWithoutFirstIndent(true)),

    strategy("Closing parenthesis for conditions")
        .forType(RPAR)
        .forElement { node -> !hasErrorElementBefore(node) }
        .within(KtNodeTypes.IF, KtNodeTypes.WHEN_ENTRY, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE)
        .set(Indent.getNoneIndent()),

    strategy("Closing parenthesis for incomplete conditions")
        .forType(RPAR)
        .forElement { node -> hasErrorElementBefore(node) }
        .within(KtNodeTypes.IF, KtNodeTypes.WHEN_ENTRY, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE)
        .set(Indent.getContinuationWithoutFirstIndent()),

    strategy("KDoc comment indent")
        .within(KDOC_CONTENT)
        .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
        .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

    strategy("Block in when entry")
        .within(KtNodeTypes.WHEN_ENTRY)
        .notForType(
            KtNodeTypes.BLOCK,
            KtNodeTypes.WHEN_CONDITION_EXPRESSION,
            KtNodeTypes.WHEN_CONDITION_IN_RANGE,
            KtNodeTypes.WHEN_CONDITION_IS_PATTERN,
            ELSE_KEYWORD,
            ARROW
        )
        .set(Indent.getNormalIndent()),

    strategy("Parameter list")
        .within(KtNodeTypes.VALUE_PARAMETER_LIST)
        .forElement { it.elementType == KtNodeTypes.VALUE_PARAMETER && it.psi.prevSibling != null }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS, indentFirst = true),

    strategy("Where clause")
        .within(KtNodeTypes.CLASS, KtNodeTypes.FUN, KtNodeTypes.PROPERTY)
        .forType(KtTokens.WHERE_KEYWORD)
        .set(Indent.getContinuationIndent()),

    strategy("Array literals")
        .within(KtNodeTypes.COLLECTION_LITERAL_EXPRESSION)
        .notForType(LBRACKET, RBRACKET)
        .set(Indent.getNormalIndent()),

    strategy("Type aliases")
        .within(KtNodeTypes.TYPEALIAS)
        .notForType(
            KtTokens.TYPE_ALIAS_KEYWORD, KtTokens.EOL_COMMENT, KtNodeTypes.MODIFIER_LIST, KtTokens.BLOCK_COMMENT,
            KtTokens.DOC_COMMENT
        )
        .set(Indent.getContinuationIndent()),

    strategy("Default parameter values")
        .within(KtNodeTypes.VALUE_PARAMETER)
        .forElement { node -> node.psi != null && node.psi == (node.psi.parent as? KtParameter)?.defaultValue }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true)
)


private fun getOperationType(node: ASTNode): IElementType? =
    node.findChildByType(KtNodeTypes.OPERATION_REFERENCE)?.firstChildNode?.elementType

fun hasErrorElementBefore(node: ASTNode): Boolean {
    val prevSibling = getPrevWithoutWhitespace(node) ?: return false
    if (prevSibling.elementType == TokenType.ERROR_ELEMENT) return true
    val lastChild = TreeUtil.getLastChild(prevSibling)
    return lastChild?.elementType == TokenType.ERROR_ELEMENT
}

/**
 * Suppress indent for binary expressions when there is a block higher in the tree that forces
 * its indent to children ('if' condition or elvis).
 */
private fun ASTNode.suppressBinaryExpressionIndent(): Boolean {
    var psi = psi.parent as? KtBinaryExpression ?: return false
    while (psi.parent is KtBinaryExpression) {
        psi = psi.parent as KtBinaryExpression
    }
    return psi.parent?.node?.elementType == KtNodeTypes.CONDITION || psi.operationToken == KtTokens.ELVIS
}

private fun getAlignmentForChildInParenthesis(
    shouldAlignChild: Boolean, parameter: IElementType, delimiter: IElementType,
    shouldAlignParenthesis: Boolean, openBracket: IElementType, closeBracket: IElementType
): CommonAlignmentStrategy {
    val parameterAlignment = if (shouldAlignChild) Alignment.createAlignment() else null
    val bracketsAlignment = if (shouldAlignParenthesis) Alignment.createAlignment() else null

    return object : CommonAlignmentStrategy() {
        override fun getAlignment(node: ASTNode): Alignment? {
            val childNodeType = node.elementType

            val prev = getPrevWithoutWhitespace(node)
            if (prev != null && prev.elementType === TokenType.ERROR_ELEMENT || childNodeType === TokenType.ERROR_ELEMENT) {
                // Prefer align to parameters on incomplete code (case of line break after comma, when next parameters is absent)
                return parameterAlignment
            }

            if (childNodeType === openBracket || childNodeType === closeBracket) {
                return bracketsAlignment
            }

            if (childNodeType === parameter || childNodeType === delimiter) {
                return parameterAlignment
            }

            return null
        }
    }
}

private fun getPrevWithoutWhitespace(pNode: ASTNode): ASTNode? {
    return pNode.siblings(forward = false).firstOrNull { it.elementType != TokenType.WHITE_SPACE }
}

private fun getPrevWithoutWhitespaceAndComments(pNode: ASTNode): ASTNode? {
    return pNode.siblings(forward = false).firstOrNull {
        it.elementType != TokenType.WHITE_SPACE && it.elementType !in COMMENTS
    }
}

private fun getWrappingStrategyForItemList(wrapType: Int, itemType: IElementType, wrapFirstElement: Boolean = false): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, wrapFirstElement)
    return { childElement -> if (childElement.elementType === itemType) itemWrap else null }
}

private fun getWrappingStrategyForItemList(wrapType: Int, itemTypes: TokenSet, wrapFirstElement: Boolean = false): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, wrapFirstElement)
    return { childElement ->
        val thisType = childElement.elementType
        val prevType = getPrevWithoutWhitespace(childElement)?.elementType
        if (thisType in itemTypes || prevType in itemTypes &&
            thisType != KtTokens.EOL_COMMENT && prevType != KtTokens.EOL_COMMENT)
            itemWrap
        else
            null
    }
}

private fun List<ASTBlock>.indexOfBlockWithType(tokenSet: TokenSet): Int {
    return indexOfFirst { block -> block.node?.elementType in tokenSet }
}

private fun extractIndent(node: ASTNode): String {
    val prevNode = node.treePrev
    if (prevNode?.elementType != TokenType.WHITE_SPACE)
        return ""
    return prevNode.text.substringAfterLast("\n", prevNode.text)
}
