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
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.formatter.lineIndent.KotlinLikeLangLineIndentProvider.KotlinElement.*
import org.jetbrains.kotlin.lexer.KtTokens

abstract class KotlinLikeLangLineIndentProvider : JavaLikeLangLineIndentProvider() {
    abstract fun indentionSettings(project: Project): KotlinIndentationAdjuster

    override fun mapType(tokenType: IElementType): SemanticEditorPosition.SyntaxElement? = SYNTAX_MAP[tokenType]

    override fun isSuitableForLanguage(language: Language): Boolean = language.isKindOf(KotlinLanguage.INSTANCE)

    private fun debugInfo(currentPosition: SemanticEditorPosition): String {
        val after = currentPosition.after()
        val before = currentPosition.before()
        val chars = currentPosition.chars
        fun print(position: SemanticEditorPosition, next: SemanticEditorPosition? = null) = "${position.currElement} =>\n'${
            if (position.isAtEnd)
                "end"
            else
                chars.subSequence(position.startOffset, next?.takeIf { !it.isAtEnd }?.startOffset ?: chars.length)
        }'"

        return "==\nbefore ${
            print(before, currentPosition)
        }\ncurr ${
            print(currentPosition, after)
        }\nafter ${
            print(after)
        }\n=="
    }

    override fun getLineIndent(project: Project, editor: Editor, language: Language?, offset: Int): String? {
        // HACK: TODO: KT-34566 investigate this hack (necessary for [org.jetbrains.kotlin.idea.editor.Kotlin.MultilineStringEnterHandler])
        return if (offset > 0 && getPosition(editor, offset - 1).isAt(RegularStringPart))
            LineIndentProvider.DO_NOT_ADJUST
        else
            super.getLineIndent(project, editor, language, offset)
    }

    override fun getIndent(project: Project, editor: Editor, language: Language?, offset: Int): IndentCalculator? {
        val factory = IndentCalculatorFactory(project, editor)
        val currentPosition = getPosition(editor, offset)
        if (!currentPosition.matchesRule { it.isAt(Whitespace) && it.isAtMultiline }) return null

        // ~~~ TESTING ~~~
//        println(debugInfo(currentPosition))
        // ~~~ TESTING ~~~

        val before = currentPosition.beforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)
        val after = currentPosition.afterOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)
        when {
            before.isAt(TemplateEntryOpen) -> {
                val baseLineOffset = before.startOffset
                val indent = if (!currentPosition.hasEmptyLineAfter(offset) && after.isAt(TemplateEntryClose))
                    Indent.getNoneIndent()
                else
                    Indent.getNormalIndent()

                return factory.createIndentCalculator(indent) { baseLineOffset }
            }

            before.isAtAnyOf(TryKeyword, FinallyKeyword) -> return factory.createIndentCalculator(
                Indent.getNoneIndent(),
                IndentCalculator.LINE_BEFORE,
            )
        }

        before.controlFlowStatementBefore()?.let { controlFlowKeywordPosition ->
            return factory.createIndentCalculator(
                when {
                    after.isAt(LeftParenthesis) -> Indent.getContinuationIndent()
                    after.isAtAnyOf(BlockOpeningBrace, Arrow) || controlFlowKeywordPosition.isWhileInsideDoWhile() -> Indent.getNoneIndent()
                    else -> Indent.getNormalIndent()
                },
                IndentCalculator.LINE_BEFORE,
            )
        }

        after.takeIf { it.isAt(TemplateEntryClose) }?.let { templateEntryPosition ->
            val indent = if (currentPosition.hasEmptyLineAfter(offset)) Indent.getNormalIndent() else Indent.getNoneIndent()
            templateEntryPosition.moveBeforeParentheses(TemplateEntryOpen, TemplateEntryClose)
            val baseLineOffset = templateEntryPosition.startOffset
            return factory.createIndentCalculator(indent) { baseLineOffset }
        }

        return null
    }

    private fun SemanticEditorPosition.moveBeforeIfThisIsWhiteSpaceOrComment() = moveBeforeOptionalMix(*WHITE_SPACE_OR_COMMENT_BIT_SET)

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
        if (isAt(BlockOpeningBrace)) {
            moveBefore()
            moveBeforeParentheses(LeftParenthesis, RightParenthesis)
            moveBeforeIfThisIsWhiteSpaceOrComment()
        }

        if (currElement in CONTROL_FLOW_CONSTRUCTIONS) return this
        if (!isAt(RightParenthesis)) return null

        moveBeforeParentheses(LeftParenthesis, RightParenthesis)
        moveBeforeIfThisIsWhiteSpaceOrComment()

        return takeIf { currElement in CONTROL_FLOW_CONSTRUCTIONS }
    }

    enum class KotlinElement : SemanticEditorPosition.SyntaxElement {
        TemplateEntryOpen,
        TemplateEntryClose,
        Arrow,
        WhenKeyword,
        CatchKeyword,
        FinallyKeyword,
        WhileKeyword,
        RegularStringPart,
        KDoc,
    }

    companion object {
        private val SYNTAX_MAP: Map<IElementType, SemanticEditorPosition.SyntaxElement> = hashMapOf(
            KtTokens.WHITE_SPACE to Whitespace,
            KtTokens.LONG_TEMPLATE_ENTRY_START to TemplateEntryOpen,
            KtTokens.LONG_TEMPLATE_ENTRY_END to TemplateEntryClose,
            KtTokens.EOL_COMMENT to LineComment,
            KtTokens.BLOCK_COMMENT to BlockComment,
            KtTokens.DOC_COMMENT to KDoc,
            KtTokens.ARROW to Arrow,
            KtTokens.LBRACE to BlockOpeningBrace,
            KtTokens.RBRACE to BlockClosingBrace,
            KtTokens.LPAR to LeftParenthesis,
            KtTokens.RPAR to RightParenthesis,
            KtTokens.IF_KEYWORD to IfKeyword,
            KtTokens.ELSE_KEYWORD to ElseKeyword,
            KtTokens.WHEN_KEYWORD to WhenKeyword,
            KtTokens.TRY_KEYWORD to TryKeyword,
            KtTokens.CATCH_KEYWORD to CatchKeyword,
            KtTokens.FINALLY_KEYWORD to FinallyKeyword,
            KtTokens.WHILE_KEYWORD to WhileKeyword,
            KtTokens.DO_KEYWORD to DoKeyword,
            KtTokens.FOR_KEYWORD to ForKeyword,
            KtTokens.REGULAR_STRING_PART to RegularStringPart,
            KtTokens.LBRACKET to ArrayOpeningBracket,
            KtTokens.RBRACKET to ArrayClosingBracket,
        )

        private val CONTROL_FLOW_CONSTRUCTIONS: HashSet<SemanticEditorPosition.SyntaxElement> = hashSetOf(
            WhenKeyword,
            IfKeyword,
            ElseKeyword,
            DoKeyword,
            WhileKeyword,
            ForKeyword,
            TryKeyword,
            CatchKeyword,
            FinallyKeyword,
        )

        private val WHITE_SPACE_OR_COMMENT_BIT_SET: Array<SemanticEditorPosition.SyntaxElement> = arrayOf(
            Whitespace,
            LineComment,
            BlockComment,
        )
    }
}