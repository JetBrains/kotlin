/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.*
import com.intellij.formatting.alignment.AlignmentStrategy
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.NodeIndentStrategy.strategy
import org.jetbrains.kotlin.kdoc.lexer.KDocTokens
import org.jetbrains.kotlin.kdoc.parser.KDocElementTypes
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtDeclaration
import java.util.*

private val KDOC_COMMENT_INDENT = 1

private val BINARY_EXPRESSIONS = TokenSet.create(BINARY_EXPRESSION, BINARY_WITH_TYPE, IS_EXPRESSION)
private val QUALIFIED_OPERATION = TokenSet.create(DOT, SAFE_ACCESS)
private val ALIGN_FOR_BINARY_OPERATIONS = TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, ELVIS, LT, GT, LTEQ, GTEQ, ANDAND, OROR)

private val CODE_BLOCKS = TokenSet.create(BLOCK, CLASS_BODY, FUNCTION_LITERAL)

private val KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC, KDocElementTypes.KDOC_SECTION, KDocElementTypes.KDOC_TAG)

/**
 * @see Block for good JavaDoc documentation
 */
class KotlinBlock(
        node: ASTNode,
        private val myAlignmentStrategy: NodeAlignmentStrategy,
        private val myIndent: Indent?,
        wrap: Wrap?,
        private val mySettings: CodeStyleSettings,
        private val mySpacingBuilder: KotlinSpacingBuilder) : AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)) {

    private var mySubBlocks: List<Block>? = null

    override fun getIndent(): Indent? {
        return myIndent
    }

    override fun buildChildren(): List<Block> {
        if (mySubBlocks == null) {
            var nodeSubBlocks = buildSubBlocks() as ArrayList<Block>

            if (node.elementType === DOT_QUALIFIED_EXPRESSION || node.elementType === SAFE_ACCESS_EXPRESSION) {
                val operationBlockIndex = findNodeBlockIndex(nodeSubBlocks, QUALIFIED_OPERATION)
                if (operationBlockIndex != -1) {
                    // Create fake ".something" or "?.something" block here, so child indentation will be
                    // relative to it when it starts from new line (see Indent javadoc).

                    val operationBlock = nodeSubBlocks[operationBlockIndex]
                    val operationSyntheticBlock = SyntheticKotlinBlock(
                            (operationBlock as ASTBlock).node,
                            nodeSubBlocks.subList(operationBlockIndex, nodeSubBlocks.size),
                            null, operationBlock.getIndent(), null, mySpacingBuilder)

                    nodeSubBlocks = ContainerUtil.addAll(
                            ContainerUtil.newArrayList(nodeSubBlocks.subList(0, operationBlockIndex)),
                            operationSyntheticBlock)
                }
            }

            mySubBlocks = nodeSubBlocks
        }
        return mySubBlocks!!
    }

    private fun buildSubBlocks(): List<Block> {
        val blocks = ArrayList<Block>()

        val childrenAlignmentStrategy = getChildrenAlignmentStrategy()
        val wrappingStrategy = getWrappingStrategy()

        var child: ASTNode? = myNode.firstChildNode
        while (child != null) {
            val childType = child.elementType

            if (child.textRange.length == 0) {
                child = child.treeNext
                continue
            }

            if (childType === TokenType.WHITE_SPACE) {
                child = child.treeNext
                continue
            }

            blocks.add(buildSubBlock(child, childrenAlignmentStrategy, wrappingStrategy))
            child = child.treeNext
        }

        return blocks
    }

    private fun buildSubBlock(
            child: ASTNode,
            alignmentStrategy: NodeAlignmentStrategy,
            wrappingStrategy: WrappingStrategy): Block {
        val wrap = wrappingStrategy.getWrap(child.elementType)

        // Skip one sub-level for operators, so type of block node is an element type of operator
        if (child.elementType === OPERATION_REFERENCE) {
            val operationNode = child.firstChildNode
            if (operationNode != null) {
                return KotlinBlock(operationNode, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder)
            }
        }

        return KotlinBlock(child, alignmentStrategy, createChildIndent(child), wrap, mySettings, mySpacingBuilder)
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = mySpacingBuilder.getSpacing(this, child1, child2)

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        val type = node.elementType
        return when (type) {
            in CODE_BLOCKS, WHEN, IF, FOR, WHILE, DO_WHILE -> ChildAttributes(Indent.getNormalIndent(), null)

            TRY -> ChildAttributes(Indent.getNoneIndent(), null)

            DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION -> ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)

            VALUE_PARAMETER_LIST, VALUE_ARGUMENT_LIST -> {
                if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                    val block = subBlocks[newChildIndex]
                    ChildAttributes(block.indent, block.alignment)
                }
                else {
                    ChildAttributes(Indent.getContinuationIndent(), null)
                }
            }

            DOC_COMMENT -> ChildAttributes(Indent.getSpaceIndent(KDOC_COMMENT_INDENT), null)

            PARENTHESIZED -> super.getChildAttributes(newChildIndex)

            else -> {
                val blocks = subBlocks
                if (newChildIndex != 0) {
                    val isIncomplete = if (newChildIndex < blocks.size) blocks[newChildIndex - 1].isIncomplete else isIncomplete
                    if (isIncomplete) {
                        return super.getChildAttributes(newChildIndex)
                    }
                }

                ChildAttributes(Indent.getNoneIndent(), null)
            }
        }
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    private fun getWrappingStrategy(): WrappingStrategy {
        val commonSettings = mySettings.getCommonSettings(KotlinLanguage.INSTANCE)
        val elementType = myNode.elementType

        if (elementType === VALUE_ARGUMENT_LIST) {
            return getWrappingStrategyForItemList(commonSettings.CALL_PARAMETERS_WRAP, VALUE_ARGUMENT)
        }
        if (elementType === VALUE_PARAMETER_LIST) {
            val parentElementType = myNode.treeParent.elementType
            if (parentElementType === FUN || parentElementType === CLASS) {
                return getWrappingStrategyForItemList(commonSettings.METHOD_PARAMETERS_WRAP, VALUE_PARAMETER)
            }
        }

        return WrappingStrategy.NoWrapping
    }

    // Redefine list of strategies for some special elements
    // Propagate when alignment for ->
    private fun getChildrenAlignmentStrategy(): NodeAlignmentStrategy {
        val jetCommonSettings = mySettings.getCommonSettings(KotlinLanguage.INSTANCE)
        val jetSettings = mySettings.getCustomSettings(KotlinCodeStyleSettings::class.java)
        val parentType = myNode.elementType
        if (parentType === VALUE_PARAMETER_LIST) {
            return getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS, VALUE_PARAMETER, COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR)
        }
        else if (parentType === VALUE_ARGUMENT_LIST) {
            return getAlignmentForChildInParenthesis(
                    jetCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, VALUE_ARGUMENT, COMMA,
                    jetCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR)
        }
        else if (parentType === WHEN) {
            return getAlignmentForCaseBranch(jetSettings.ALIGN_IN_COLUMNS_CASE_BRANCH)
        }
        else if (parentType === WHEN_ENTRY) {
            return myAlignmentStrategy
        }
        else if (parentType in BINARY_EXPRESSIONS && getOperationType(node) in ALIGN_FOR_BINARY_OPERATIONS) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION, alignment)))
        }
        else if (parentType === SUPER_TYPE_LIST || parentType === INITIALIZER_LIST) {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(
                    createAlignment(jetCommonSettings.ALIGN_MULTILINE_EXTENDS_LIST, alignment)))
        }
        else if (parentType === PARENTHESIZED) {
            return object : NodeAlignmentStrategy() {
                private var bracketsAlignment: Alignment? = if (jetCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION) Alignment.createAlignment() else null

                override fun getAlignment(childNode: ASTNode): Alignment? {
                    val childNodeType = childNode.elementType
                    val prev = getPrevWithoutWhitespace(childNode)

                    if (prev != null && prev.elementType === TokenType.ERROR_ELEMENT || childNodeType === TokenType.ERROR_ELEMENT) {
                        return bracketsAlignment
                    }

                    if (childNodeType === LPAR || childNodeType === RPAR) {
                        return bracketsAlignment
                    }

                    return null
                }
            }
        }

        return NodeAlignmentStrategy.getNullStrategy()
    }
}

