/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.formatter.lineIndent

import com.intellij.formatting.Indent
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
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

    override fun getIndent(project: Project, editor: Editor, language: Language?, offset: Int): IndentCalculator? {
        val factory = IndentCalculatorFactory(project, editor)
        val currentPosition = getPosition(editor, offset)

        // ~~~ TESTING ~~~
//        println(debugInfo(currentPosition))
        // ~~~ TESTING ~~~

        currentPosition.beforeOptionalMix(Whitespace, LineComment)
            .takeIf { it.isAt(TemplateEntryOpen) }
            ?.let { templateEntryPosition ->
                val baseLineOffset = templateEntryPosition.startOffset
                return factory.createIndentCalculator(Indent.getNormalIndent()) { baseLineOffset }
            }

        currentPosition.afterOptionalMix(Whitespace, LineComment)
            .takeIf { it.isAt(TemplateEntryClose) }
            ?.let { templateEntryPosition ->
                val baseLineOffset = templateEntryPosition.beforeParentheses(TemplateEntryOpen, TemplateEntryClose).startOffset
                val indent = if (currentPosition.hasEmptyLineAfter(offset)) Indent.getNormalIndent() else Indent.getNoneIndent()
                return factory.createIndentCalculator(indent) { baseLineOffset }
            }

        return null
    }

    enum class KotlinElement : SemanticEditorPosition.SyntaxElement {
        TemplateEntryOpen,
        TemplateEntryClose,
        Arrow,
        WhenKeyword,
    }

    companion object {
        private val SYNTAX_MAP: LinkedHashMap<IElementType, SemanticEditorPosition.SyntaxElement> = linkedMapOf(
            KtTokens.WHITE_SPACE to Whitespace,
            KtTokens.LONG_TEMPLATE_ENTRY_START to TemplateEntryOpen,
            KtTokens.LONG_TEMPLATE_ENTRY_END to TemplateEntryClose,
            KtTokens.EOL_COMMENT to LineComment,
            KtTokens.ARROW to Arrow,
            KtTokens.LBRACE to BlockOpeningBrace,
            KtTokens.RBRACE to BlockClosingBrace,
            KtTokens.ELSE_KEYWORD to ElseKeyword,
            KtTokens.WHEN_KEYWORD to WhenKeyword,
        )
    }
}