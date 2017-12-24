/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.lang.ASTNode
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
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.jetbrains.kotlin.psi.psiUtil.siblings

private val QUALIFIED_OPERATION = TokenSet.create(DOT, SAFE_ACCESS)
private val QUALIFIED_EXPRESSIONS = TokenSet.create(KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION)
private val KDOC_COMMENT_INDENT = 1

private val BINARY_EXPRESSIONS = TokenSet.create(KtNodeTypes.BINARY_EXPRESSION, KtNodeTypes.BINARY_WITH_TYPE, KtNodeTypes.IS_EXPRESSION)
private val KDOC_CONTENT = TokenSet.create(KDocTokens.KDOC, KDocElementTypes.KDOC_SECTION, KDocElementTypes.KDOC_TAG)

private val CODE_BLOCKS = TokenSet.create(KtNodeTypes.BLOCK, KtNodeTypes.CLASS_BODY, KtNodeTypes.FUNCTION_LITERAL)

private val ALIGN_FOR_BINARY_OPERATIONS = TokenSet.create(MUL, DIV, PERC, PLUS, MINUS, ELVIS, LT, GT, LTEQ, GTEQ, ANDAND, OROR)
private val ANNOTATIONS = TokenSet.create(KtNodeTypes.ANNOTATION_ENTRY, KtNodeTypes.ANNOTATION)

val CodeStyleSettings.kotlinCommonSettings: CommonCodeStyleSettings
    get() = getCommonSettings(KotlinLanguage.INSTANCE)

val CodeStyleSettings.kotlinCustomSettings: KotlinCodeStyleSettings
    get() = getCustomSettings(KotlinCodeStyleSettings::class.java)!!