private val INDENT_RULES = arrayOf<NodeIndentStrategy>(
        strategy("No indent for braces in blocks")
                .`in`(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
                .forType(RBRACE, LBRACE)
                .set(Indent.getNoneIndent()),

        strategy("Indent for block content")
                .`in`(BLOCK, CLASS_BODY, FUNCTION_LITERAL)
                .notForType(RBRACE, LBRACE, BLOCK)
                .set(Indent.getNormalIndent(false)),

        strategy("Indent for property accessors")
                .`in`(PROPERTY).forType(PROPERTY_ACCESSOR)
                .set(Indent.getNormalIndent()),

        strategy("For a single statement in 'for'")
                .`in`(BODY).notForType(BLOCK)
                .set(Indent.getNormalIndent()),

        strategy("For the entry in when")
                .forType(WHEN_ENTRY)
                .set(Indent.getNormalIndent()),

        strategy("For single statement in THEN and ELSE")
                .`in`(THEN, ELSE).notForType(BLOCK)
                .set(Indent.getNormalIndent()),

        strategy("Indent for parts")
                .`in`(PROPERTY, FUN, DESTRUCTURING_DECLARATION)
                .notForType(BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD)
                .set(Indent.getContinuationWithoutFirstIndent()),

        strategy("Chained calls")
                .`in`(DOT_QUALIFIED_EXPRESSION, SAFE_ACCESS_EXPRESSION)
                .set(Indent.getContinuationWithoutFirstIndent(false)),

        strategy("Delegation list")
                .`in`(SUPER_TYPE_LIST, INITIALIZER_LIST)
                .set(Indent.getContinuationIndent(false)),

        strategy("Indices")
                .`in`(INDICES)
                .set(Indent.getContinuationIndent(false)),

        strategy("Binary expressions")
                .`in`(BINARY_EXPRESSIONS)
                .set(Indent.getContinuationWithoutFirstIndent(false)),

        strategy("Parenthesized expression")
                .`in`(PARENTHESIZED)
                .set(Indent.getContinuationWithoutFirstIndent(false)),

        strategy("KDoc comment indent")
                .`in`(KDOC_CONTENT)
                .forType(KDocTokens.LEADING_ASTERISK, KDocTokens.END)
                .set(Indent.getSpaceIndent(KDOC_COMMENT_INDENT)),

        strategy("Block in when entry")
                .`in`(WHEN_ENTRY)
                .notForType(BLOCK, WHEN_CONDITION_EXPRESSION, WHEN_CONDITION_IN_RANGE, WHEN_CONDITION_IS_PATTERN, ELSE_KEYWORD, ARROW)
                .set(Indent.getNormalIndent()))

private fun getPrevWithoutWhitespace(pNode: ASTNode?): ASTNode? {
    var node = pNode
    node = node!!.treePrev
    while (node != null && node.elementType === TokenType.WHITE_SPACE) {
        node = node.treePrev
    }

    return node
}

private fun getPrevWithoutWhitespaceAndComments(pNode: ASTNode?): ASTNode? {
    var node = pNode
    node = node!!.treePrev
    while (node != null && (node.elementType === TokenType.WHITE_SPACE || KtTokens.COMMENTS.contains(node.elementType))) {
        node = node.treePrev
    }

    return node
}

private fun getWrappingStrategyForItemList(wrapType: Int, itemType: IElementType): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, false)
    return object : WrappingStrategy {
        override fun getWrap(childElementType: IElementType): Wrap? {
            return if (childElementType === itemType) itemWrap else null
        }
    }
}

