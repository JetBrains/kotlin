/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.formatter

import com.intellij.formatting.*

import org.jetbrains.jet.plugin.JetLanguage
import com.intellij.psi.codeStyle.CodeStyleSettings
import java.util.ArrayList
import org.jetbrains.jet.JetNodeTypes.*
import org.jetbrains.jet.lexer.JetTokens.*
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.tree.IElementType
import com.intellij.psi.formatter.FormatterUtil
import com.intellij.lang.ASTNode

class KotlinSpacingBuilder(val codeStyleSettings: CodeStyleSettings) {

    private val builders = ArrayList<Builder>()

    private trait Builder {
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

        fun lineBreakIfLineBreakInParent(numSpacesOtherwise: Int) {
            newRule {
                p, l, r ->
                Spacing.createDependentLFSpacing(numSpacesOtherwise, numSpacesOtherwise, p.getTextRange(),
                                                 codeStyleSettings.KEEP_LINE_BREAKS, codeStyleSettings.KEEP_BLANK_LINES_IN_CODE)
            }
        }

        fun customRule(block: (ASTBlock, ASTBlock, ASTBlock) -> Spacing?) {
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
}

fun rules(codeStyleSettings: CodeStyleSettings, init: KotlinSpacingBuilder.() -> Unit): KotlinSpacingBuilder {
    val builder = KotlinSpacingBuilder(codeStyleSettings)
    builder.init()
    return builder
}

fun createSpacingBuilder(settings: CodeStyleSettings): KotlinSpacingBuilder {
    val jetSettings = settings.getCustomSettings(javaClass<JetCodeStyleSettings>())!!
    val jetCommonSettings = settings.getCommonSettings(JetLanguage.INSTANCE)!!
    return rules(settings) {
        simple {
            // ============ Line breaks ==============
            after(PACKAGE_DIRECTIVE).blankLines(1)
            between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).lineBreakInCode()
            after(IMPORT_LIST).blankLines(1)

            before(DOC_COMMENT).lineBreakInCode()
            before(FUN).lineBreakInCode()
            before(PROPERTY).lineBreakInCode()
            between(FUN, FUN).blankLines(1)
            between(FUN, PROPERTY).blankLines(1)

            // =============== Spacing ================
            before(COMMA).spaceIf(jetCommonSettings.SPACE_BEFORE_COMMA)
            after(COMMA).spaceIf(jetCommonSettings.SPACE_AFTER_COMMA)

            around(TokenSet.create(EQ, MULTEQ, DIVEQ, PLUSEQ, MINUSEQ, PERCEQ)).spaceIf(jetCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            around(TokenSet.create(ANDAND, OROR)).spaceIf(jetCommonSettings.SPACE_AROUND_LOGICAL_OPERATORS)
            around(TokenSet.create(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ)).spaceIf(jetCommonSettings.SPACE_AROUND_EQUALITY_OPERATORS)
            aroundInside(TokenSet.create(LT, GT, LTEQ, GTEQ), BINARY_EXPRESSION).spaceIf(jetCommonSettings.SPACE_AROUND_RELATIONAL_OPERATORS)
            aroundInside(TokenSet.create(PLUS, MINUS), BINARY_EXPRESSION).spaceIf(jetCommonSettings.SPACE_AROUND_ADDITIVE_OPERATORS)
            aroundInside(TokenSet.create(MUL, DIV, PERC), BINARY_EXPRESSION).spaceIf(jetCommonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS)
            around(TokenSet.create(PLUSPLUS, MINUSMINUS, EXCLEXCL, MINUS, PLUS, EXCL)).spaceIf(jetCommonSettings.SPACE_AROUND_UNARY_OPERATOR)
            around(RANGE).spaceIf(jetSettings.SPACE_AROUND_RANGE)

            afterInside(LPAR, VALUE_PARAMETER_LIST).spaces(0)
            beforeInside(RPAR, VALUE_PARAMETER_LIST).spaces(0)
            afterInside(LT, TYPE_PARAMETER_LIST).spaces(0)
            beforeInside(GT, TYPE_PARAMETER_LIST).spaces(0)
            afterInside(LPAR, VALUE_ARGUMENT_LIST).spaces(0)
            beforeInside(RPAR, VALUE_ARGUMENT_LIST).spaces(0)
            afterInside(LT, TYPE_ARGUMENT_LIST).spaces(0)
            beforeInside(GT, TYPE_ARGUMENT_LIST).spaces(0)

            betweenInside(FOR_KEYWORD, LPAR, FOR).spacing(1, 1, 0, false, 0)
            betweenInside(IF_KEYWORD, LPAR, IF).spacing(1, 1, 0, false, 0)
            betweenInside(WHILE_KEYWORD, LPAR, WHILE).spacing(1, 1, 0, false, 0)
            betweenInside(WHILE_KEYWORD, LPAR, DO_WHILE).spacing(1, 1, 0, false, 0)

            aroundInside(WHILE_KEYWORD, DO_WHILE).spaces(1)

            // TODO: Ask for better API
            // Type of the declaration colon
            beforeInside(COLON, PROPERTY).spaceIf(jetSettings.SPACE_BEFORE_TYPE_COLON)
            afterInside(COLON, PROPERTY).spaceIf(jetSettings.SPACE_AFTER_TYPE_COLON)
            beforeInside(COLON, FUN).spaceIf(jetSettings.SPACE_BEFORE_TYPE_COLON)
            afterInside(COLON, FUN).spaceIf(jetSettings.SPACE_AFTER_TYPE_COLON)
            beforeInside(COLON, VALUE_PARAMETER).spaceIf(jetSettings.SPACE_BEFORE_TYPE_COLON)
            afterInside(COLON, VALUE_PARAMETER).spaceIf(jetSettings.SPACE_AFTER_TYPE_COLON)

            // Extends or constraint colon
            beforeInside(COLON, TYPE_CONSTRAINT).spaceIf(jetSettings.SPACE_BEFORE_EXTEND_COLON)
            afterInside(COLON, TYPE_CONSTRAINT).spaceIf(jetSettings.SPACE_AFTER_EXTEND_COLON)
            beforeInside(COLON, CLASS).spaceIf(jetSettings.SPACE_BEFORE_EXTEND_COLON)
            afterInside(COLON, CLASS).spaceIf(jetSettings.SPACE_AFTER_EXTEND_COLON)
            beforeInside(COLON, TYPE_PARAMETER).spaceIf(jetSettings.SPACE_BEFORE_EXTEND_COLON)
            afterInside(COLON, TYPE_PARAMETER).spaceIf(jetSettings.SPACE_AFTER_EXTEND_COLON)

            between(VALUE_ARGUMENT_LIST, FUNCTION_LITERAL_EXPRESSION).spaces(1)
            beforeInside(ARROW, FUNCTION_LITERAL).spaceIf(jetSettings.SPACE_BEFORE_LAMBDA_ARROW)

            //when
            aroundInside(ARROW, WHEN_ENTRY).spaceIf(jetSettings.SPACE_AROUND_WHEN_ARROW)
            beforeInside(LBRACE, WHEN).spacing(1, 1, 0, true, 0)          //omit blank lines before '{' in 'when' statement

            aroundInside(ARROW, FUNCTION_TYPE).spaceIf(jetSettings.SPACE_AROUND_FUNCTION_TYPE_ARROW)

            betweenInside(REFERENCE_EXPRESSION, FUNCTION_LITERAL_EXPRESSION, CALL_EXPRESSION).spaces(1)

            beforeInside(ELSE_KEYWORD, IF).spaces(1)
        }
        custom {

            fun spacingForLeftBrace(block: ASTNode?): Spacing? {
                val noBlockSpacing = Spacing.createSpacing(1, 1, 0, settings.KEEP_LINE_BREAKS, settings.KEEP_BLANK_LINES_IN_CODE)
                if (block != null && block.getElementType() == BLOCK) {
                    val leftBrace = block.getFirstChildNode()
                    if (leftBrace != null && leftBrace.getElementType() == LBRACE) {
                        val previousLeaf = FormatterUtil.getPreviousNonWhitespaceLeaf(leftBrace)
                        val isAfterEolComment = previousLeaf != null && (previousLeaf.getElementType() == EOL_COMMENT)
                        val keepLineBreaks = jetSettings.LBRACE_ON_NEXT_LINE || isAfterEolComment
                        val minimumLF = if (jetSettings.LBRACE_ON_NEXT_LINE) 1 else 0
                        return Spacing.createSpacing(1, 1, minimumLF, keepLineBreaks, 0)
                    }
                }
                return noBlockSpacing
            }

            val leftBraceRule = {
                (parent: ASTBlock, left: ASTBlock, right: ASTBlock) ->
                spacingForLeftBrace(right.getNode())
            }

            val leftBraceRuleIfBlockIsWrapped = {
                (parent: ASTBlock, left: ASTBlock, right: ASTBlock) ->
                spacingForLeftBrace(right.getNode()!!.getFirstChildNode())
            }

            inPosition(parent = IF, right = THEN).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = IF, right = ELSE).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = FOR, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = DO_WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = TRY, right = BLOCK).customRule(leftBraceRule)
            inPosition(parent = CATCH, right = BLOCK).customRule(leftBraceRule)
            inPosition(parent = FINALLY, right = BLOCK).customRule(leftBraceRule)

            inPosition(parent = FUN, right = BLOCK).customRule(leftBraceRule)


            val spacesInSimpleFunction = if (jetSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD) 1 else 0
            inPosition(parent = FUNCTION_LITERAL,
                       left = LBRACE,
                       right = BLOCK)
                    .lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(parent = FUNCTION_LITERAL,
                       left = ARROW,
                       right = BLOCK)
                    .lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)

            inPosition(parent = FUNCTION_LITERAL,
                       right = RBRACE)
                    .lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(parent = FUNCTION_LITERAL,
                       left = LBRACE)
                    .customRule {
                parent, left, right ->
                val rightNode = right.getNode()!!
                val rightType = rightNode.getElementType()
                var numSpaces = spacesInSimpleFunction
                if (rightType == VALUE_PARAMETER_LIST) {
                    val firstParamListNode = rightNode.getFirstChildNode()
                    if (firstParamListNode != null && firstParamListNode.getElementType() == LPAR) {
                        // Don't put space for situation {<here>(a: Int) -> a }
                        numSpaces = 0
                    }
                }

                Spacing.createSpacing(numSpaces, numSpaces, 0, settings.KEEP_LINE_BREAKS, settings.KEEP_BLANK_LINES_IN_CODE)
            }
        }

        simple {
            afterInside(LBRACE, BLOCK).lineBreakInCode()
            beforeInside(RBRACE, CLASS_BODY).lineBreakInCode()
            beforeInside(RBRACE, BLOCK).lineBreakInCode()
            between(RPAR, BODY).spaces(1)
        }
    }
}
