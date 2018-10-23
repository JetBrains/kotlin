/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.*
import com.intellij.formatting.alignment.AlignmentStrategy
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes.WHEN_ENTRY
import org.jetbrains.kotlin.lexer.KtTokens.ARROW

/**
 * @see Block for good JavaDoc documentation
 */
class KotlinBlock(
    node: ASTNode,
    private val myAlignmentStrategy: CommonAlignmentStrategy,
    private val myIndent: Indent?,
    wrap: Wrap?,
    mySettings: CodeStyleSettings,
    private val mySpacingBuilder: KotlinSpacingBuilder,
    overrideChildren: Sequence<ASTNode>? = null
) : AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)) {

    private val kotlinDelegationBlock = object : KotlinCommonBlock(
        node, mySettings, mySpacingBuilder, myAlignmentStrategy, overrideChildren
    ) {
        override fun getNullAlignmentStrategy(): CommonAlignmentStrategy = NodeAlignmentStrategy.getNullStrategy()

        override fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(createAlignment(alignOption, defaultAlignment)))
        }

        override fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy {
            return if (shouldAlignInColumns) {
                NodeAlignmentStrategy.fromTypes(
                    AlignmentStrategy.createAlignmentPerTypeStrategy(listOf(ARROW as IElementType), WHEN_ENTRY, true)
                )
            } else {
                NodeAlignmentStrategy.getNullStrategy()
            }
        }

        override fun getAlignment(): Alignment? = alignment

        override fun isIncompleteInSuper(): Boolean = super@KotlinBlock.isIncomplete()

        override fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes = super@KotlinBlock.getChildAttributes(newChildIndex)

        override fun getSubBlocks(): List<Block> = subBlocks

        override fun createBlock(
            node: ASTNode,
            alignmentStrategy: CommonAlignmentStrategy,
            indent: Indent?,
            wrap: Wrap?,
            settings: CodeStyleSettings,
            spacingBuilder: KotlinSpacingBuilder,
            overrideChildren: Sequence<ASTNode>?
        ): ASTBlock {
            return KotlinBlock(
                node,
                alignmentStrategy,
                indent,
                wrap,
                mySettings,
                mySpacingBuilder,
                overrideChildren
            )
        }

        override fun createSyntheticSpacingNodeBlock(node: ASTNode): ASTBlock {
            return object : AbstractBlock(node, null, null) {
                override fun isLeaf(): Boolean = false
                override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
                override fun buildChildren(): List<Block> = emptyList()
            }
        }
    }

    override fun getIndent(): Indent? = myIndent

    override fun buildChildren(): List<Block> = kotlinDelegationBlock.buildChildren()

    override fun getSpacing(child1: Block?, child2: Block): Spacing? = mySpacingBuilder.getSpacing(this, child1, child2)

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes = kotlinDelegationBlock.getChildAttributes(newChildIndex)

    override fun isLeaf(): Boolean = kotlinDelegationBlock.isLeaf()

    override fun getTextRange() = kotlinDelegationBlock.getTextRange()

    override fun isIncomplete(): Boolean = kotlinDelegationBlock.isIncomplete()
}

object KotlinSpacingBuilderUtilImpl : KotlinSpacingBuilderUtil {
    override fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode? {
        return FormatterUtil.getPreviousNonWhitespaceLeaf(node)
    }

    override fun isWhitespaceOrEmpty(node: ASTNode?): Boolean {
        return FormatterUtil.isWhitespaceOrEmpty(node)
    }

    override fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing {
        return object : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
            override fun getMinLineFeeds(): Int {
                val superMin = super.getMinLineFeeds()
                return if (superMin == 0) minimumLineFeeds else superMin
            }
        }
    }
}

private fun createAlignment(alignOption: Boolean, defaultAlignment: Alignment?): Alignment? {
    return if (alignOption) createAlignmentOrDefault(null, defaultAlignment) else defaultAlignment
}

private fun createAlignmentOrDefault(base: Alignment?, defaultAlignment: Alignment?): Alignment? {
    return defaultAlignment ?: if (base == null) Alignment.createAlignment() else Alignment.createChildAlignment(base)
}