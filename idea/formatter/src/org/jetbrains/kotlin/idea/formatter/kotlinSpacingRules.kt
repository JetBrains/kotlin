/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter

import com.intellij.formatting.ASTBlock
import com.intellij.formatting.DependentSpacingRule
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.SpacingBuilder.RuleBuilder
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.text.TextRangeUtil
import org.jetbrains.kotlin.KtNodeTypes.*
import org.jetbrains.kotlin.idea.formatter.KotlinSpacingBuilder.CustomSpacingBuilder
import org.jetbrains.kotlin.idea.util.requireNode
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.children
import org.jetbrains.kotlin.psi.psiUtil.isObjectLiteral
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.textRangeWithoutComments

val MODIFIERS_LIST_ENTRIES = TokenSet.orSet(TokenSet.create(ANNOTATION_ENTRY, ANNOTATION), MODIFIER_KEYWORDS)

val EXTEND_COLON_ELEMENTS =
    TokenSet.create(TYPE_CONSTRAINT, CLASS, OBJECT_DECLARATION, TYPE_PARAMETER, ENUM_ENTRY, SECONDARY_CONSTRUCTOR)

val DECLARATIONS = TokenSet.create(PROPERTY, FUN, CLASS, OBJECT_DECLARATION, ENUM_ENTRY, SECONDARY_CONSTRUCTOR, CLASS_INITIALIZER)

fun SpacingBuilder.beforeInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> beforeInside(element, inType).spacingFun() }
}

fun SpacingBuilder.afterInside(element: IElementType, tokenSet: TokenSet, spacingFun: RuleBuilder.() -> Unit) {
    tokenSet.types.forEach { inType -> afterInside(element, inType).spacingFun() }
}

fun RuleBuilder.spacesNoLineBreak(spaces: Int): SpacingBuilder? =
    spacing(spaces, spaces, 0, false, 0)

