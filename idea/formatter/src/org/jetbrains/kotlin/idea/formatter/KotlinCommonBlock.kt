/*
 * Copyright 2000-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.NodeIndentStrategy.Companion.strategy
import org.jetbrains.kotlin.idea.util.getLineCount
import org.jetbrains.kotlin.idea.util.requireNode
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

private val QUALIFIED_OPERATION = TokenSet.create(DOT, SAFE_ACCESS)
private val QUALIFIED_EXPRESSIONS = TokenSet.create(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)
private val ELVIS_SET = TokenSet.create(ELVIS)
private val QUALIFIED_EXPRESSIONS_WITHOUT_WRAP = TokenSet.create(IMPORT_DIRECTIVE, PACKAGE_DIRECTIVE)

private const val KDOC_COMMENT_INDENT = 1

private val BINARY_EXPRESSIONS = TokenSet.create(BINARY_EXPRESSION, BINARY_WITH_TYPE, IS_EXPRESSION)
private val KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC, KDocElementTypes.KDOC_SECTION, KDocElementTypes.KDOC_TAG)

private val CODE_BLOCKS = TokenSet.create(BLOCK, CLASS_BODY, FUNCTION_LITERAL)

private val ALIGN_FOR_BINARY_OPERATIONS = TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, ELVIS, LT, GT, LTEQ, GTEQ, ANDAND, OROR)
private val ANNOTATIONS = TokenSet.create(ANNOTATION_ENTRY, ANNOTATION)

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

    fun isIncomplete(): Boolean {
        if (isIncompleteInSuper()) {
            return true
        }

        // An incomplete declaration is the reason when modifier list can become a class body child, otherwise
        // it's going to be a declaration child.
        return node.elementType == MODIFIER_LIST && node.treeParent?.elementType == CLASS_BODY
    }

    fun buildChildren(): List<Block> {
        if (mySubBlocks != null) {
            return mySubBlocks!!
        }

        var nodeSubBlocks = buildSubBlocks()

        if (node.elementType in QUALIFIED_EXPRESSIONS) {
            nodeSubBlocks = splitSubBlocksOnDot(nodeSubBlocks)
        } else {
            val psi = node.psi
            if (psi is KtBinaryExpression && psi.operationToken == ELVIS) {
                nodeSubBlocks = splitSubBlocksOnElvis(nodeSubBlocks)
            }
        }

        mySubBlocks = nodeSubBlocks

        return nodeSubBlocks
    }

    private fun ASTNode.isQualifiedNode(strict: Boolean = false): Boolean {
        if (strict) return elementType in QUALIFIED_EXPRESSIONS

        var currentNode: ASTNode? = this
        while (currentNode != null) {
            if (currentNode.elementType in QUALIFIED_EXPRESSIONS) return true
            if (currentNode.elementType != PARENTHESIZED && currentNode.psi?.safeAs<KtPostfixExpression>()
                    ?.operationToken != EXCLEXCL
            ) return false
            currentNode = currentNode.treeParent
        }

        return false
    }

    private fun splitSubBlocksOnDot(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        if (node.treeParent?.isQualifiedNode() == true) return nodeSubBlocks
        if (!canWrapCallChain(node)) return nodeSubBlocks

        val operationBlockIndex = nodeSubBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
        if (operationBlockIndex == -1) return nodeSubBlocks

        val block = nodeSubBlocks.first()
        val wrap = createWrapForQualifiedExpression(node)
        val indent = createIndentForQualifiedExpression(block)
        val newBlock = block.processBlock(indent, wrap)
        return nodeSubBlocks.replaceBlock(newBlock, 0).splitAtIndex(operationBlockIndex, indent, wrap)
    }

    private fun ASTBlock.processBlock(indent: Indent, wrap: Wrap?): ASTBlock {
        val currentNode = requireNode()

        @Suppress("UNCHECKED_CAST")
        val subBlocks = subBlocks as List<ASTBlock>
        val elementType = currentNode.elementType
        val index = when (elementType) {
            PARENTHESIZED -> subBlocks.indexOfFirst { block ->
                val type = block.requireNode().elementType
                type != LPAR && type !in WHITE_SPACE_OR_COMMENT_BIT_SET
            }
            POSTFIX_EXPRESSION, in QUALIFIED_EXPRESSIONS -> 0
            else -> return this
        }

        val resultWrap = if (currentNode.wrapForFirstCallInChainIsAllowed)
            wrap.takeIf { elementType != PARENTHESIZED } ?: createWrapForQualifiedExpression(currentNode)
        else
            null

        val newBlock = subBlocks.elementAt(index).processBlock(indent, resultWrap)
        return subBlocks.replaceBlock(newBlock, index).let {
            val operationIndex = subBlocks.indexOfBlockWithType(QUALIFIED_OPERATION)
            if (operationIndex != -1)
                it.splitAtIndex(operationIndex, indent, resultWrap)
            else
                it
        }.wrapToBlock(currentNode, this)
    }

    private fun List<ASTBlock>.replaceBlock(block: ASTBlock, index: Int = 0): List<ASTBlock> = toMutableList().apply { this[index] = block }

    private val ASTNode.wrapForFirstCallInChainIsAllowed
        get() = settings.kotlinCommonSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN ||
                settings.kotlinCommonSettings.METHOD_CALL_CHAIN_WRAP != CommonCodeStyleSettings.WRAP_AS_NEEDED ||
                firstChildNode?.isQualifiedNode(true) == true

    private fun createWrapForQualifiedExpression(node: ASTNode): Wrap? = if (node.wrapForFirstCallInChainIsAllowed)
        Wrap.createWrap(
            settings.kotlinCommonSettings.METHOD_CALL_CHAIN_WRAP,
            settings.kotlinCommonSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN
        )
    else
        null

    private fun createIndentForQualifiedExpression(block: ASTBlock): Indent {
        // enforce indent to children when there's a line break before the dot in any call in the chain (meaning that
        // the call chain following that call is indented)
        val enforceIndentToChildren = anyCallInCallChainIsWrapped(block)

        val indentType = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS) {
            if (enforceIndentToChildren) Indent.Type.CONTINUATION else Indent.Type.CONTINUATION_WITHOUT_FIRST
        } else {
            Indent.Type.NORMAL
        }

        return Indent.getIndent(
            indentType, false,
            enforceIndentToChildren
        )
    }

    private fun List<ASTBlock>.wrapToBlock(
        anchor: ASTNode?,
        parentBlock: ASTBlock?
    ): ASTBlock = splitAtIndex(0, null, null, anchor, parentBlock).single()

    private fun List<ASTBlock>.splitAtIndex(
        index: Int,
        indent: Indent?,
        wrap: Wrap?,
        anchor: ASTNode? = null,
        parentBlock: ASTBlock? = null
    ): List<ASTBlock> {
        val operationBlock = this[index]
        val createParentSyntheticSpacingBlock: (ASTNode) -> ASTBlock = if (parentBlock != null) {
            { parentBlock }
        } else {
            {
                val parent = it.treeParent ?: node
                val skipOperationNodeParent = if (parent.elementType === OPERATION_REFERENCE) {
                    parent.treeParent ?: parent
                } else {
                    parent
                }
                createSyntheticSpacingNodeBlock(skipOperationNodeParent)
            }
        }
        val operationSyntheticBlock = SyntheticKotlinBlock(
            anchor ?: operationBlock.requireNode(),
            subList(index, size),
            null, indent, wrap, spacingBuilder, createParentSyntheticSpacingBlock
        )

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
        val node = astBlock.requireNode()
        val lastChildElementType = node.lastChildNode?.elementType
        return node.isQualifiedNode() && (lastChildElementType == CALL_EXPRESSION || lastChildElementType == REFERENCE_EXPRESSION)
    }

    private fun canWrapCallChain(node: ASTNode): Boolean {
        val callChainParent = node.parents().firstOrNull { !it.isQualifiedNode() } ?: return true
        return callChainParent.elementType !in QUALIFIED_EXPRESSIONS_WITHOUT_WRAP
    }

    private fun splitSubBlocksOnElvis(nodeSubBlocks: List<ASTBlock>): List<ASTBlock> {
        val elvisIndex = nodeSubBlocks.indexOfBlockWithType(ELVIS_SET)
        if (elvisIndex >= 0) {
            val indent = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ELVIS) {
                Indent.getContinuationIndent()
            } else {
                Indent.getNormalIndent()
            }

            return nodeSubBlocks.splitAtIndex(
                elvisIndex,
                indent,
                null
            )
        }
        return nodeSubBlocks
    }

    private fun createChildIndent(child: ASTNode): Indent? {
        val childParent = child.treeParent
        val childType = child.elementType

        if (childParent != null && isInCodeChunk(childParent)) {
            return Indent.getNoneIndent()
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

            if (parentType === VALUE_PARAMETER_LIST || parentType === VALUE_ARGUMENT_LIST) {
                val prev = getPrevWithoutWhitespace(child)
                if (childType === RPAR && (prev == null || prev.elementType !== TokenType.ERROR_ELEMENT)) {
                    return Indent.getNoneIndent()
                }

                return if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)
                    Indent.getContinuationWithoutFirstIndent()
                else
                    Indent.getNormalIndent()
            }

            if (parentType === TYPE_PARAMETER_LIST || parentType === TYPE_ARGUMENT_LIST) {
                return Indent.getContinuationWithoutFirstIndent()
            }
        }

        return Indent.getNoneIndent()
    }

    private fun isInCodeChunk(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false

        if (node.elementType != BLOCK) {
            return false
        }

        val parentType = parent.elementType
        return parentType == SCRIPT
                || parentType == BLOCK_CODE_FRAGMENT
                || parentType == EXPRESSION_CODE_FRAGMENT
                || parentType == TYPE_CODE_FRAGMENT
    }

    fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType

        if (isInCodeChunk(node)) {
            return ChildAttributes(Indent.getNoneIndent(), null)
        }

        if (type == IF) {
            val elseBlock = mySubBlocks?.getOrNull(newChildIndex)
            if (elseBlock != null && elseBlock.requireNode().elementType == ELSE_KEYWORD) {
                return ChildAttributes.DELEGATE_TO_NEXT_CHILD
            }
        }

        if (newChildIndex > 0) {
            val prevBlock = mySubBlocks?.get(newChildIndex - 1)
            if (prevBlock?.node?.elementType == MODIFIER_LIST) {
                return ChildAttributes(Indent.getNoneIndent(), null)
            }
        }

        return when (type) {
            in CODE_BLOCKS, WHEN, IF, FOR, WHILE, DO_WHILE, WHEN_ENTRY -> ChildAttributes(
                Indent.getNormalIndent(),
                null
            )

            TRY -> ChildAttributes(Indent.getNoneIndent(), null)

            in QUALIFIED_EXPRESSIONS -> ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)

            VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST -> {
                val subBlocks = getSubBlocks()
                if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                    val block = subBlocks[newChildIndex]
                    ChildAttributes(block.indent, block.alignment)
                } else {
                    val indent =
                        if ((type == VALUE_PARAMETER_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS) ||
                            (type == VALUE_ARGUMENT_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_ARGUMENT_LISTS)
                        ) {
                            Indent.getNormalIndent()
                        } else {
                            Indent.getContinuationIndent()
                        }
                    ChildAttributes(indent, null)
                }
            }

            DOC_COMMENT -> ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null)

            PARENTHESIZED -> getSuperChildAttributes(newChildIndex)

            else -> {
                val blocks = getSubBlocks()
                if (newChildIndex != 0) {
                    val isIncomplete = if (newChildIndex < blocks.size) blocks[newChildIndex - 1].isIncomplete else isIncompleteInSuper()
                    if (isIncomplete) {
                        if (blocks.size == newChildIndex && !settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES) {
                            val lastInParent = blocks.last()
                            if (lastInParent is ASTBlock && lastInParent.node?.elementType in ALL_ASSIGNMENTS) {
                                return ChildAttributes(Indent.getNormalIndent(), null)
                            }
                        }

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
            parentType === VALUE_PARAMETER_LIST ->
                getAlignmentForChildInParenthesis(
                    kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS, VALUE_PARAMETER, COMMA,
                    kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR
                )

            parentType === VALUE_ARGUMENT_LIST ->
                getAlignmentForChildInParenthesis(
                    kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, VALUE_ARGUMENT, COMMA,
                    kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR
                )

            parentType === WHEN ->
                getAlignmentForCaseBranch(kotlinCustomSettings.ALIGN_IN_COLUMNS_CASE_BRANCH)

            parentType === WHEN_ENTRY ->
                alignmentStrategy

            parentType in BINARY_EXPRESSIONS && getOperationType(node) in ALIGN_FOR_BINARY_OPERATIONS ->
                createAlignmentStrategy(kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, getAlignment())

            parentType === SUPER_TYPE_LIST ->
                createAlignmentStrategy(kotlinCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST, getAlignment())

            parentType === PARENTHESIZED ->
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

            parentType == TYPE_CONSTRAINT_LIST ->
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
        if (child.elementType === OPERATION_REFERENCE) {
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
            node.elementType == BINARY_EXPRESSION -> {
                val binaryExpression = node.psi as? KtBinaryExpression
                if (binaryExpression != null && ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)) {
                    node.children()
                } else {
                    val binaryExpressionChildren = mutableListOf<ASTNode>()
                    collectBinaryExpressionChildren(node, binaryExpressionChildren)
                    binaryExpressionChildren.asSequence()
                }
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
        if (node.elementType == FUN && false /* TODO fix tests and restore */) {
            val filteredChildren = node.children().filter {
                it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE
            }
            val significantChildren = filteredChildren.dropWhile { it.elementType == EOL_COMMENT }
            val funIndent = extractIndent(significantChildren.first())
            val eolComments = filteredChildren.takeWhile {
                it.elementType == EOL_COMMENT && extractIndent(it) != funIndent
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
            if (child.elementType == BINARY_EXPRESSION) {
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
            elementType === VALUE_ARGUMENT_LIST -> {
                val wrapSetting = commonSettings.CALL_PARAMETERS_WRAP
                if ((wrapSetting == CommonCodeStyleSettings.WRAP_AS_NEEDED || wrapSetting == CommonCodeStyleSettings.WRAP_ON_EVERY_ITEM) &&
                    !needWrapArgumentList(nodePsi)
                ) {
                    return ::noWrapping
                }
                return getWrappingStrategyForItemList(wrapSetting, VALUE_ARGUMENT)
            }

            elementType === VALUE_PARAMETER_LIST -> {
                if (parentElementType === FUN ||
                    parentElementType === PRIMARY_CONSTRUCTOR ||
                    parentElementType === SECONDARY_CONSTRUCTOR
                ) {
                    val wrap = Wrap.createWrap(commonSettings.METHOD_PARAMETERS_WRAP, false)
                    val withTrailingComma = node.withTrailingComma
                    return { childElement ->
                        val childElementType = childElement.elementType
                        if (withTrailingComma && (childElementType === RPAR || getPrevWithoutWhitespaceAndComments(childElement)?.elementType === LPAR))
                            Wrap.createWrap(WrapType.ALWAYS, true)
                        else
                            wrap.takeIf { childElementType === VALUE_PARAMETER }
                    }
                }
            }

            elementType === SUPER_TYPE_LIST -> {
                val wrap = Wrap.createWrap(commonSettings.EXTENDS_LIST_WRAP, false)
                return { childElement -> if (childElement.psi is KtSuperTypeListEntry) wrap else null }
            }

            elementType === CLASS_BODY ->
                return getWrappingStrategyForItemList(commonSettings.ENUM_CONSTANTS_WRAP, ENUM_ENTRY)

            elementType === MODIFIER_LIST -> {
                when (val parent = node.treeParent.psi) {
                    is KtParameter ->
                        return getWrappingStrategyForItemList(
                            commonSettings.PARAMETER_ANNOTATION_WRAP,
                            ANNOTATIONS,
                            !node.treeParent.isFirstParameter()
                        )
                    is KtClassOrObject, is KtTypeAlias ->
                        return getWrappingStrategyForItemList(
                            commonSettings.CLASS_ANNOTATION_WRAP,
                            ANNOTATIONS
                        )

                    is KtNamedFunction, is KtSecondaryConstructor ->
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

            elementType === VALUE_PARAMETER ->
                return wrapAfterAnnotation(commonSettings.PARAMETER_ANNOTATION_WRAP)

            nodePsi is KtClassOrObject || nodePsi is KtTypeAlias ->
                return wrapAfterAnnotation(commonSettings.CLASS_ANNOTATION_WRAP)

            nodePsi is KtNamedFunction || nodePsi is KtSecondaryConstructor ->
                return wrap@{ childElement ->
                    getWrapAfterAnnotation(childElement, commonSettings.METHOD_ANNOTATION_WRAP)?.let {
                        return@wrap it
                    }
                    if (getPrevWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
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
                    if (getPrevWithoutWhitespaceAndComments(childElement)?.elementType == EQ) {
                        return@wrap Wrap.createWrap(settings.kotlinCommonSettings.ASSIGNMENT_WRAP, true)
                    }
                    null
                }

            nodePsi is KtBinaryExpression -> {
                if (nodePsi.operationToken == EQ) {
                    return { childElement ->
                        if (getPrevWithoutWhitespaceAndComments(childElement)?.elementType == OPERATION_REFERENCE) {
                            Wrap.createWrap(settings.kotlinCommonSettings.ASSIGNMENT_WRAP, true)
                        } else {
                            null
                        }
                    }
                }
                if (nodePsi.operationToken == ELVIS && nodePsi.getStrictParentOfType<KtStringTemplateExpression>() == null) {
                    return { childElement ->
                        if (childElement.elementType == OPERATION_REFERENCE && (childElement.psi as? KtOperationReferenceExpression)
                                ?.operationSignTokenType == ELVIS
                        ) {
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

    private val ASTNode.withTrailingComma: Boolean
        get() = when {
            lastChildNode?.let { getPrevWithoutWhitespaceAndComments(it) }?.elementType === COMMA -> true
            settings.kotlinCustomSettings.ALLOW_TRAILING_COMMA -> psi?.getLineCount()?.let { it > 1 } == true
            else -> false
        }
}

private fun ASTNode.isFirstParameter(): Boolean = treePrev?.elementType == LPAR

private fun wrapAfterAnnotation(wrapType: Int): WrappingStrategy {
    return { childElement -> getWrapAfterAnnotation(childElement, wrapType) }
}

private fun getWrapAfterAnnotation(childElement: ASTNode, wrapType: Int): Wrap? {
    if (childElement.elementType in COMMENTS) return null
    var prevLeaf = childElement.treePrev
    while (prevLeaf?.elementType == TokenType.WHITE_SPACE) {
        prevLeaf = prevLeaf.treePrev
    }
    if (prevLeaf?.elementType == MODIFIER_LIST) {
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

private fun hasDoubleLineBreakBefore(node: ASTNode): Boolean {
    val prevSibling = node.leaves(false).firstOrNull() ?: return false

    return prevSibling.text.count { it == '\n' } >= 2
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
        .within(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
        .forType(RBRACE, LBRACE)
        .set(Indent.getNoneIndent()),

    strategy("Indent for block content")
        .within(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
        .notForType(RBRACE, LBRACE, BLOCK)
        .set(Indent.getNormalIndent(false)),

    strategy("Indent for property accessors")
        .within(PROPERTY).forType(PROPERTY_ACCESSOR)
        .set(Indent.getNormalIndent()),

    strategy("For a single statement in 'for'")
        .within(BODY).notForType(BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("For WHEN content")
        .within(WHEN)
        .notForType(RBRACE, LBRACE, WHEN_KEYWORD)
        .set(Indent.getNormalIndent()),

    strategy("For single statement in THEN and ELSE")
        .within(THEN, ELSE).notForType(BLOCK)
        .set(Indent.getNormalIndent()),

    strategy("Expression body")
        .within(FUN)
        .forElement {
            (it.psi is KtExpression && it.psi !is KtBlockExpression)
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    strategy("Line comment at expression body position")
        .forElement { node ->
            val psi = node.psi
            val parent = psi.parent
            if (psi is PsiComment && parent is KtDeclarationWithInitializer) {
                psi.getNextSiblingIgnoringWhitespace() == parent.initializer
            } else {
                false
            }
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true),

    strategy("If condition")
        .within(CONDITION)
        .set { settings ->
            val indentType = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_IF_CONDITIONS)
                Indent.Type.CONTINUATION
            else
                Indent.Type.NORMAL
            Indent.getIndent(indentType, false, true)
        },

    strategy("Property accessor expression body")
        .within(PROPERTY_ACCESSOR)
        .forElement {
            it.psi is KtExpression && it.psi !is KtBlockExpression
        }
        .set(Indent.getNormalIndent()),

    strategy("Property initializer")
        .within(PROPERTY)
        .forElement {
            it.psi is KtExpression
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Destructuring declaration")
        .within(DESTRUCTURING_DECLARATION)
        .forElement {
            it.psi is KtExpression
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Assignment expressions")
        .within(BINARY_EXPRESSION)
        .within {
            val binaryExpression = it.psi as? KtBinaryExpression
                ?: return@within false

            return@within ALL_ASSIGNMENTS.contains(binaryExpression.operationToken)
        }
        .forElement {
            val psi = it.psi
            val binaryExpression = psi?.parent as? KtBinaryExpression
            binaryExpression?.right == psi
        }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES),

    strategy("Indent for parts")
        .within(PROPERTY, FUN, DESTRUCTURING_DECLARATION, SECONDARY_CONSTRUCTOR)
        .notForType(
            BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, CONSTRUCTOR_KEYWORD, RPAR,
            EOL_COMMENT
        )
        .set(Indent.getContinuationWithoutFirstIndent()),

    strategy("Chained calls")
        .within(QUALIFIED_EXPRESSIONS)
        .notForType(DOT, SAFE_ACCESS)
        .forElement { it.treeParent.firstChildNode != it }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_CHAINED_CALLS),

    strategy("Colon of delegation list")
        .within(CLASS, OBJECT_DECLARATION)
        .forType(COLON)
        .set(Indent.getNormalIndent(false)),

    strategy("Delegation list")
        .within(SUPER_TYPE_LIST)
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_SUPERTYPE_LISTS, indentFirst = true),

    strategy("Indices")
        .within(INDICES)
        .notForType(RBRACKET)
        .set(Indent.getContinuationIndent(false)),

    strategy("Binary expressions")
        .within(BINARY_EXPRESSIONS)
        .forElement { node ->
            !node.suppressBinaryExpressionIndent()
        }
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    strategy("Parenthesized expression")
        .within(PARENTHESIZED)
        .set(Indent.getContinuationWithoutFirstIndent(false)),

    strategy("Opening parenthesis for conditions")
        .forType(LPAR)
        .within(IF, WHEN_ENTRY, WHILE, DO_WHILE)
        .set(Indent.getContinuationWithoutFirstIndent(true)),

    strategy("Closing parenthesis for conditions")
        .forType(RPAR)
        .forElement { node -> !hasErrorElementBefore(node) }
        .within(IF, WHEN_ENTRY, WHILE, DO_WHILE)
        .set(Indent.getNoneIndent()),

    strategy("Closing parenthesis for incomplete conditions")
        .forType(RPAR)
        .forElement { node -> hasErrorElementBefore(node) }
        .within(IF, WHEN_ENTRY, WHILE, DO_WHILE)
        .set(Indent.getContinuationWithoutFirstIndent()),

    strategy("KDoc comment indent")
        .within(KDOC_CONTENT)
        .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
        .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

    strategy("Block in when entry")
        .within(WHEN_ENTRY)
        .notForType(
            BLOCK,
            WHEN_CONDITION_EXPRESSION,
            WHEN_CONDITION_IN_RANGE,
            WHEN_CONDITION_IS_PATTERN,
            ELSE_KEYWORD,
            ARROW
        )
        .set(Indent.getNormalIndent()),

    strategy("Parameter list")
        .within(VALUE_PARAMETER_LIST)
        .forElement { it.elementType == VALUE_PARAMETER && it.psi.prevSibling != null }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_IN_PARAMETER_LISTS, indentFirst = true),

    strategy("Where clause")
        .within(CLASS, FUN, PROPERTY)
        .forType(WHERE_KEYWORD)
        .set(Indent.getContinuationIndent()),

    strategy("Array literals")
        .within(COLLECTION_LITERAL_EXPRESSION)
        .notForType(LBRACKET, RBRACKET)
        .set(Indent.getNormalIndent()),

    strategy("Type aliases")
        .within(TYPEALIAS)
        .notForType(
            TYPE_ALIAS_KEYWORD, EOL_COMMENT, MODIFIER_LIST, BLOCK_COMMENT,
            DOC_COMMENT
        )
        .set(Indent.getContinuationIndent()),

    strategy("Default parameter values")
        .within(VALUE_PARAMETER)
        .forElement { node -> node.psi != null && node.psi == (node.psi.parent as? KtParameter)?.defaultValue }
        .continuationIf(KotlinCodeStyleSettings::CONTINUATION_INDENT_FOR_EXPRESSION_BODIES, indentFirst = true)
)


private fun getOperationType(node: ASTNode): IElementType? =
    node.findChildByType(OPERATION_REFERENCE)?.firstChildNode?.elementType

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
    return psi.parent?.node?.elementType == CONDITION || psi.operationToken == ELVIS
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
            val hasTrailingComma = childNodeType === closeBracket && prev?.elementType == COMMA

            if (hasTrailingComma && hasDoubleLineBreakBefore(node)) {
                // Prefer align to parameters on code with trailing comma (case of line break after comma, when before closing bracket there was a line break)
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
            thisType != EOL_COMMENT && prevType != EOL_COMMENT
        )
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