private fun getAlignmentForChildInParenthesis(
        shouldAlignChild: Boolean, parameter: IElementType, delimiter: IElementType,
        shouldAlignParenthesis: Boolean, openBracket: IElementType, closeBracket: IElementType): NodeAlignmentStrategy {
    val parameterAlignment = if (shouldAlignChild) Alignment.createAlignment() else null
    val bracketsAlignment = if (shouldAlignParenthesis) Alignment.createAlignment() else null

    return object : NodeAlignmentStrategy() {
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

private fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): NodeAlignmentStrategy {
    return if (shouldAlignInColumns) {
        NodeAlignmentStrategy.fromTypes(
                AlignmentStrategy.createAlignmentPerTypeStrategy(listOf(ARROW as IElementType), WHEN_ENTRY, true))
    }
    else {
        NodeAlignmentStrategy.getNullStrategy()
    }
}

private fun createChildIndent(child: ASTNode): Indent? {
    val childParent = child.treeParent
    val childType = child.elementType

    if (childParent != null && childParent.treeParent != null) {
        if (childParent.elementType === BLOCK && childParent.treeParent.elementType === SCRIPT) {
            return Indent.getNoneIndent()
        }
    }

    // do not indent child after heading comments inside declaration
    if (childParent != null && childParent.psi is KtDeclaration) {
        val prev = getPrevWithoutWhitespace(child)
        if (prev != null && prev.elementType in KtTokens.COMMENTS && getPrevWithoutWhitespaceAndComments(prev) == null) {
            return Indent.getNoneIndent()
        }
    }

    for (strategy in INDENT_RULES) {
        val indent = strategy.getIndent(child)
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

            return Indent.getContinuationWithoutFirstIndent()
        }

        if (parentType === TYPE_PARAMETER_LIST || parentType === TYPE_ARGUMENT_LIST) {
            return Indent.getContinuationWithoutFirstIndent()
        }
    }

    return Indent.getNoneIndent()
}

private fun createAlignment(alignOption: Boolean, defaultAlignment: Alignment?): Alignment? {
    return if (alignOption) createAlignmentOrDefault(null, defaultAlignment) else defaultAlignment
}

private fun createAlignmentOrDefault(base: Alignment?, defaultAlignment: Alignment?): Alignment? {
    return defaultAlignment ?: if (base == null) Alignment.createAlignment() else Alignment.createChildAlignment(base)
}

private fun findNodeBlockIndex(blocks: List<Block>, tokenSet: TokenSet): Int {
    return blocks.indexOfFirst { block ->
        if (block !is ASTBlock) return@indexOfFirst false

        val node = block.node
        node != null && node.elementType in tokenSet
    }
}

private fun getOperationType(node: ASTNode): IElementType? {
    val operationNode = node.findChildByType(OPERATION_REFERENCE)
    return operationNode?.firstChildNode?.elementType;
}