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
import com.intellij.formatting.DependentSpacingRule.Anchor
import com.intellij.formatting.DependentSpacingRule.Trigger
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

class KotlinSpacingBuilder(val commonCodeStyleSettings: CommonCodeStyleSettings, val spacingBuilderUtil: KotlinSpacingBuilderUtil) {
    private val builders = ArrayList<Builder>()

    private interface Builder {
        fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing?
    }

    inner class BasicSpacingBuilder : SpacingBuilder(commonCodeStyleSettings), Builder {
        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            return super.getSpacing(parent, left, right)
        }
    }

    inner class CustomSpacingBuilder : Builder {
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

        fun inPosition(parent: IElementType? = null, left: IElementType? = null, right: IElementType? = null,
                       parentSet: TokenSet? = null, leftSet: TokenSet? = null, rightSet: TokenSet? = null): CustomSpacingBuilder {
            conditions.add {
                p, l, r ->
                (parent == null || p.node!!.elementType == parent) &&
                (left == null || l.node!!.elementType == left) &&
                (right == null || r.node!!.elementType == right) &&
                (parentSet == null || parentSet.contains(p.node!!.elementType)) &&
                (leftSet == null || leftSet.contains(l.node!!.elementType)) &&
                (rightSet == null || rightSet.contains(r.node!!.elementType))
            }
            return this
        }

        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int, allowBlankLines: Boolean = true) {
            newRule { p, _, _ ->
            Spacing.createDependentLFSpacing(numSpacesOtherwise, numSpacesOtherwise, p.textRange,
                                                 commonCodeStyleSettings.KEEP_LINE_BREAKS,
                                                 if (allowBlankLines) commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE else 0)
            }
        }

        fun emptyLinesIfLineBreakInLeft(emptyLines: Int, numberOfLineFeedsOtherwise: Int = 1, numSpacesOtherwise: Int = 0) {
            newRule { _: ASTBlock, left: ASTBlock, _: ASTBlock ->
                val lastChild = left.node?.psi?.lastChild
                val leftEndsWithComment = lastChild is PsiComment && lastChild.tokenType == KtTokens.EOL_COMMENT
                val dependentSpacingRule = DependentSpacingRule(Trigger.HAS_LINE_FEEDS).registerData(Anchor.MIN_LINE_FEEDS, emptyLines + 1)
                spacingBuilderUtil.createLineFeedDependentSpacing(numSpacesOtherwise,
                                                                  numSpacesOtherwise,
                                                                  if (leftEndsWithComment) Math.max(1, numberOfLineFeedsOtherwise) else numberOfLineFeedsOtherwise,
                                                                  commonCodeStyleSettings.KEEP_LINE_BREAKS,
                                                                  commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                                                                  left.textRange,
                                                                  dependentSpacingRule)
            }
        }

        fun spacing(spacing: Spacing) {
            newRule { _, _, _ -> spacing }
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
                if (child1.node.elementType == KtTokens.EOL_COMMENT && spacing.toString().contains("minLineFeeds=0")) {
                    val isBeforeBlock = child2.node.elementType == KtNodeTypes.BLOCK || child2.node.firstChildNode?.elementType == KtNodeTypes.BLOCK
                    val keepBlankLines = if (isBeforeBlock) 0 else commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    return createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = keepBlankLines)
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

    fun createSpacing(minSpaces: Int,
                      maxSpaces: Int = minSpaces,
                      minLineFeeds: Int = 0,
                      keepLineBreaks: Boolean = commonCodeStyleSettings.KEEP_LINE_BREAKS,
                      keepBlankLines: Int = commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE): Spacing {
        return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)
    }
}

interface KotlinSpacingBuilderUtil {
    fun createLineFeedDependentSpacing(minSpaces: Int,
                                       maxSpaces: Int,
                                       minimumLineFeeds: Int,
                                       keepLineBreaks: Boolean,
                                       keepBlankLines: Int,
                                       dependency: TextRange,
                                       rule: DependentSpacingRule): Spacing

    fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode?

    fun isWhitespaceOrEmpty(node: ASTNode?): Boolean
}

fun rules(commonCodeStyleSettings: CommonCodeStyleSettings, builderUtil: KotlinSpacingBuilderUtil, init: KotlinSpacingBuilder.() -> Unit): KotlinSpacingBuilder {
    val builder = KotlinSpacingBuilder(commonCodeStyleSettings, builderUtil)
    builder.init()
    return builder
}
