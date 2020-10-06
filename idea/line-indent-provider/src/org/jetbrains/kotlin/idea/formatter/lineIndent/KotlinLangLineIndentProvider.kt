/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.lineIndent

import com.intellij.formatting.Indent
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.lineIndent.LineIndentProvider
import com.intellij.psi.impl.source.codeStyle.SemanticEditorPosition
import com.intellij.psi.impl.source.codeStyle.lineIndent.IndentCalculator
import com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider
import com.intellij.psi.impl.source.codeStyle.lineIndent.JavaLikeLangLineIndentProvider.JavaLikeElement.*
import com.intellij.psi.tree.IElementType
import com.intellij.util.text.CharArrayUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.formatter.lineIndent.KotlinLangLineIndentProvider.KotlinElement.*
import org.jetbrains.kotlin.lexer.KtTokens

abstract class KotlinLangLineIndentProvider : JavaLikeLangLineIndentProvider() {
    abstract fun indentionSettings(project: Project): KotlinIndentationAdjuster

    override fun mapType(tokenType: IElementType): SemanticEditorPosition.SyntaxElement? = SYNTAX_MAP[tokenType]

    override fun isSuitableForLanguage(language: Language): Boolean = language.isKindOf(KotlinLanguage.INSTANCE)

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        // HACK: TODO: KT-34566 investigate this hack (necessary for [org.jetbrains.kotlin.idea.editor.Kotlin.MultilineStringEnterHandler])
        return if (offset > 0 && getPosition(editor, offset - 1).isAt(RegularStringPart))
            LineIndentProvider.DO_NOT_ADJUST
        else
            super.getLineIndent(project, editor, language, offset)
    }

    override fun getIndent(project: Project, editor: Editor, language: Language?, offset: Int): IndentCalculator? {
        val factory = IndentCalculatorFactory(project, editor)
        val settings = indentionSettings(project)
        val currentPosition = getPosition(editor, offset)
        if (!currentPosition.matchesRule { it.isAt(Whitespace) && it.isAtMultiline }) return null

        val before = currentPosition.beforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)
        val after = currentPosition.afterOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

        when {
            after.isAt(BlockClosingBrace) && !currentPosition.hasLineBreaksAfter(offset) ->
                return factory.createIndentCalculatorForBrace(before, after, BlockOpeningBrace, BlockClosingBrace, Indent.getNoneIndent())

            before.isAt(BlockOpeningBrace) && after.isAt(BlockClosingBrace) -> {
                return factory.createIndentCalculatorForBrace(before, after, BlockOpeningBrace, BlockClosingBrace, Indent.getNormalIndent())
            }

            after.isAt(ArrayClosingBracket) && !currentPosition.hasLineBreaksAfter(offset) ->
                return factory.createIndentCalculatorForBrace(
                    before,
                    after,
                    ArrayOpeningBracket,
                    ArrayClosingBracket,
                    Indent.getNoneIndent()
                )

            before.isAt(ArrayOpeningBracket) && after.isAt(ArrayClosingBracket) -> {
                val indent = if (isSimilarToFunctionInvocation(before))
                    Indent.getContinuationIndent()
                else
                    Indent.getNormalIndent()

                return factory.createIndentCalculator(indent, before.startOffset)
            }

            //            KT-39716
            //            after.isAt(Quest) && after.after().isAt(Colon) -> {
            //                val indent = if (settings.continuationIndentInElvis)
            //                    Indent.getContinuationIndent()
            //                else
            //                    Indent.getNormalIndent()
            //
            //                return factory.createIndentCalculator(indent, before.startOffset)
            //            }

            before.isAt(Colon) && before.before().isAt(Quest) ->
                return factory.createIndentCalculator(Indent.getNoneIndent(), before.startOffset)

            before.isAt(TemplateEntryOpen) -> {
                val indent = if (!currentPosition.hasLineBreaksAfter(offset) && after.isAt(TemplateEntryClose))
                    Indent.getNoneIndent()
                else
                    Indent.getNormalIndent()

                return factory.createIndentCalculator(indent, before.startOffset)
            }

            before.isAtAnyOf(TryKeyword) || before.isFinallyKeyword() ->
                return factory.createIndentCalculator(Indent.getNoneIndent(), IndentCalculator.LINE_BEFORE)

            after.isAt(TemplateEntryClose) -> {
                val indent = if (currentPosition.hasEmptyLineAfter(offset)) Indent.getNormalIndent() else Indent.getNoneIndent()
                after.moveBeforeParentheses(TemplateEntryOpen, TemplateEntryClose)
                return factory.createIndentCalculator(indent, after.startOffset)
            }

            before.isAt(Eq) -> {
                val declaration = findFunctionOrPropertyOrMultiDeclarationBefore(before.beforeIgnoringWhiteSpaceOrComment())
                if (declaration != null) {
                    val indent = if (settings.continuationIndentForExpressionBodies)
                        Indent.getContinuationIndent()
                    else
                        Indent.getNormalIndent()

                    return factory.createIndentCalculator(indent, declaration.startOffset)
                }
            }

            before.isAt(LeftParenthesis) && after.isAt(RightParenthesis) ->
                factory.createIndentCalculatorForParenthesis(before, currentPosition, after, offset, settings)?.let { return it }
        }

        findFunctionOrPropertyOrMultiDeclarationBefore(before)?.let {
            return factory.createIndentCalculator(Indent.getNoneIndent(), it.startOffset)
        }

        return before.controlFlowStatementBefore()?.let { controlFlowKeywordPosition ->
            val indent = when {
                controlFlowKeywordPosition.similarToCatchKeyword() -> if (before.isAt(RightParenthesis)) Indent.getNoneIndent() else Indent.getNormalIndent()
                after.isAt(LeftParenthesis) -> Indent.getContinuationIndent()
                after.isAtAnyOf(BlockOpeningBrace, Arrow) || controlFlowKeywordPosition.isWhileInsideDoWhile() -> Indent.getNoneIndent()
                else -> Indent.getNormalIndent()
            }

            factory.createIndentCalculator(indent, IndentCalculator.LINE_BEFORE)
        }
    }

    private enum class KotlinElement : SemanticEditorPosition.SyntaxElement {
        TemplateEntryOpen,
        TemplateEntryClose,
        Arrow,
        WhenKeyword,
        WhileKeyword,
        RegularStringPart,
        KDoc,
        Identifier,
        OpenTypeBrace,
        CloseTypeBrace,
        FunctionKeyword,
        Dot,
        Quest,

        Eq,

        Val, Var,
    }

    companion object {
        private val SYNTAX_MAP: Map<IElementType, SemanticEditorPosition.SyntaxElement> = hashMapOf(
            KtTokens.WHITE_SPACE to Whitespace,
            KtTokens.EOL_COMMENT to LineComment,
            KtTokens.BLOCK_COMMENT to BlockComment,
            KtTokens.DOC_COMMENT to KDoc,
            KtTokens.ARROW to Arrow,

            KtTokens.LONG_TEMPLATE_ENTRY_START to TemplateEntryOpen,
            KtTokens.LONG_TEMPLATE_ENTRY_END to TemplateEntryClose,

            KtTokens.LBRACE to BlockOpeningBrace,
            KtTokens.RBRACE to BlockClosingBrace,

            KtTokens.LPAR to LeftParenthesis,
            KtTokens.RPAR to RightParenthesis,

            KtTokens.LBRACKET to ArrayOpeningBracket,
            KtTokens.RBRACKET to ArrayClosingBracket,

            KtTokens.LT to OpenTypeBrace,
            KtTokens.GT to CloseTypeBrace,

            KtTokens.IF_KEYWORD to IfKeyword,
            KtTokens.ELSE_KEYWORD to ElseKeyword,
            KtTokens.WHEN_KEYWORD to WhenKeyword,
            KtTokens.TRY_KEYWORD to TryKeyword,
            KtTokens.WHILE_KEYWORD to WhileKeyword,
            KtTokens.DO_KEYWORD to DoKeyword,
            KtTokens.FOR_KEYWORD to ForKeyword,

            KtTokens.REGULAR_STRING_PART to RegularStringPart,
            KtTokens.IDENTIFIER to Identifier,
            KtTokens.FUN_KEYWORD to FunctionKeyword,
            KtTokens.DOT to Dot,
            KtTokens.QUEST to Quest,
            KtTokens.COMMA to Comma,
            KtTokens.COLON to Colon,

            KtTokens.EQ to Eq,

            KtTokens.VAL_KEYWORD to Val,
            KtTokens.VAR_KEYWORD to Var,
        )

        private val CONTROL_FLOW_KEYWORDS: HashSet<SemanticEditorPosition.SyntaxElement> = hashSetOf(
            WhenKeyword,
            IfKeyword,
            ElseKeyword,
            DoKeyword,
            WhileKeyword,
            ForKeyword,
            TryKeyword,
        )

        private val WHITE_SPACE_OR_COMMENT_BIT_SET: Array<SemanticEditorPosition.SyntaxElement> = arrayOf(
            Whitespace,
            LineComment,
            BlockComment,
        )

        private fun IndentCalculatorFactory.createIndentCalculatorForBrace(
            before: SemanticEditorPosition,
            after: SemanticEditorPosition,
            leftBraceType: SemanticEditorPosition.SyntaxElement,
            rightBraceType: SemanticEditorPosition.SyntaxElement,
            defaultIndent: Indent
        ): IndentCalculator {
            val leftBrace = before.copyAnd {
                it.moveToLeftParenthesisBackwardsSkippingNested(leftBraceType, rightBraceType)
            }

            if (after.after().afterOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET).isAt(Comma)) {
                return createIndentCalculator(createAlignMultilineIndent(leftBrace), leftBrace.startOffset)
            }

            val beforeLeftBrace = leftBrace.copyAnd { it.moveBeforeIgnoringWhiteSpaceOrComment() }
            val leftAnchor = if (beforeLeftBrace.isAt(RightParenthesis)) {
                beforeLeftBrace.moveBeforeParentheses(LeftParenthesis, RightParenthesis)
                beforeLeftBrace
            } else {
                findFunctionDeclarationBeforeBody(beforeLeftBrace)
            }

            val resultPosition = leftAnchor?.takeIf { !it.isAtEnd } ?: leftBrace
            return createIndentCalculator(defaultIndent, resultPosition.startOffset)
        }

        private fun IndentCalculatorFactory.createIndentCalculatorForParenthesis(
            leftParenthesis: SemanticEditorPosition,
            currentPosition: SemanticEditorPosition,
            rightParenthesis: SemanticEditorPosition,
            offset: Int,
            settings: KotlinIndentationAdjuster,
        ): IndentCalculator? {
            assert(leftParenthesis.isAt(LeftParenthesis))
            assert(rightParenthesis.isAt(RightParenthesis))

            // case only for caret before [RightParenthesis]
            if (!currentPosition.hasLineBreaksAfter(offset)) {
                val indentForParentheses by lazy {
                    if (settings.alignWhenMultilineFunctionParentheses) createAlignMultilineIndent(leftParenthesis) else Indent.getNoneIndent()
                }

                findFunctionKeywordBeforeIdentifier(leftParenthesis.beforeIgnoringWhiteSpaceOrComment())?.let {
                    return createIndentCalculator(indentForParentheses, it.startOffset)
                }

                // NB: this covered [KtTokens.CONSTRUCTOR_KEYWORD], [KtTokens.SET_KEYWORD], [KtTokens.GET_KEYWORD], [KtTokens.INIT_KEYWORD] as well
                if (isSimilarToFunctionInvocation(leftParenthesis)) {
                    return createIndentCalculator(indentForParentheses, leftParenthesis.startOffset)
                }

                if (isDestructuringDeclaration(leftParenthesis, rightParenthesis)) {
                    return createIndentCalculator(Indent.getNoneIndent(), leftParenthesis.startOffset)
                }

                leftParenthesis.beforeIgnoringWhiteSpaceOrComment().let { keyword ->
                    if (keyword.isControlFlowKeyword()) {
                        return createIndentCalculator(Indent.getNoneIndent(), keyword.startOffset)
                    }
                }

                val indentForBinaryExpression = if (settings.alignWhenMultilineBinaryExpression)
                    createAlignMultilineIndent(leftParenthesis)
                else
                    Indent.getContinuationIndent()

                return createIndentCalculator(indentForBinaryExpression, leftParenthesis.startOffset)
            }

            return null
        }

        /**
         * @param endOfDeclaration is position before '=' for expression body or '{' for block body
         */
        private fun findFunctionOrPropertyOrMultiDeclarationBefore(endOfDeclaration: SemanticEditorPosition): SemanticEditorPosition? =
            findFunctionDeclarationBeforeBody(endOfDeclaration)
                ?: findPropertyDeclarationBeforeAssignment(endOfDeclaration)
                ?: findMultiDeclarationBeforeAssignment(endOfDeclaration)

        /**
         * `val (a, b) = 1 to 2`
         *           ^
         */
        private fun findMultiDeclarationBeforeAssignment(rightParenthesis: SemanticEditorPosition): SemanticEditorPosition? {
            if (!rightParenthesis.isAt(RightParenthesis)) return null
            return with(rightParenthesis.copy()) {
                if (!moveBeforeParenthesesIfPossible()) return null
                takeIf { isVarOrVal() }
            }
        }

        /**
         * `val a = 5`
         * `val a: Int = 5`
         * `val List<Int>.a: Int = 5`
         * `val <T> List<Int>.a: Int = 5`
         */
        private fun findPropertyDeclarationBeforeAssignment(endOfDeclaration: SemanticEditorPosition): SemanticEditorPosition? {
            // `val a = 5`
            // this is a false positive for a declaration with explicit return type
            if (endOfDeclaration.isAt(Identifier)) {
                findPropertyKeywordBeforeIdentifier(endOfDeclaration)?.let { return it }
            }

            return with(endOfDeclaration.copy()) {
                // explicit type `fun a(): String` or `val a: String`
                if (moveBeforeTypeQualifierIfPossible(true)) {
                    if (!isAt(Colon)) return null
                    moveBeforeIgnoringWhiteSpaceOrComment()
                }

                findPropertyKeywordBeforeIdentifier(this)
            }
        }

        /**
         * `val a = fun() { }`
         * `val a = fun String.Companion????.() { }`
         * `fun a() = 4`
         * `fun a(): Int = 4`
         * `fun Int.a(): Int = 4`
         * `fun <T> T.a(): Int = 4`
         */
        private fun findFunctionDeclarationBeforeBody(endOfDeclaration: SemanticEditorPosition): SemanticEditorPosition? =
            with(endOfDeclaration.copy()) {
                // explicit type `fun a(): String`
                //                              ^
                if (moveBeforeTypeQualifierIfPossible(true)) {
                    if (!isAt(Colon)) return null
                    moveBeforeIgnoringWhiteSpaceOrComment()
                }

                if (!moveBeforeParenthesesIfPossible()) return null

                findFunctionKeywordBeforeIdentifier(this)
            }

        private fun findPropertyKeywordBeforeIdentifier(identifierPosition: SemanticEditorPosition): SemanticEditorPosition? {
            if (!identifierPosition.isAt(Identifier)) return null
            return with(identifierPosition.copy()) {
                if (!moveBeforeTypeQualifierIfPossible(false)) return null
                // `val <T> List<T>.prop`
                moveBeforeTypeParametersIfPossible()
                takeIf { it.isVarOrVal() }
            }
        }

        /**
         * @return position of `fun` keyword before the declaration or null
         * TODO: support [KtTokens.CONSTRUCTOR_KEYWORD], [KtTokens.INIT_KEYWORD]. Maybe [KtTokens.SET_KEYWORD], [KtTokens.GET_KEYWORD] (related to KT-39444)
         */
        private fun findFunctionKeywordBeforeIdentifier(identifierPosition: SemanticEditorPosition): SemanticEditorPosition? {
            // anonymous function `val a = fun() { }`
            //                               ^
            if (identifierPosition.isAt(FunctionKeyword)) return identifierPosition

            return with(identifierPosition.copy()) {
                moveBeforeWhileThisIsWhiteSpaceOrComment()

                // anonymous function with receiver `val a = fun String.Companion????.() { }`
                //                                                                   ^
                if (isAt(Dot)) {
                    moveBeforeIgnoringWhiteSpaceOrComment()
                    if (!moveBeforeTypeQualifierIfPossible(true)) return null
                    return if (isAt(FunctionKeyword)) this else null
                }

                // name of declaration
                if (!isAt(Identifier)) return null
                if (!moveBeforeTypeQualifierIfPossible(false)) return null

                moveBeforeTypeParametersIfPossible()

                takeIf { it.isAt(FunctionKeyword) }
            }
        }

        /**
         * @param leftParenthesis is `(` or `[`
         */
        private fun isSimilarToFunctionInvocation(leftParenthesis: SemanticEditorPosition): Boolean = with(leftParenthesis.copy()) {
            moveBefore()

            if (!moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment() || isAtEnd) return false

            // calls with types e.g. `test<Int>()`
            if (isAt(CloseTypeBrace)) {
                moveBeforeParentheses(OpenTypeBrace, CloseTypeBrace)
                return moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment() && isIdentifier()
            }

            return isIdentifier() || isAtAnyOf(RightParenthesis, ArrayClosingBracket)
        }

        private fun isDestructuringDeclaration(
            leftParenthesis: SemanticEditorPosition,
            rightParenthesis: SemanticEditorPosition
        ): Boolean = with(leftParenthesis.copy()) {
            moveBeforeIgnoringWhiteSpaceOrComment()

            // val (a, b) = 1 to 2
            if (isVarOrVal()) return true

            // in lambda like `val a = { i: Int -> println(i) }`
            if (!rightParenthesis.moveBeforeParametersIfPossible()) return false

            return isAt(BlockOpeningBrace)
        }

        /**
         * @receiver start position of any parameter
         */
        private fun SemanticEditorPosition.moveBeforeParametersIfPossible(): Boolean {
            while (!isAtEnd) {
                if (!moveBeforeParameterIfPossible()) return false
                if (!isAt(Comma)) return true
                moveBeforeIgnoringWhiteSpaceOrComment()
            }

            return false
        }

        /**
         * @receiver start position of any parameter
         */
        private fun SemanticEditorPosition.moveBeforeParameterIfPossible(): Boolean {
            // destructuring declaration in lambda parameters without type
            // { a, (b, c), d ->
            //           ^
            if (isAt(RightParenthesis)) return moveBeforeParenthesesIfPossible()

            if (!moveBeforeTypeQualifierIfPossible(true)) return false

            // optional colon
            // { a: Int ->
            //    ^
            // (a: Int)
            //   ^
            if (isAt(Colon)) {
                moveBeforeIgnoringWhiteSpaceOrComment()

                // destructuring declaration in lambda parameters with type
                // { a, b, (c, d): Pair<Long, Long>, e ->
                //              ^
                if (isAt(RightParenthesis)) return moveBeforeParenthesesIfPossible()

                // { a: Int ->
                //   ^
                // (a: Int)
                //  ^
                if (!isAt(Identifier)) return false
                moveBeforeIgnoringWhiteSpaceOrComment()
            }

            return true
        }

        /***
         * Constructions like `OuterClass/*   */.MiddleClass<String>?????`, `String.Companion.testName`
         *
         * @receiver position of identifier
         */
        private fun SemanticEditorPosition.moveBeforeTypeQualifierIfPossible(canStartWithTypeParameter: Boolean): Boolean {
            if (!canStartWithTypeParameter && !isAt(Identifier)) return false

            while (!isAtEnd) {
                moveBeforeOptionalMix(Quest, *WHITE_SPACE_OR_COMMENT_BIT_SET)
                moveBeforeTypeParametersIfPossible()
                if (!isAt(Identifier)) return false
                moveBeforeIgnoringWhiteSpaceOrComment()
                if (!isAt(Dot)) return true
                moveBeforeIgnoringWhiteSpaceOrComment()
            }

            return false
        }

        private fun createAlignMultilineIndent(position: SemanticEditorPosition): Indent {
            val beforeLineStart = CharArrayUtil.shiftBackwardUntil(position.chars, position.startOffset, "\n") + 1
            val beforeLineWithoutIndentStart = CharArrayUtil.shiftForward(position.chars, beforeLineStart, " \t")
            return Indent.getSpaceIndent(position.startOffset - beforeLineWithoutIndentStart)
        }

        private fun SemanticEditorPosition.isWhileInsideDoWhile(): Boolean {
            if (!isAt(WhileKeyword)) return false
            with(copy()) {
                moveBefore()
                var whileKeywordLevel = 1
                while (!isAtEnd) when {
                    isAt(BlockOpeningBrace) -> return false
                    isAt(DoKeyword) -> {
                        if (--whileKeywordLevel == 0) return true
                        moveBefore()
                    }
                    isAt(WhileKeyword) -> {
                        ++whileKeywordLevel
                        moveBefore()
                    }
                    isAt(BlockClosingBrace) -> moveBeforeParentheses(BlockOpeningBrace, BlockClosingBrace)
                    else -> moveBefore()
                }
            }

            return false
        }

        private fun SemanticEditorPosition.controlFlowStatementBefore(): SemanticEditorPosition? = with(copy()) {
            if (isAt(BlockOpeningBrace)) moveBeforeIgnoringWhiteSpaceOrComment()

            if (isControlFlowKeyword()) return this
            if (!moveBeforeParenthesesIfPossible()) return null

            return takeIf { isControlFlowKeyword() }
        }

        private fun SemanticEditorPosition.isControlFlowKeyword(): Boolean =
            currElement in CONTROL_FLOW_KEYWORDS || isCatchKeyword() || isFinallyKeyword()

        private fun SemanticEditorPosition.similarToCatchKeyword(): Boolean = textOfCurrentPosition() == KtTokens.CATCH_KEYWORD.value

        private fun SemanticEditorPosition.isCatchKeyword(): Boolean = with(copy()) {
            // try-catch-*-catch
            do {
                if (!isAt(Identifier)) return false
                if (!similarToCatchKeyword()) return false

                moveBeforeIgnoringWhiteSpaceOrComment()
                if (!moveBeforeBlockIfPossible()) return false

                if (isAt(TryKeyword)) return true

                if (!moveBeforeParenthesesIfPossible()) return false
            } while (!isAtEnd)

            return false
        }

        private fun SemanticEditorPosition.isFinallyKeyword(): Boolean {
            if (!isAt(Identifier)) return false
            if (textOfCurrentPosition() != KtTokens.FINALLY_KEYWORD.value) return false
            with(copy()) {
                moveBeforeIgnoringWhiteSpaceOrComment()
                if (!moveBeforeBlockIfPossible()) return false

                // try-finally
                if (isAt(TryKeyword)) return true

                if (!moveBeforeParenthesesIfPossible()) return false

                // try-catch-finally
                return isCatchKeyword()
            }
        }

        private fun SemanticEditorPosition.moveBeforeWhileThisIsWhiteSpaceOrComment() =
            moveBeforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

        private fun SemanticEditorPosition.moveBeforeIgnoringWhiteSpaceOrComment() {
            moveBefore()
            moveBeforeWhileThisIsWhiteSpaceOrComment()
        }

        private fun SemanticEditorPosition.beforeIgnoringWhiteSpaceOrComment() = copyAnd { it.moveBeforeIgnoringWhiteSpaceOrComment() }
        private fun SemanticEditorPosition.moveBeforeWhileThisIsWhiteSpaceOnSameLineOrBlockComment(): Boolean {
            while (!isAtEnd) {
                if (isAt(Whitespace) && isAtMultiline) return false
                if (!isAt(BlockComment)) break
                moveBefore()
            }

            return true
        }

        private fun SemanticEditorPosition.isIdentifier(): Boolean = isAt(Identifier) || isAt(KtTokens.THIS_KEYWORD)
        private fun SemanticEditorPosition.isVarOrVal(): Boolean = isAtAnyOf(Var, Val)
        private fun SemanticEditorPosition.moveBeforeBlockIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
            leftParenthesis = BlockOpeningBrace,
            rightParenthesis = BlockClosingBrace,
        )

        private fun SemanticEditorPosition.moveBeforeTypeParametersIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
            leftParenthesis = OpenTypeBrace,
            rightParenthesis = CloseTypeBrace,
        )

        private fun SemanticEditorPosition.moveBeforeParenthesesIfPossible(): Boolean = moveBeforeParenthesesIfPossible(
            leftParenthesis = LeftParenthesis,
            rightParenthesis = RightParenthesis,
        )

        private fun SemanticEditorPosition.moveBeforeParenthesesIfPossible(
            leftParenthesis: SemanticEditorPosition.SyntaxElement,
            rightParenthesis: SemanticEditorPosition.SyntaxElement,
        ): Boolean {
            if (!isAt(rightParenthesis)) return false

            moveBeforeParentheses(leftParenthesis, rightParenthesis)
            moveBeforeWhileThisIsWhiteSpaceOrComment()
            return true
        }
    }
}

private fun JavaLikeLangLineIndentProvider.IndentCalculatorFactory.createIndentCalculator(
    indent: Indent,
    baseLineOffset: Int,
): IndentCalculator = createIndentCalculator(indent) { baseLineOffset } ?: error("Contract (null, _ -> null) is broken")

private fun SemanticEditorPosition.textOfCurrentPosition(): String =
    if (isAtEnd) "" else chars.subSequence(startOffset, after().startOffset).toString()