fun createSpacingBuilder(settings: CodeStyleSettings, builderUtil: KotlinSpacingBuilderUtil): KotlinSpacingBuilder {
    val kotlinCommonSettings = settings.kotlinCommonSettings
    val kotlinCustomSettings = settings.kotlinCustomSettings
    return rules(kotlinCommonSettings, builderUtil) {
        simple {
            before(FILE_ANNOTATION_LIST).lineBreakInCode()
            between(IMPORT_DIRECTIVE, IMPORT_DIRECTIVE).lineBreakInCode()
            after(IMPORT_LIST).blankLines(1)
        }

        custom {
            fun commentSpacing(minSpaces: Int): Spacing {
                if (kotlinCommonSettings.KEEP_FIRST_COLUMN_COMMENT) {
                    return Spacing.createKeepingFirstColumnSpacing(
                        minSpaces,
                        Int.MAX_VALUE,
                        settings.KEEP_LINE_BREAKS,
                        kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
                    )
                }
                return Spacing.createSpacing(
                    minSpaces,
                    Int.MAX_VALUE,
                    0,
                    settings.KEEP_LINE_BREAKS,
                    kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
                )
            }

            // Several line comments happened to be generated in one line
            inPosition(parent = null, left = EOL_COMMENT, right = EOL_COMMENT).customRule { _, _, right ->
                val nodeBeforeRight = right.requireNode().treePrev
                if (nodeBeforeRight is PsiWhiteSpace && !nodeBeforeRight.textContains('\n')) {
                    createSpacing(0, minLineFeeds = 1)
                } else {
                    null
                }
            }

            inPosition(right = BLOCK_COMMENT).spacing(commentSpacing(0))
            inPosition(right = EOL_COMMENT).spacing(commentSpacing(1))
            inPosition(parent = FUNCTION_LITERAL, right = BLOCK).customRule { _, _, right ->
                when (right.node?.children()?.firstOrNull()?.elementType) {
                    BLOCK_COMMENT -> commentSpacing(0)
                    EOL_COMMENT -> commentSpacing(1)
                    else -> null
                }
            }
        }

        simple {
            after(FILE_ANNOTATION_LIST).blankLines(1)
            after(PACKAGE_DIRECTIVE).blankLines(1)
        }

        custom {
            inPosition(leftSet = DECLARATIONS, rightSet = DECLARATIONS).customRule(fun(
                _: ASTBlock,
                _: ASTBlock,
                right: ASTBlock
            ): Spacing? {
                val node = right.node ?: return null
                val elementStart = node.startOfDeclaration() ?: return null
                return if (StringUtil.containsLineBreak(
                        node.text.subSequence(0, elementStart.startOffset - node.startOffset).trimStart()
                    )
                )
                    createSpacing(0, minLineFeeds = 2)
                else
                    null
            })

            inPosition(left = CLASS, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = CLASS, right = OBJECT_DECLARATION).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = OBJECT_DECLARATION, right = OBJECT_DECLARATION).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = OBJECT_DECLARATION, right = CLASS).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUN, right = FUN).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = PROPERTY, right = FUN).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = FUN, right = PROPERTY).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = SECONDARY_CONSTRUCTOR, right = SECONDARY_CONSTRUCTOR).emptyLinesIfLineBreakInLeft(1)
            inPosition(left = TYPEALIAS, right = TYPEALIAS).emptyLinesIfLineBreakInLeft(1)

            // Case left for alternative constructors
            inPosition(left = FUN, right = CLASS).emptyLinesIfLineBreakInLeft(1)

            inPosition(left = ENUM_ENTRY, right = ENUM_ENTRY).emptyLinesIfLineBreakInLeft(
                emptyLines = 0, numberOfLineFeedsOtherwise = 0, numSpacesOtherwise = 1
            )

            inPosition(parent = CLASS_BODY, left = SEMICOLON).customRule { parent, _, right ->
                val klass = parent.requireNode().treeParent.psi as? KtClass ?: return@customRule null
                if (klass.isEnum() && right.requireNode().elementType in DECLARATIONS) {
                    createSpacing(0, minLineFeeds = 2, keepBlankLines = settings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                } else null
            }

            inPosition(parent = CLASS_BODY, left = LBRACE).customRule { parent, left, right ->
                if (right.requireNode().elementType == RBRACE) {
                    return@customRule createSpacing(0)
                }
                val classBody = parent.requireNode().psi as KtClassBody
                val parentPsi = classBody.parent as? KtClassOrObject ?: return@customRule null
                if (kotlinCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER == 0 || parentPsi.isObjectLiteral()) {
                    null
                } else {
                    val minLineFeeds = if (right.requireNode().elementType == FUN || right.requireNode().elementType == PROPERTY)
                        1
                    else
                        0

                    builderUtil.createLineFeedDependentSpacing(
                        1, 1, minLineFeeds,
                        settings.KEEP_LINE_BREAKS, settings.KEEP_BLANK_LINES_IN_DECLARATIONS,
                        TextRange(parentPsi.textRange.startOffset, left.requireNode().psi.textRange.startOffset),
                        DependentSpacingRule(DependentSpacingRule.Trigger.HAS_LINE_FEEDS)
                            .registerData(
                                DependentSpacingRule.Anchor.MIN_LINE_FEEDS,
                                kotlinCommonSettings.BLANK_LINES_AFTER_CLASS_HEADER + 1
                            )
                    )
                }
            }

            val parameterWithDocCommentRule = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                if (right.requireNode().firstChildNode.elementType == DOC_COMMENT) {
                    createSpacing(0, minLineFeeds = 1, keepLineBreaks = true, keepBlankLines = settings.KEEP_BLANK_LINES_IN_DECLARATIONS)
                } else {
                    null
                }
            }
            inPosition(parent = VALUE_PARAMETER_LIST, right = VALUE_PARAMETER).customRule(parameterWithDocCommentRule)

            inPosition(parent = PROPERTY, right = PROPERTY_ACCESSOR).customRule { parent, _, _ ->
                val startNode = parent.requireNode().psi.firstChild
                    .siblings()
                    .dropWhile { it is PsiComment || it is PsiWhiteSpace }.firstOrNull() ?: parent.requireNode().psi
                Spacing.createDependentLFSpacing(
                    1, 1,
                    TextRange(startNode.textRange.startOffset, parent.textRange.endOffset),
                    false, 0
                )
            }

            if (!kotlinCustomSettings.ALLOW_TRAILING_COMMA) {
                inPosition(parent = VALUE_ARGUMENT_LIST, left = LPAR).customRule { parent, _, _ ->
                    if (kotlinCommonSettings.CALL_PARAMETERS_LPAREN_ON_NEXT_LINE && needWrapArgumentList(parent.requireNode().psi)) {
                        Spacing.createDependentLFSpacing(
                            0, 0,
                            excludeLambdasAndObjects(parent),
                            commonCodeStyleSettings.KEEP_LINE_BREAKS,
                            commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                        )
                    } else {
                        createSpacing(0)
                    }
                }

                inPosition(parent = VALUE_ARGUMENT_LIST, right = RPAR).customRule { parent, left, _ ->
                    when {
                        kotlinCommonSettings.CALL_PARAMETERS_RPAREN_ON_NEXT_LINE ->
                            Spacing.createDependentLFSpacing(
                                0, 0,
                                excludeLambdasAndObjects(parent),
                                commonCodeStyleSettings.KEEP_LINE_BREAKS,
                                commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                            )
                        left.requireNode().elementType == COMMA -> // incomplete call being edited
                            createSpacing(1)
                        else ->
                            createSpacing(0)
                    }
                }
            }

            inPosition(left = CONDITION, right = RPAR).customRule { _, left, _ ->
                if (kotlinCustomSettings.IF_RPAREN_ON_NEW_LINE) {
                    Spacing.createDependentLFSpacing(
                        0, 0,
                        excludeLambdasAndObjects(left),
                        commonCodeStyleSettings.KEEP_LINE_BREAKS,
                        commonCodeStyleSettings.KEEP_BLANK_LINES_IN_CODE
                    )
                } else {
                    createSpacing(0)
                }
            }
        }

        simple {
            // ============ Line breaks ==============
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

            // TYPEALIAS - TYPEALIAS is an exception
            between(TYPEALIAS, DECLARATIONS).blankLines(1)

            // ENUM_ENTRY - ENUM_ENTRY is exception
            between(ENUM_ENTRY, DECLARATIONS).blankLines(1)

            between(ENUM_ENTRY, SEMICOLON).spaces(0)

            beforeInside(FUN, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(SECONDARY_CONSTRUCTOR, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(CLASS, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(OBJECT_DECLARATION, TokenSet.create(BODY, CLASS_BODY)).lineBreakInCode()
            beforeInside(PROPERTY, WHEN).spaces(0)
            beforeInside(PROPERTY, LABELED_EXPRESSION).spacesNoLineBreak(1)
            before(PROPERTY).lineBreakInCode()

            after(DOC_COMMENT).lineBreakInCode()

            // =============== Spacing ================
            before(COMMA).spacesNoLineBreak(if (kotlinCommonSettings.SPACE_BEFORE_COMMA) 1 else 0)
            after(COMMA).spaceIf(kotlinCommonSettings.SPACE_AFTER_COMMA)

            val spacesAroundAssignment = if (kotlinCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS) 1 else 0
            beforeInside(EQ, PROPERTY).spacesNoLineBreak(spacesAroundAssignment)
            beforeInside(EQ, FUN).spacing(spacesAroundAssignment, spacesAroundAssignment, 0, false, 0)

            around(
                TokenSet.create(EQ, MULTEQ, DIVEQ, PLUSEQ, MINUSEQ, PERCEQ)
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_ASSIGNMENT_OPERATORS)
            around(TokenSet.create(ANDAND, OROR)).spaceIf(kotlinCommonSettings.SPACE_AROUND_LOGICAL_OPERATORS)
            around(TokenSet.create(EQEQ, EXCLEQ, EQEQEQ, EXCLEQEQEQ)).spaceIf(kotlinCommonSettings.SPACE_AROUND_EQUALITY_OPERATORS)
            aroundInside(
                TokenSet.create(LT, GT, LTEQ, GTEQ), BINARY_EXPRESSION
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_RELATIONAL_OPERATORS)
            aroundInside(TokenSet.create(PLUS, MINUS), BINARY_EXPRESSION).spaceIf(kotlinCommonSettings.SPACE_AROUND_ADDITIVE_OPERATORS)
            aroundInside(
                TokenSet.create(MUL, DIV, PERC), BINARY_EXPRESSION
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_MULTIPLICATIVE_OPERATORS)
            around(
                TokenSet.create(PLUSPLUS, MINUSMINUS, EXCLEXCL, MINUS, PLUS, EXCL)
            ).spaceIf(kotlinCommonSettings.SPACE_AROUND_UNARY_OPERATOR)
            before(ELVIS).spaces(1)
            after(ELVIS).spacesNoLineBreak(1)
            around(RANGE).spaceIf(kotlinCustomSettings.SPACE_AROUND_RANGE)

            after(MODIFIER_LIST).spaces(1)

            beforeInside(IDENTIFIER, CLASS).spaces(1)
            beforeInside(IDENTIFIER, OBJECT_DECLARATION).spaces(1)

            after(VAL_KEYWORD).spaces(1)
            after(VAR_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, PROPERTY).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, PROPERTY).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, PROPERTY).spacing(0, 0, 0, false, 0)

            betweenInside(RETURN_KEYWORD, LABEL_QUALIFIER, RETURN).spaces(0)
            afterInside(RETURN_KEYWORD, RETURN).spaces(1)
            afterInside(LABEL_QUALIFIER, RETURN).spaces(1)
            betweenInside(LABEL_QUALIFIER, EOL_COMMENT, LABELED_EXPRESSION).spacing(
                0, Int.MAX_VALUE, 0, true, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
            )
            betweenInside(LABEL_QUALIFIER, BLOCK_COMMENT, LABELED_EXPRESSION).spacing(
                0, Int.MAX_VALUE, 0, true, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
            )
            betweenInside(LABEL_QUALIFIER, LAMBDA_EXPRESSION, LABELED_EXPRESSION).spaces(0)
            afterInside(LABEL_QUALIFIER, LABELED_EXPRESSION).spaces(1)

            betweenInside(FUN_KEYWORD, VALUE_PARAMETER_LIST, FUN).spacing(0, 0, 0, false, 0)
            after(FUN_KEYWORD).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, TYPE_REFERENCE, FUN).spaces(1)
            betweenInside(TYPE_PARAMETER_LIST, IDENTIFIER, FUN).spaces(1)
            betweenInside(TYPE_REFERENCE, DOT, FUN).spacing(0, 0, 0, false, 0)
            betweenInside(DOT, IDENTIFIER, FUN).spacing(0, 0, 0, false, 0)
            afterInside(IDENTIFIER, FUN).spacing(0, 0, 0, false, 0)
            aroundInside(DOT, USER_TYPE).spaces(0)

            around(AS_KEYWORD).spaces(1)
            around(AS_SAFE).spaces(1)
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
                inPosition(right = PRIMARY_CONSTRUCTOR).customRule { _, _, r ->
                    val spacesCount = if (r.requireNode().findLeafElementAt(0)?.elementType != LPAR) 1 else 0
                    createSpacing(spacesCount, minLineFeeds = 0, keepLineBreaks = true, keepBlankLines = 0)
                }
            }

            afterInside(CONSTRUCTOR_KEYWORD, PRIMARY_CONSTRUCTOR).spaces(0)
            betweenInside(IDENTIFIER, TYPE_PARAMETER_LIST, CLASS).spaces(0)

            beforeInside(DOT, DOT_QUALIFIED_EXPRESSION).spaces(0)
            afterInside(DOT, DOT_QUALIFIED_EXPRESSION).spacesNoLineBreak(0)
            beforeInside(SAFE_ACCESS, SAFE_ACCESS_EXPRESSION).spaces(0)
            afterInside(SAFE_ACCESS, SAFE_ACCESS_EXPRESSION).spacesNoLineBreak(0)

            between(MODIFIERS_LIST_ENTRIES, MODIFIERS_LIST_ENTRIES).spaces(1)

            after(LBRACKET).spaces(0)
            before(RBRACKET).spaces(0)

            afterInside(LPAR, VALUE_PARAMETER_LIST).spaces(0, kotlinCommonSettings.METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE)
            beforeInside(RPAR, VALUE_PARAMETER_LIST).spaces(0, kotlinCommonSettings.METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE)
            afterInside(LT, TYPE_PARAMETER_LIST).spaces(0)
            beforeInside(GT, TYPE_PARAMETER_LIST).spaces(0)
            afterInside(LT, TYPE_ARGUMENT_LIST).spaces(0)
            beforeInside(GT, TYPE_ARGUMENT_LIST).spaces(0)
            before(TYPE_ARGUMENT_LIST).spaces(0)

            after(LPAR).spaces(0)
            before(RPAR).spaces(0)

            betweenInside(FOR_KEYWORD, LPAR, FOR).spaceIf(kotlinCommonSettings.SPACE_BEFORE_FOR_PARENTHESES)
            betweenInside(IF_KEYWORD, LPAR, IF).spaceIf(kotlinCommonSettings.SPACE_BEFORE_IF_PARENTHESES)
            betweenInside(WHILE_KEYWORD, LPAR, WHILE).spaceIf(kotlinCommonSettings.SPACE_BEFORE_WHILE_PARENTHESES)
            betweenInside(WHILE_KEYWORD, LPAR, DO_WHILE).spaceIf(kotlinCommonSettings.SPACE_BEFORE_WHILE_PARENTHESES)
            betweenInside(WHEN_KEYWORD, LPAR, WHEN).spaceIf(kotlinCustomSettings.SPACE_BEFORE_WHEN_PARENTHESES)
            betweenInside(CATCH_KEYWORD, VALUE_PARAMETER_LIST, CATCH).spaceIf(kotlinCommonSettings.SPACE_BEFORE_CATCH_PARENTHESES)

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
            beforeInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_BEFORE_TYPE_COLON) }
            afterInside(COLON, TYPE_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_AFTER_TYPE_COLON) }

            afterInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_AFTER_EXTEND_COLON) }

            beforeInside(ARROW, FUNCTION_LITERAL).spaceIf(kotlinCustomSettings.SPACE_BEFORE_LAMBDA_ARROW)

            aroundInside(ARROW, FUNCTION_TYPE).spaceIf(kotlinCustomSettings.SPACE_AROUND_FUNCTION_TYPE_ARROW)

            before(VALUE_ARGUMENT_LIST).spaces(0)
            between(VALUE_ARGUMENT_LIST, LAMBDA_ARGUMENT).spaces(1)
            betweenInside(REFERENCE_EXPRESSION, LAMBDA_ARGUMENT, CALL_EXPRESSION).spaces(1)
            betweenInside(TYPE_ARGUMENT_LIST, LAMBDA_ARGUMENT, CALL_EXPRESSION).spaces(1)

            around(COLONCOLON).spaces(0)

            around(BY_KEYWORD).spaces(1)
            betweenInside(IDENTIFIER, PROPERTY_DELEGATE, PROPERTY).spaces(1)
            betweenInside(TYPE_REFERENCE, PROPERTY_DELEGATE, PROPERTY).spaces(1)

            before(INDICES).spaces(0)
            before(WHERE_KEYWORD).spaces(1)

            afterInside(GET_KEYWORD, PROPERTY_ACCESSOR).spaces(0)
            afterInside(SET_KEYWORD, PROPERTY_ACCESSOR).spaces(0)
        }
        custom {

            fun CustomSpacingBuilder.ruleForKeywordOnNewLine(
                shouldBeOnNewLine: Boolean,
                keyword: IElementType,
                parent: IElementType,
                afterBlockFilter: (wordParent: ASTNode, block: ASTNode) -> Boolean = { _, _ -> true }
            ) {
                if (shouldBeOnNewLine) {
                    inPosition(parent = parent, right = keyword)
                        .lineBreakIfLineBreakInParent(numSpacesOtherwise = 1, allowBlankLines = false)
                } else {
                    inPosition(parent = parent, right = keyword).customRule { _, _, right ->

                        val previousLeaf = builderUtil.getPreviousNonWhitespaceLeaf(right.requireNode())
                        val leftBlock = if (
                            previousLeaf != null &&
                            previousLeaf.elementType == RBRACE &&
                            previousLeaf.treeParent?.elementType == BLOCK
                        ) {
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
            ruleForKeywordOnNewLine(
                kotlinCommonSettings.WHILE_ON_NEW_LINE,
                keyword = WHILE_KEYWORD,
                parent = DO_WHILE
            ) { keywordParent, block ->
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
                        val keepLineBreaks = kotlinCustomSettings.LBRACE_ON_NEXT_LINE || isAfterEolComment
                        val minimumLF = if (kotlinCustomSettings.LBRACE_ON_NEXT_LINE) 1 else 0
                        return createSpacing(1, minLineFeeds = minimumLF, keepLineBreaks = keepLineBreaks, keepBlankLines = 0)
                    }
                }
                return createSpacing(1)
            }

            fun leftBraceRule(blockType: IElementType = BLOCK) = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.node, blockType)
            }

            val leftBraceRuleIfBlockIsWrapped = { _: ASTBlock, _: ASTBlock, right: ASTBlock ->
                spacingForLeftBrace(right.requireNode().firstChildNode)
            }

            // Add space after a semicolon if there is another child at the same line
            inPosition(left = SEMICOLON).customRule { _, left, _ ->
                val nodeAfterLeft = left.requireNode().treeNext
                if (nodeAfterLeft is PsiWhiteSpace && !nodeAfterLeft.textContains('\n')) {
                    createSpacing(1)
                } else {
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

            inPosition(left = WHEN_ENTRY, right = WHEN_ENTRY).customRule { _, left, right ->
                val leftEntry = left.requireNode().psi as KtWhenEntry
                val rightEntry = right.requireNode().psi as KtWhenEntry
                val blankLines = if (leftEntry.expression is KtBlockExpression || rightEntry.expression is KtBlockExpression)
                    settings.kotlinCustomSettings.BLANK_LINES_AROUND_BLOCK_WHEN_BRANCHES
                else
                    0

                createSpacing(0, minLineFeeds = blankLines + 1)
            }

            inPosition(parent = WHEN_ENTRY, right = BLOCK).customRule(leftBraceRule())
            inPosition(parent = WHEN, right = LBRACE).customRule { parent, _, _ ->
                spacingForLeftBrace(block = parent.requireNode(), blockType = WHEN)
            }

            inPosition(left = LBRACE, right = WHEN_ENTRY).customRule { _, _, _ ->
                createSpacing(0, minLineFeeds = 1)
            }

            val spacesInSimpleFunction = if (kotlinCustomSettings.INSERT_WHITESPACES_IN_SIMPLE_ONE_LINE_METHOD) 1 else 0
            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE,
                right = BLOCK
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = ARROW,
                right = BLOCK
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE,
                right = RBRACE
            ).spacing(createSpacing(minSpaces = 0, maxSpaces = 1))

            inPosition(
                parent = FUNCTION_LITERAL,
                right = RBRACE
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = spacesInSimpleFunction)

            inPosition(
                parent = FUNCTION_LITERAL,
                left = LBRACE
            ).customRule { _, _, right ->
                val rightNode = right.requireNode()
                val rightType = rightNode.elementType
                if (rightType == VALUE_PARAMETER_LIST) {
                    createSpacing(spacesInSimpleFunction, keepLineBreaks = false)
                } else {
                    createSpacing(spacesInSimpleFunction)
                }
            }

            inPosition(parent = CLASS_BODY, right = RBRACE).customRule { parent, _, _ ->
                kotlinCommonSettings.createSpaceBeforeRBrace(1, parent.textRange)
            }

            inPosition(parent = BLOCK, right = RBRACE).customRule { block, left, _ ->
                val psiElement = block.requireNode().treeParent.psi

                val empty = left.requireNode().elementType == LBRACE

                when (psiElement) {
                    is KtFunction -> {
                        if (psiElement.name != null && !empty) return@customRule null
                    }
                    is KtPropertyAccessor ->
                        if (!empty) return@customRule null
                    else ->
                        return@customRule null
                }

                val spaces = if (empty) 0 else spacesInSimpleFunction
                kotlinCommonSettings.createSpaceBeforeRBrace(spaces, psiElement.textRangeWithoutComments)
            }

            inPosition(parent = BLOCK, left = LBRACE).customRule { parent, _, _ ->
                val psiElement = parent.requireNode().treeParent.psi
                val funNode = psiElement as? KtFunction ?: return@customRule null

                if (funNode.name != null) return@customRule null

                // Empty block is covered in above rule
                Spacing.createDependentLFSpacing(
                    spacesInSimpleFunction, spacesInSimpleFunction, funNode.textRangeWithoutComments,
                    kotlinCommonSettings.KEEP_LINE_BREAKS,
                    kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE
                )
            }

            inPosition(parentSet = EXTEND_COLON_ELEMENTS, left = PRIMARY_CONSTRUCTOR, right = COLON).customRule { _, left, _ ->
                val primaryConstructor = left.requireNode().psi as KtPrimaryConstructor
                val rightParenthesis = primaryConstructor.valueParameterList?.rightParenthesis
                val prevSibling = rightParenthesis?.prevSibling
                val spaces = if (kotlinCustomSettings.SPACE_BEFORE_EXTEND_COLON) 1 else 0
                // TODO This should use DependentSpacingRule, but it doesn't set keepLineBreaks to false if max LFs is 0
                if ((prevSibling as? PsiWhiteSpace)?.textContains('\n') == true || kotlinCommonSettings
                        .METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE
                ) {
                    createSpacing(spaces, keepLineBreaks = false)
                } else {
                    createSpacing(spaces)
                }
            }

            inPosition(
                parent = CLASS_BODY,
                left = LBRACE,
                right = ENUM_ENTRY
            ).lineBreakIfLineBreakInParent(numSpacesOtherwise = 1)
        }

        simple {
            afterInside(LBRACE, BLOCK).lineBreakInCode()
            beforeInside(RBRACE, BLOCK).spacing(
                1, 0, 1,
                kotlinCommonSettings.KEEP_LINE_BREAKS,
                kotlinCommonSettings.KEEP_BLANK_LINES_BEFORE_RBRACE
            )
            between(LBRACE, ENUM_ENTRY).spacing(1, 0, 0, true, kotlinCommonSettings.KEEP_BLANK_LINES_IN_CODE)
            beforeInside(RBRACE, WHEN).lineBreakInCode()
            between(RPAR, BODY).spaces(1)

            // if when entry has block, spacing after arrow should be set by lbrace rule
            aroundInside(ARROW, WHEN_ENTRY).spaceIf(kotlinCustomSettings.SPACE_AROUND_WHEN_ARROW)

            beforeInside(COLON, EXTEND_COLON_ELEMENTS) { spaceIf(kotlinCustomSettings.SPACE_BEFORE_EXTEND_COLON) }

            after(EOL_COMMENT).lineBreakInCode()
        }
    }
}

private fun excludeLambdasAndObjects(parent: ASTBlock): List<TextRange> {
    val rangesToExclude = mutableListOf<TextRange>()
    parent.requireNode().psi.accept(object : KtTreeVisitorVoid() {
        override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
            super.visitLambdaExpression(lambdaExpression)
            rangesToExclude.add(lambdaExpression.textRange)
        }

        override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression) {
            super.visitObjectLiteralExpression(expression)
            rangesToExclude.add(expression.textRange)
        }

        override fun visitNamedFunction(function: KtNamedFunction) {
            super.visitNamedFunction(function)
            if (function.name == null) {
                rangesToExclude.add(function.textRange)
            }
        }
    })
    return TextRangeUtil.excludeRanges(parent.textRange, rangesToExclude).toList()
}