abstract class KotlinCommonBlock(
        private val node: ASTNode,
        private val settings: CodeStyleSettings,
        private val spacingBuilder: KotlinSpacingBuilder,
        private val alignmentStrategy: CommonAlignmentStrategy) {
    private @Volatile var mySubBlocks: List<ASTBlock>? = null

    abstract protected fun createBlock(node: ASTNode,
                                       alignmentStrategy: CommonAlignmentStrategy,
                                       indent: Indent?,
                                       wrap: Wrap?,
                                       settings: CodeStyleSettings,
                                       spacingBuilder: KotlinSpacingBuilder): ASTBlock

    abstract protected fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock

    abstract protected fun getSubBlocks(): List<Block>

    abstract protected fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes

    abstract protected fun isIncompleteInSuper(): Boolean

    abstract protected fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy

    abstract protected fun getAlignment(): Alignment?

    abstract protected fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy

    abstract protected fun getNullAlignmentStrategy(): CommonAlignmentStrategy

    fun isLeaf(): Boolean = node.firstChildNode == null

    fun buildChildren(): List<Block> {
        if (mySubBlocks != null) {
            return mySubBlocks!!
        }

        var nodeSubBlocks = buildSubBlocks()

        if (node.elementType in QUALIFIED_EXPRESSIONS) {
            val operationBlockIndex = findNodeBlockIndex(nodeSubBlocks, QUALIFIED_OPERATION)
            if (operationBlockIndex != -1) {
                // Create fake ".something" or "?.something" block here, so child indentation will be
                // relative to it when it starts from new line (see Indent javadoc).

                val operationBlock = nodeSubBlocks[operationBlockIndex]
                val indent = if (settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS)
                    Indent.getContinuationWithoutFirstIndent()
                else
                    Indent.getNormalIndent()
                val isNonFirstChainedCall = operationBlockIndex > 0 && isCallBlock(nodeSubBlocks[operationBlockIndex - 1])
                val wrap = if ((settings.kotlinCommonSettings.WRAP_FIRST_METHOD_IN_CALL_CHAIN || isNonFirstChainedCall) &&
                               canWrapCallChain(node))
                    Wrap.createWrap(settings.kotlinCommonSettings.METHOD_CALL_CHAIN_WRAP, true)
                else
                    null

                val operationSyntheticBlock = SyntheticKotlinBlock(
                        operationBlock.node,
                        nodeSubBlocks.subList(operationBlockIndex, nodeSubBlocks.size),
                        null, indent, wrap, spacingBuilder) { createSyntheticSpacingNodeBlock(it) }

                nodeSubBlocks = nodeSubBlocks.subList(0, operationBlockIndex) + operationSyntheticBlock
            }
        }

        mySubBlocks = nodeSubBlocks

        return nodeSubBlocks
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

    fun createChildIndent(child: ASTNode): Indent? {
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

                return Indent.getContinuationWithoutFirstIndent()
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

        return when (type) {
            in CODE_BLOCKS, KtNodeTypes.WHEN, KtNodeTypes.IF, KtNodeTypes.FOR, KtNodeTypes.WHILE, KtNodeTypes.DO_WHILE -> ChildAttributes(Indent.getNormalIndent(), null)

            KtNodeTypes.TRY -> ChildAttributes(Indent.getNoneIndent(), null)

            in QUALIFIED_EXPRESSIONS -> ChildAttributes(Indent.getContinuationWithoutFirstIndent(), null)

            KtNodeTypes.VALUE_PARAMETER_LIST, KtNodeTypes.VALUE_ARGUMENT_LIST -> {
                val subBlocks = getSubBlocks()
                if (newChildIndex != 1 && newChildIndex != 0 && newChildIndex < subBlocks.size) {
                    val block = subBlocks[newChildIndex]
                    ChildAttributes(block.indent, block.alignment)
                }
                else {
                    val indent = if (type == KtNodeTypes.VALUE_PARAMETER_LIST && !settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS)
                        Indent.getNormalIndent()
                    else
                        Indent.getContinuationIndent()
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
                        kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR)

            parentType === KtNodeTypes.VALUE_ARGUMENT_LIST ->
                getAlignmentForChildInParenthesis(
                        kotlinCommonSettings.ALIGN_MULTILINE_PARAMETERS_IN_CALLS, KtNodeTypes.VALUE_ARGUMENT, COMMA,
                        kotlinCommonSettings.ALIGN_MULTILINE_METHOD_BRACKETS, LPAR, RPAR)

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
                    private var bracketsAlignment: Alignment? = if (kotlinCommonSettings.ALIGN_MULTILINE_BINARY_OPERATION) Alignment.createAlignment() else null

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


    private fun buildSubBlock(child: ASTNode, alignmentStrategy: CommonAlignmentStrategy, wrappingStrategy: WrappingStrategy): ASTBlock {
        val childWrap = wrappingStrategy.getWrap(child)

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
                        spacingBuilder)
            }
        }

        return createBlock(child, alignmentStrategy, createChildIndent(child), childWrap, settings, spacingBuilder)
    }

    private fun buildSubBlocks(): List<ASTBlock> {
        val childrenAlignmentStrategy = getChildrenAlignmentStrategy()
        val wrappingStrategy = getWrappingStrategy()

        return node.children()
                .filter { it.textRange.length > 0 && it.elementType != TokenType.WHITE_SPACE }
                .map { buildSubBlock(it, childrenAlignmentStrategy, wrappingStrategy ) }
                .toList()
    }

    private fun getWrappingStrategy(): WrappingStrategy {
        val commonSettings = settings.kotlinCommonSettings
        val elementType = node.elementType
        val nodePsi = node.psi

        when {
            elementType === KtNodeTypes.VALUE_ARGUMENT_LIST ->
                return getWrappingStrategyForItemList(commonSettings.CALL_PARAMETERS_WRAP, KtNodeTypes.VALUE_ARGUMENT)

            elementType === KtNodeTypes.VALUE_PARAMETER_LIST -> {
                val parentElementType = node.treeParent.elementType
                if (parentElementType === KtNodeTypes.FUN ||
                    parentElementType === KtNodeTypes.PRIMARY_CONSTRUCTOR ||
                    parentElementType === KtNodeTypes.SECONDARY_CONSTRUCTOR) {
                    val wrap = Wrap.createWrap(commonSettings.METHOD_PARAMETERS_WRAP, false)
                    return object : WrappingStrategy {
                        override fun getWrap(childElement: ASTNode): Wrap? {
                            return if (childElement.elementType === KtNodeTypes.VALUE_PARAMETER && !childElement.startsWithAnnotation())
                                wrap
                            else
                                null
                        }
                    }
                }
            }

            elementType === KtNodeTypes.SUPER_TYPE_LIST -> {
                val wrap = Wrap.createWrap(commonSettings.EXTENDS_LIST_WRAP, false)
                return object : WrappingStrategy {
                    override fun getWrap(childElement: ASTNode): Wrap? =
                        if (childElement.psi is KtSuperTypeListEntry) wrap else null
                }
            }

            elementType === KtNodeTypes.CLASS_BODY ->
                return getWrappingStrategyForItemList(commonSettings.ENUM_CONSTANTS_WRAP, KtNodeTypes.ENUM_ENTRY)

            elementType === KtNodeTypes.MODIFIER_LIST -> {
                val parent = node.treeParent.psi
                when (parent) {
                    is KtParameter ->
                        return getWrappingStrategyForItemList(commonSettings.PARAMETER_ANNOTATION_WRAP,
                                                              ANNOTATIONS,
                                                              !node.treeParent.isFirstParameter())
                    is KtClassOrObject ->
                        return getWrappingStrategyForItemList(commonSettings.CLASS_ANNOTATION_WRAP,
                                                              ANNOTATIONS)

                    is KtNamedFunction ->
                        return getWrappingStrategyForItemList(commonSettings.METHOD_ANNOTATION_WRAP,
                                                              ANNOTATIONS)

                    is KtProperty ->
                        return getWrappingStrategyForItemList(if (parent.isLocal)
                                                                  commonSettings.VARIABLE_ANNOTATION_WRAP
                                                              else
                                                                  commonSettings.FIELD_ANNOTATION_WRAP,
                                                              ANNOTATIONS)
                }
            }

            elementType === KtNodeTypes.VALUE_PARAMETER ->
                return wrapAfterAnnotation(commonSettings.PARAMETER_ANNOTATION_WRAP)

            nodePsi is KtClassOrObject ->
                return wrapAfterAnnotation(commonSettings.CLASS_ANNOTATION_WRAP)

            nodePsi is KtNamedFunction ->
                return wrapAfterAnnotation(commonSettings.METHOD_ANNOTATION_WRAP)

            nodePsi is KtProperty ->
                return wrapAfterAnnotation(if (nodePsi.isLocal)
                                               commonSettings.VARIABLE_ANNOTATION_WRAP
                                           else
                                               commonSettings.FIELD_ANNOTATION_WRAP)
        }

        return WrappingStrategy.NoWrapping
    }
}

private fun ASTNode.startsWithAnnotation() = firstChildNode?.firstChildNode?.elementType == KtNodeTypes.ANNOTATION_ENTRY

private fun ASTNode.isFirstParameter(): Boolean = treePrev?.elementType == KtTokens.LPAR

private fun wrapAfterAnnotation(wrapType: Int): WrappingStrategy {
    return object : WrappingStrategy {
        override fun getWrap(childElement: ASTNode): Wrap? {
            if (childElement.elementType in KtTokens.COMMENTS) return null
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
    }
}

private val INDENT_RULES = arrayOf<NodeIndentStrategy>(
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
                .set { settings ->
                    if (settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_EXPRESSION_BODIES)
                        Indent.getContinuationIndent()
                    else
                        Indent.getNormalIndent()
                },

        strategy("Property accessor expression body")
                .within(KtNodeTypes.PROPERTY_ACCESSOR)
                .forElement {
                    it.psi is KtExpression && it.psi !is KtBlockExpression
                }
                .set(Indent.getNormalIndent()),

        strategy("Indent for parts")
                .within(KtNodeTypes.PROPERTY, KtNodeTypes.FUN, KtNodeTypes.DESTRUCTURING_DECLARATION, KtNodeTypes.SECONDARY_CONSTRUCTOR)
                .notForType(KtNodeTypes.BLOCK, FUN_KEYWORD, VAL_KEYWORD, VAR_KEYWORD, CONSTRUCTOR_KEYWORD, KtTokens.RPAR)
                .set(Indent.getContinuationWithoutFirstIndent()),

        strategy("Chained calls")
                .within(QUALIFIED_EXPRESSIONS)
                .notForType(KtTokens.DOT, KtTokens.SAFE_ACCESS)
                .forElement { it.treeParent.firstChildNode != it }
                .set { settings ->
                    if (settings.kotlinCustomSettings.CONTINUATION_INDENT_FOR_CHAINED_CALLS)
                        Indent.getContinuationWithoutFirstIndent()
                    else
                        Indent.getNormalIndent()
                },

        strategy("Colon of delegation list")
                .within(KtNodeTypes.CLASS, KtNodeTypes.OBJECT_DECLARATION)
                .forType(KtTokens.COLON)
                .set(Indent.getNormalIndent(false)),

        strategy("Delegation list")
                .within(KtNodeTypes.SUPER_TYPE_LIST, KtNodeTypes.INITIALIZER_LIST)
                .set { settings ->
                    if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_SUPERTYPE_LISTS)
                        Indent.getContinuationIndent(false)
                    else
                        Indent.getNormalIndent()
                },

        strategy("Indices")
                .within(KtNodeTypes.INDICES)
                .set(Indent.getContinuationIndent(false)),

        strategy("Binary expressions")
                .within(BINARY_EXPRESSIONS)
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
                .notForType(KtNodeTypes.BLOCK, KtNodeTypes.WHEN_CONDITION_EXPRESSION, KtNodeTypes.WHEN_CONDITION_IN_RANGE, KtNodeTypes.WHEN_CONDITION_IS_PATTERN, ELSE_KEYWORD, ARROW)
                .set(Indent.getNormalIndent()),

        strategy("Parameter list")
                .within(KtNodeTypes.VALUE_PARAMETER_LIST)
                .forElement { it.elementType == KtNodeTypes.VALUE_PARAMETER && it.psi.prevSibling != null }
                .set { settings ->
                    if (settings.kotlinCustomSettings.CONTINUATION_INDENT_IN_PARAMETER_LISTS)
                        Indent.getContinuationIndent()
                    else
                        Indent.getNormalIndent()
                },

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
                .notForType(KtTokens.TYPE_ALIAS_KEYWORD, KtTokens.EOL_COMMENT, KtNodeTypes.MODIFIER_LIST)
                .set(Indent.getContinuationIndent())
)


private fun getOperationType(node: ASTNode): IElementType? = node.findChildByType(KtNodeTypes.OPERATION_REFERENCE)?.firstChildNode?.elementType

private fun hasErrorElementBefore(node: ASTNode): Boolean {
    val prevSibling = getPrevWithoutWhitespace(node) ?: return false
    if (prevSibling.elementType == TokenType.ERROR_ELEMENT) return true
    val lastChild = TreeUtil.getLastChild(prevSibling)
    return lastChild?.elementType == TokenType.ERROR_ELEMENT
}

private fun getAlignmentForChildInParenthesis(
        shouldAlignChild: Boolean, parameter: IElementType, delimiter: IElementType,
        shouldAlignParenthesis: Boolean, openBracket: IElementType, closeBracket: IElementType): CommonAlignmentStrategy {
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
    return object : WrappingStrategy {
        override fun getWrap(childElement: ASTNode): Wrap? {
            return if (childElement.elementType === itemType) itemWrap else null
        }
    }
}

private fun getWrappingStrategyForItemList(wrapType: Int, itemTypes: TokenSet, wrapFirstElement: Boolean = false): WrappingStrategy {
    val itemWrap = Wrap.createWrap(wrapType, wrapFirstElement)
    return object : WrappingStrategy {
        override fun getWrap(childElement: ASTNode): Wrap? {
            return if (childElement.elementType in itemTypes) itemWrap else null
        }
    }
}

private fun findNodeBlockIndex(blocks: List<ASTBlock>, tokenSet: TokenSet): Int {
    return blocks.indexOfFirst { block -> block.node?.elementType in tokenSet }
}
