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
import com.intellij.formatting.DependentSpacingRule.Anchor
import com.intellij.formatting.DependentSpacingRule.Trigger
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.JetNodeTypes
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.lexer.JetTokens
import java.util.ArrayList

class KotlinSpacingBuilder(val codeStyleSettings: CodeStyleSettings) {
    class SpacingNodeBlock(node: ASTNode): AbstractBlock(node, null, null) {
        override fun buildChildren(): MutableList<Block>? = ArrayList()
        override fun getSpacing(child1: Block?, child2: Block): Spacing? = null
        override fun isLeaf(): Boolean = false
    }

    private val builders = ArrayList<Builder>()

    private interface Builder {
        fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing?
    }

    inner class BasicSpacingBuilder() : SpacingBuilder(codeStyleSettings, JetLanguage.INSTANCE), Builder {
        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            return super<SpacingBuilder>.getSpacing(parent, left, right)
        }
    }

    inner class CustomSpacingBuilder() : Builder {
        private val rules = ArrayList<(ASTBlock, ASTBlock, ASTBlock) -> Spacing?>()
        private var conditions = ArrayList<(ASTBlock, ASTBlock, ASTBlock) -> Boolean>()

        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            for (rule in rules) {
                val spacing = rule(parent, left, right)
                if (spacing != null) {
                    return spacing
                }
            }
            return null
        }

        fun inPosition(parent: IElementType? = null, left: IElementType? = null, right: IElementType? = null): CustomSpacingBuilder {
            conditions.add {
                p, l, r ->
                (parent == null || p.getNode()!!.getElementType() == parent) &&
                (left == null || l.getNode()!!.getElementType() == left) &&
                (right == null || r.getNode()!!.getElementType() == right)
            }
            return this
        }

        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int, allowBlankLines: Boolean = true) {
            newRule {
                p, l, r ->
                Spacing.createDependentLFSpacing(numSpacesOtherwise, numSpacesOtherwise, p.getTextRange(),
                                                 codeStyleSettings.KEEP_LINE_BREAKS,
                                                 if (allowBlankLines) codeStyleSettings.KEEP_BLANK_LINES_IN_CODE else 0)
            }
        }

        fun emptyLinesIfLineBreakInLeft(emptyLines: Int, numberOfLineFeedsOtherwise: Int = 1, numSpacesOtherwise: Int = 0) {
            newRule { parent: ASTBlock, left: ASTBlock, right: ASTBlock ->
                val dependentSpacingRule = DependentSpacingRule(Trigger.HAS_LINE_FEEDS).registerData(Anchor.MIN_LINE_FEEDS, emptyLines + 1)
                LineFeedDependantSpacing(
                        numSpacesOtherwise, numSpacesOtherwise,
                        minimumLineFeeds = numberOfLineFeedsOtherwise,
                        keepLineBreaks = codeStyleSettings.KEEP_LINE_BREAKS,
                        keepBlankLines = codeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                        dependency = left.getTextRange(), rule = dependentSpacingRule)
            }
        }

        fun spacing(spacing: Spacing) {
            newRule { parent, left, right -> spacing }
        }

        fun customRule(block: (parent: ASTBlock, left: ASTBlock, right: ASTBlock) -> Spacing?) {
            newRule(block)
        }

        private fun newRule(rule: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?) {
            val savedConditions = ArrayList(conditions)
            rules.add { p, l, r -> if (savedConditions.all { it(p, l, r) }) rule(p, l, r) else null }
            conditions.clear()
        }
    }

    fun getSpacing(parent: Block, child1: Block?, child2: Block): Spacing? {
        if (parent !is ASTBlock || child1 !is ASTBlock || child2 !is ASTBlock) {
            return null
        }

        for (builder in builders) {
            val spacing = builder.getSpacing(parent, child1, child2)

            if (spacing != null) {
                // TODO: it's a severe hack but I don't know how to implement it in other way
                if (child1.getNode().getElementType() == JetTokens.EOL_COMMENT && spacing.toString().contains("minLineFeeds=0")) {
                    val isBeforeBlock = child2.getNode().getElementType() == JetNodeTypes.BLOCK || child2.getNode().getFirstChildNode()?.getElementType() == JetNodeTypes.BLOCK
                    val keepBlankLines = if (isBeforeBlock) 0 else codeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    return Spacing.createSpacing(0, 0, 1, true, keepBlankLines)
                }
                return spacing
            }
        }
        return null
    }

    fun simple(init: BasicSpacingBuilder.() -> Unit) {
        val builder = BasicSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    fun custom(init: CustomSpacingBuilder.() -> Unit) {
        val builder = CustomSpacingBuilder()
        builder.init()
        builders.add(builder)
    }

    private class LineFeedDependantSpacing(
            minSpaces: Int,
            maxSpaces: Int,
            val minimumLineFeeds: Int,
            keepLineBreaks: Boolean,
            keepBlankLines: Int,
            dependency: TextRange,
            rule: DependentSpacingRule) : DependantSpacingImpl(minSpaces, maxSpaces, dependency, keepLineBreaks, keepBlankLines, rule) {
        override fun getMinLineFeeds(): Int {
            val superMin = super.getMinLineFeeds()
            return if (superMin == 0) minimumLineFeeds else superMin
        }
    }
}

fun rules(codeStyleSettings: CodeStyleSettings, init: KotlinSpacingBuilder.() -> Unit): KotlinSpacingBuilder {
    val builder = KotlinSpacingBuilder(codeStyleSettings)
    builder.init()
    return builder
}
