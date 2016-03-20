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
import com.intellij.formatting.alignment.AlignmentStrategy
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.lexer.KtTokens.*

/**
 * @see Block for good JavaDoc documentation
 */
class KotlinBlock(
        node: ASTNode,
        private val myAlignmentStrategy: CommonAlignmentStrategy,
        private val myIndent: Indent?,
        wrap: Wrap?,
        mySettings: CodeStyleSettings,
        private val mySpacingBuilder: KotlinSpacingBuilder) : AbstractBlock(node, wrap, myAlignmentStrategy.getAlignment(node)) {

    private val kotlinDelegationBlock = object : KotlinCommonBlock(node, mySettings, mySpacingBuilder, myAlignmentStrategy) {
        override fun getNullAlignmentStrategy(): CommonAlignmentStrategy = NodeAlignmentStrategy.getNullStrategy()

        override fun createAlignmentStrategy(alignOption: Boolean, defaultAlignment: Alignment?): CommonAlignmentStrategy {
            return NodeAlignmentStrategy.fromTypes(AlignmentStrategy.wrap(createAlignment(alignOption, defaultAlignment)))
        }

        override fun getAlignmentForCaseBranch(shouldAlignInColumns: Boolean): CommonAlignmentStrategy {
            return if (shouldAlignInColumns) {
                NodeAlignmentStrategy.fromTypes(
                        AlignmentStrategy.createAlignmentPerTypeStrategy(listOf(ARROW as IElementType), WHEN_ENTRY, true))
            }
            else {
                NodeAlignmentStrategy.getNullStrategy()
            }
        }

        override fun getAlignment(): Alignment? = alignment

        override fun isIncompleteInSuper(): Boolean = this@KotlinBlock.isIncomplete

        override fun getSuperChildAttributes(newChildIndex: Int): ChildAttributes = super@KotlinBlock.getChildAttributes(newChildIndex)

        override fun getSubBlocks(): List<Block> = subBlocks

        override fun createBlock(node: ASTNode, alignmentStrategy: CommonAlignmentStrategy, indent: Indent?, wrap: Wrap?, settings: CodeStyleSettings, spacingBuilder: KotlinSpacingBuilder): Block {
            return KotlinBlock(
                    node,
                    alignmentStrategy,
                    indent,
                    wrap,
                    mySettings,
                    mySpacingBuilder)
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
            rule: DependentSpacingRule): Spacing {
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