/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.util.requireNode
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*
import kotlin.math.max

fun CommonCodeStyleSettings.createSpaceBeforeRBrace(numSpacesOtherwise: Int, textRange: TextRange): Spacing? {
    return Spacing.createDependentLFSpacing(
        numSpacesOtherwise, numSpacesOtherwise, textRange,
        KEEP_LINE_BREAKS,
        KEEP_BLANK_LINES_BEFORE_RBRACE
    )
}

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

    private data class Condition(
        val parent: IElementType? = null,
        val left: IElementType? = null,
        val right: IElementType? = null,
        val parentSet: TokenSet? = null,
        val leftSet: TokenSet? = null,
        val rightSet: TokenSet? = null
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Boolean {
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Boolean =
            (parent == null || p.requireNode().elementType == parent) &&
                    (left == null || l.requireNode().elementType == left) &&
                    (right == null || r.requireNode().elementType == right) &&
                    (parentSet == null || parentSet.contains(p.requireNode().elementType)) &&
                    (leftSet == null || leftSet.contains(l.requireNode().elementType)) &&
                    (rightSet == null || rightSet.contains(r.requireNode().elementType))
    }

    private data class Rule(
        val conditions: List<Condition>,
        val action: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?
    ) : (ASTBlock, ASTBlock, ASTBlock) -> Spacing? {
        override fun invoke(p: ASTBlock, l: ASTBlock, r: ASTBlock): Spacing? =
            if (conditions.all { it(p, l, r) }) action(p, l, r) else null
    }

    inner class CustomSpacingBuilder : Builder {
        private val rules = ArrayList<Rule>()
        private var conditions = ArrayList<Condition>()

        override fun getSpacing(parent: ASTBlock, left: ASTBlock, right: ASTBlock): Spacing? {
            for (rule in rules) {
                val spacing = rule(parent, left, right)
                if (spacing != null) {
                    return spacing
                }
            }
            return null
        }

        fun inPosition(
            parent: IElementType? = null, left: IElementType? = null, right: IElementType? = null,
            parentSet: TokenSet? = null, leftSet: TokenSet? = null, rightSet: TokenSet? = null
        ): CustomSpacingBuilder {
            conditions.add(Condition(parent, left, right, parentSet, leftSet, rightSet))
            return this
        }

        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int, allowBlankLines: Boolean = true) {
            newRule { p, _, _ ->
                Spacing.createDependentLFSpacing(
                    numSpacesOtherwise, numSpacesOtherwise, p.textRange,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    if (allowBlankLines) commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE else 0
                )
            }
        }

        fun emptyLinesIfLineBreakInLeft(emptyLines: Int, numberOfLineFeedsOtherwise: Int = 1, numSpacesOtherwise: Int = 0) {
            newRule { _: ASTBlock, left: ASTBlock, _: ASTBlock ->
                val lastChild = left.node?.psi?.lastChild
                val leftEndsWithComment = lastChild is PsiComment && lastChild.tokenType == KtTokens.EOL_COMMENT
                val dependentSpacingRule = DependentSpacingRule(Trigger.HAS_LINE_FEEDS).registerData(Anchor.MIN_LINE_FEEDS, emptyLines + 1)
                spacingBuilderUtil.createLineFeedDependentSpacing(
                    numSpacesOtherwise,
                    numSpacesOtherwise,
                    if (leftEndsWithComment) max(1, numberOfLineFeedsOtherwise) else numberOfLineFeedsOtherwise,
                    commonCodeStyleSettings.KEEP_LINE_BREAKS,
                    commonCodeStyleSettings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                    left.textRange,
                    dependentSpacingRule
                )
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
            rules.add(Rule(savedConditions, rule))
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
                if (child1.requireNode().elementType == KtTokens.EOL_COMMENT && spacing.toString().contains("minLineFeeds=0")) {
                    val isBeforeBlock =
                        child2.requireNode().elementType == KtNodeTypes.BLOCK || child2.requireNode().firstChildNode?.elementType == KtNodeTypes.BLOCK
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

    fun createSpacing(
        minSpaces: Int,
        maxSpaces: Int = minSpaces,
        minLineFeeds: Int = 0,
        keepLineBreaks: Boolean = commonCodeStyleSettings.KEEP_LINE_BREAKS,
        keepBlankLines: Int = commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
    ): Spacing {
        return Spacing.createSpacing(minSpaces, maxSpaces, minLineFeeds, keepLineBreaks, keepBlankLines)
    }
}

interface KotlinSpacingBuilderUtil {
    fun createLineFeedDependentSpacing(
        minSpaces: Int,
        maxSpaces: Int,
        minimumLineFeeds: Int,
        keepLineBreaks: Boolean,
        keepBlankLines: Int,
        dependency: TextRange,
        rule: DependentSpacingRule
    ): Spacing

    fun getPreviousNonWhitespaceLeaf(node: ASTNode?): ASTNode?

    fun isWhitespaceOrEmpty(node: ASTNode?): Boolean
}

fun rules(
    commonCodeStyleSettings: CommonCodeStyleSettings,
    builderUtil: KotlinSpacingBuilderUtil,
    init: KotlinSpacingBuilder.() -> Unit
): KotlinSpacingBuilder {
    val builder = KotlinSpacingBuilder(commonCodeStyleSettings, builderUtil)
    builder.init()
    return builder
}
