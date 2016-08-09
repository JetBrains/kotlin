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

import com.intellij.formatting.ASTBlock
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.SpacingBuilder.RuleBuilder
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.core.formatter.KotlinCodeStyleSettings
import org.jetbrains.kotlin.idea.formatter.KotlinSpacingBuilder.CustomSpacingBuilder
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.KtClass

val MODIFIERS_LIST_ENTRIES = TokenSet.orSet(TokenSet.create(ANNOTATION_ENTRY, ANNOTATION), MODIFIER_KEYWORDS)

fun SpacingBuilder.beforeInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> beforeInside(element, inType).spacingFun() }
}

fun SpacingBuilder.afterInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> afterInside(element, inType).spacingFun() }
}

fun createSpacingBuilder(settings: CodeStyleSettings, builderUtil: KotlinSpacingBuilderUtil): KotlinSpacingBuilder {
    val kotlinSettings = settings.getCustomSettings(KotlinCodeStyleSettings::class.java)!!
    val kotlinCommonSettings = settings.getCommonSettings(KotlinLanguage.INSTANCE)!!

    return rules(settings, builderUtil) {
        val DECLARATIONS =
                TokenSet.create(PROPERTY, FUN, CLASS, OBJECT_DECLARATION, ENUM_ENTRY, SECONDARY_CONSTRUCTOR, CLASS_INITIALIZER)

        custom {
            inPosition(left = CLASS, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = CLASS, right = OBJECT_DECLARATION).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = OBJECT_DECLARATION, right = OBJECT_DECLARATION).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = OBJECT_DECLARATION, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUN, right = FUN).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = PROPERTY, right = FUN).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUN, right = PROPERTY).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = SECONDARY_CONSTRUCTOR, right = SECONDARY_CONSTRUCTOR).emptyLinesIfLineBreakInLeft(1)

            // Case left for alternative constructors
            inPosition(left = FUN, right = CLASS).emptyLinesIfLineBreakInLeft(1)

            inPosition(left = ENUM_ENTRY, right = ENUM_ENTRY).emptyLinesIfLineBreakInLeft(
                    emptyLines = 0, numberOfLineFeedsOtherwise = 0, numSpacesOtherwise = 1)

            inPosition(parent = CLASS_BODY, left = SEMICOLON).customRule { parent, left, right ->
                val klass = parent.node.treeParent.psi as? KtClass ?: return@customRule null
                if (klass.isEnum() && right.node.elementType in DECLARATIONS) {
                    createSpacing(0, minLineFeeds = 2, keepBlankLines = settings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                } else null
            }

            val parameterWithDocCommentRule = {
                parent: ASTBlock, left: ASTBlock, right: ASTBlock ->
                if (right.node.firstChildNode.elementType == KtTokens.DOC_COMMENT) {
                    createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = settings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                }
                else {
                    null
                }
            }
            inPosition(parent = VALUE_PARAMETER_LIST, right = VALUE_PARAMETER).customRule(parameterWithDocCommentRule)
        }

        simple {
            // ============ Line breaks ==============
            before(FILE_ANNOTATION_LIST).lineBreakInCode()
            after(FILE_ANNOTATION_LIST).blankLines(1)

            after(PACKAGE_DIRECTIVE).blankLines(1)
            between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).lineBreakInCode()
            after(IMPORT_LIST).blankLines(1)

            before(DOC_COMMENT).lineBreakInCode()
            between(PROPERTY, PROPERTY).lineBreakInCode()

            // CLASS - CLASS, CLASS - OBJECT_DECLARATION are exception
            between(CLASS, DECLARATIONS).blankLines(1)

            // FUN - FUN, FUN - PROPERTY, FUN - CLASS are exceptions
            between(FUN, DECLARATIONS).blankLines(1)

            // PROPERTY - PROPERTY, PROPERTY - FUN are exceptions
            between(PROPERTY, DECLARATIONS).blankLines(1)

            // OBJECT_DECLARATION - OBJECT_DECLARATION, CLASS - OBJECT_DECLARATION are exception
            between(OBJECT_DECLARATION, DECLARATIONS).blankLines(1)
            between(SECONDARY_CONSTRUCTOR, DECLARATIONS).blankLines(1)
            between(CLASS_INITIALIZER, DECLARATIONS).blankLines(1)

            // ENUM_ENTRY - ENUM_ENTRY is exception
            between(ENUM_ENTRY, DECLARATIONS).blankLines(1)

            between(ENUM_ENTRY, SEMICOLON).spaces(0)

            beforeInside(FUN, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(SECONDARY_CONSTRUCTOR, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(CLASS, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(OBJECT_DECLARATION, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            before(PROPERTY).lineBreakInCode()

            after(DOC_COMMENT).lineBreakInCode()

            // =============== Spacing ================
            betweenInside(LBRACE, RBRACE, CLASS_BODY).spaces(0)

            before(COMMA).spaceIf(kotlinCommonSettings.SPACE_BEFORE_COMMA)
            after(COMMA).spaceIf(kotlinCommonSettings.SPACE_AFTER_COMMA)

            around(TokenSet.create(EQ, MULTEQ, DIVEQ, PLUSEQ, MINUSEQ, PERCEQ)).spaceIf(kotlinCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            around(TokenSet.create(ANDAND, OROR)).spaceIf(kotlinCommonSettings.SPACE_AROUND_LOGICAL_OPERATORS)
            around(TokenSet.create(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ)).spaceIf(kotlinCommonSettings.SPACE_AROUND_EQUALITY_OPERATORS)
            aroundInside(TokenSet.create(LT, GT, LTEQ, GTEQ), BINARY_EXPRESSION).spaceIf(kotlinCommonSettings.SPACE_AROUND_RELATIONAL_OPERATORS)
            aroundInside(TokenSet.create(PLUS, MINUS), BINARY_EXPRESSION).spaceIf(kotlinCommonSettings.SPACE_AROUND_ADDITIVE_OPERATORS)
            aroundInside(TokenSet.create(MUL, DIV, PERC), BINARY_EXPRESSION).spaceIf(kotlinCommonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS)
            around(TokenSet.create(PLUSPLUS, MINUSMINUS, EXCLEXCL, MINUS, PLUS, EXCL)).spaceIf(kotlinCommonSettings.SPACE_AROUND_UNARY_OPERATOR)
            around(ELVIS).spaces(1)
            around(RANGE).spaceIf(kotlinSettings.SPACE_AROUND_RANGE)

            after(MODIFIER_LIST).spaces(1)

            beforeInside(IDENTIFIER, CLASS).spaces(1)
            beforeInside(IDENTIFIER, OBJECT_DECLARATION).spaces(1)

            after(VAL_KEYWORD).spaces(1)
            after(VAR_KEYWORD).spaces(1)
            beforeInside(PROPERTY_ACCESSOR, PROPERTY).spacing(1, 0, 0, true, 0)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, PROPERTY).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, PROPERTY).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, PROPERTY).spacing(0, 0, 0, false, 0)

            betweenInside(RETURN_KEYWORD, LABEL_QUALIFIER, RETURN).spaces(0)
            afterInside(RETURN_KEYWORD, RETURN).spaces(1)
            afterInside(LABEL_QUALIFIER, RETURN).spaces(1)

            betweenInside(FUN_KEYWORD, VALUE_PARAMETER_LIST, FUN).spacing(0, 0, 0, false, 0)
            after(FUN_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, TYPE_REFERENCE, FUN).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, FUN).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, FUN).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, FUN).spacing(0, 0, 0, false, 0)
            afterInside(IDENTIFIER, FUN).spacing(0, 0, 0, false, 0)

            around(AS_KEYWORD).spaces(1)
            around(IS_KEYWORD).spaces(1)
            around(NOT_IS).spaces(1)
            around(IN_KEYWORD).spaces(1)
            around(NOT_IN).spaces(1)
            aroundInside(IDENTIFIER, BINARY_EXPRESSION).spaces(1)

            // before LPAR in constructor(): this() {}
            after(CONSTRUCTOR_DELEGATION_REFERENCE).spacing(0, 0, 0, false, 0)

            // class A() - no space before LPAR of PRIMARY_CONSTRUCTOR
            // class A private() - one space before modifier
            custom {
                inPosition(right = PRIMARY_CONSTRUCTOR).customRule { p, l, r ->
                    val spacesCount = if (r.node.findLeafElementAt(0)?.elementType != LPAR) 1 else 0
                    createSpacing(spacesCount, minLineFeeds = 0, keepLineBreaks = true, keepBlankLines = 0)
                }
            }

            afterInside(CONSTRUCTOR_KEYWORD, PRIMARY_CONSTRUCTOR).spaces(0)
            betweenInside(IDENTIFIER, TYPE_PARAMETER_LIST, CLASS).spaces(0)

            aroundInside(DOT, DOT_QUALIFIED_EXPRESSION).spaces(0)
            aroundInside(SAFE_ACCESS, SAFE_ACCESS_EXPRESSION).spaces(0)

            between(MODIFIERS_LIST_ENTRIES, MODIFIERS_LIST_ENTRIES).spaces(1)

            after(LBRACKET).spaces(0)
            before(RBRACKET).spaces(0)
            after(LPAR).spaces(0)
            before(RPAR).spaces(0)

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
            betweenInside(WHEN_KEYWORD, LPAR, WHEN).spacing(1, 1, 0, false, 0)

            betweenInside(LPAR, VALUE_PARAMETER, FOR).spaces(0)
            betweenInside(LPAR, DESTRUCTURING_DECLARATION, FOR).spaces(0)
            betweenInside(LOOP_RANGE, RPAR, FOR).spaces(0)

            after(LONG_TEMPLATE_ENTRY_START).spaces(0)
            before(LONG_TEMPLATE_ENTRY_END).spaces(0)

            afterInside(ANNOTATION_ENTRY, ANNOTATED_EXPRESSION).spaces(1)

            before(SEMICOLON).spaces(0)

            beforeInside(INITIALIZER_LIST, ENUM_ENTRY).spaces(0)

            beforeInside(QUEST, NULLABLE_TYPE).spaces(0)

            val TYPE_COLON_ELEMENTS = TokenSet.create(PROPERTY, FUN, VALUE_PARAMETER, DESTRUCTURING_DECLARATION_ENTRY, FUNCTION_LITERAL)
            beforeInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(kotlinSettings.SPACE_BEFORE_TYPE_COLON) }
            afterInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(kotlinSettings.SPACE_AFTER_TYPE_COLON) }

            val EXTEND_COLON_ELEMENTS =
                    TokenSet.create(TYPE_CONSTRAINT, CLASS, OBJECT_DECLARATION, TYPE_PARAMETER, ENUM_ENTRY, SECONDARY_CONSTRUCTOR)
            beforeInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(kotlinSettings.SPACE_BEFORE_EXTEND_COLON) }
            afterInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(kotlinSettings.SPACE_AFTER_EXTEND_COLON) }

            beforeInside(ARROW, FUNCTION_LITERAL).spaceIf(kotlinSettings.SPACE_BEFORE_LAMBDA_ARROW)

            aroundInside(ARROW, FUNCTION_TYPE).spaceIf(kotlinSettings.SPACE_AROUND_FUNCTION_TYPE_ARROW)

            before(VALUE_ARGUMENT_LIST).spaces(0)
            between(VALUE_ARGUMENT_LIST, LAMBDA_ARGUMENT).spaces(1)
            betweenInside(REFERENCE_EXPRESSION, LAMBDA_ARGUMENT, CALL_EXPRESSION).spaces(1)
            betweenInside(TYPE_ARGUMENT_LIST, LAMBDA_ARGUMENT, CALL_EXPRESSION).spaces(1)

            between(WHEN_ENTRY, WHEN_ENTRY).lineBreakInCode()
        }
        custom {

            fun CustomSpacingBuilder.ruleForKeywordOnNewLine(
                    shouldBeOnNewLine: Boolean,
                    keyword: IElementType,
                    parent: IElementType,
                    afterBlockFilter: (wordParent: ASTNode, block: ASTNode) -> Boolean = { keywordParent, block -> true }) {
                if (shouldBeOnNewLine) {
                    inPosition(parent = parent, right = keyword)
                            .lineBreakIfLineBreakInParent(numSpacesOtherwise = 1, allowBlankLines = false)
                }
                else {
                    inPosition(parent = parent, right = keyword).customRule {
                        parent, left, right ->

                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(right.node)
                        val leftBlock = if (
                                previousLeaf != null &&
                                previousLeaf.elementType == RBRACE &&
                                previousLeaf.treeParent?.elementType == BLOCK) {
                                previousLeaf.treeParent!!
                        } else null

                        val removeLineBreaks = leftBlock != null && afterBlockFilter(right.node?.treeParent!!, leftBlock)
                        createSpacing(1, minLineFeeds = 0, keepLineBreaks = !removeLineBreaks, keepBlankLines = 0)
                    }
                }
            }

            ruleForKeywordOnNewLine(kotlinCommonSettings.ELSE_ON_NEW_LINE, keyword = ELSE_KEYWORD, parent = IF) { keywordParent, block ->
                block.treeParent?.elementType == THEN && block.treeParent?.treeParent == keywordParent
            }
            ruleForKeywordOnNewLine(kotlinCommonSettings.WHILE_ON_NEW_LINE, keyword = WHILE_KEYWORD, parent = DO_WHILE) { keywordParent, block ->
                block.treeParent?.elementType == BODY && block.treeParent?.treeParent == keywordParent
            }
            ruleForKeywordOnNewLine(kotlinCommonSettings.CATCH_ON_NEW_LINE, keyword = CATCH, parent = TRY)
            ruleForKeywordOnNewLine(kotlinCommonSettings.FINALLY_ON_NEW_LINE, keyword = FINALLY, parent = TRY)


            fun spacingForLeftBrace(block: ASTNode?, blockType: IElementType = BLOCK): Spacing? {
                if (block != null && block.elementType == blockType) {
                    val leftBrace = block.findChildByType(LBRACE)
                    if (leftBrace != null) {
                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(leftBrace)
                        val isAfterEolComment = previousLeaf != null && (previousLeaf.elementType == EOL_COMMENT)
                        val keepLineBreaks = kotlinSettings.LBRACE_ON_NEXT_LINE || isAfterEolComment
                        val minimumLF = if (kotlinSettings.LBRACE_ON_NEXT_LINE) 1 else 0
                        return createSpacing(1, minLineFeeds = minimumLF, keepLineBreaks = keepLineBreaks, keepBlankLines = 0)
                    }
                }
                return createSpacing(1)
            }

            fun leftBraceRule(blockType: IElementType = BLOCK) = {
                parent: ASTBlock, left: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.node, blockType)
            }

            val leftBraceRuleIfBlockIsWrapped = {
                parent: ASTBlock, left: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.node!!.firstChildNode)
            }

            if (kotlinCommonSettings.KEEP_FIRST_COLUMN_COMMENT) {
                inPosition(parent = null, left = EOL_COMMENT, right = EOL_COMMENT).customRule { parent, left, right ->
                    val nodeBeforeRight = right.node.treePrev
                    if (nodeBeforeRight is PsiWhiteSpace && !nodeBeforeRight.textContains('\n')) {
                        // Several line comments happened to be generated in one line
                        createSpacing(0, minLineFeeds = 1)
                    }
                    else {
                        null
                    }
                }

                inPosition(rightSet = TokenSet.create(EOL_COMMENT, BLOCK_COMMENT)).spacing(
                        Spacing.createKeepingFirstColumnSpacing(0, Integer.MAX_VALUE, settings.KEEP_LINE_BREAKS, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE))
            }

            // Add space after a semicolon if there is another child at the same line
            inPosition(left = SEMICOLON).customRule { parent, left, right ->
                val nodeAfterLeft = left.node.treeNext
                if (nodeAfterLeft is PsiWhiteSpace && !nodeAfterLeft.textContains('\n')) {
                    createSpacing(1)
                }
                else {
                    null
                }
            }

            inPosition(parent = IF, right = THEN).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = IF, right = ELSE).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = FOR, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)
            inPosition(parent = DO_WHILE, right = BODY).customRule(leftBraceRuleIfBlockIsWrapped)

            inPosition(parent = TRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = CATCH, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = FINALLY, right = BLOCK).customRule(leftBraceRule())

            inPosition(parent = FUN, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = SECONDARY_CONSTRUCTOR, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = CLASS_INITIALIZER, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = PROPERTY_ACCESSOR, right = BLOCK).customRule(leftBraceRule())

            inPosition(right = CLASS_BODY).customRule(leftBraceRule(blockType = CLASS_BODY))

            inPosition(parent = WHEN_ENTRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = WHEN, right = LBRACE).customRule {
                parent, left, right ->
                spacingForLeftBrace(block = parent.node, blockType = WHEN)
            }

            val spacesInSimpleFunction = if (kotlinSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD) 1 else 0
            inPosition(parent = FUNCTION_LITERAL,
                       left = LBRACE,
                       right = BLOCK)
                    .lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(parent = FUNCTION_LITERAL,
                       left = ARROW,
                       right = BLOCK)
                    .lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)

            inPosition(parent = FUNCTION_LITERAL,
                       left = LBRACE,
                       right = RBRACE)
                    .spacing(createSpacing(minSpaces = 0, maxSpaces = 1))

            inPosition(parent = FUNCTION_LITERAL,
                       right = RBRACE)
                    .lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(parent = FUNCTION_LITERAL,
                       left = LBRACE)
                    .customRule {
                parent, left, right ->
                val rightNode = right.node!!
                val rightType = rightNode.elementType
                var numSpaces = spacesInSimpleFunction
                if (rightType == VALUE_PARAMETER_LIST) {
                    val firstParamListNode = rightNode.firstChildNode
                    if (firstParamListNode != null && firstParamListNode.elementType == LPAR) {
                        // Don't put space for situation {<here>(a: Int) -> a }
                        numSpaces = 0
                    }
                }

                createSpacing(numSpaces)
            }

            inPosition(parent = CLASS_BODY, right = RBRACE).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)
        }

        simple {
            afterInside(LBRACE, BLOCK).lineBreakInCode()
            beforeInside(RBRACE, BLOCK).lineBreakInCode()
            beforeInside(RBRACE, WHEN).lineBreakInCode()
            between(RPAR, BODY).spaces(1)

            // if when entry has block, spacing after arrow should be set by lbrace rule
            aroundInside(ARROW, WHEN_ENTRY).spaceIf(kotlinSettings.SPACE_AROUND_WHEN_ARROW)

            after(EOL_COMMENT).lineBreakInCode()
        }
    }
}