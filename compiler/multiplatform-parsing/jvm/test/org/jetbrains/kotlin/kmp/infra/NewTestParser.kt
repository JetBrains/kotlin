/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.element.SyntaxTokenTypes
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import fleet.com.intellij.platform.syntax.parser.prepareProduction
import fleet.com.intellij.platform.syntax.util.lexer.LexerBase
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KDocTokens
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.AbstractParser
import org.jetbrains.kotlin.kmp.parser.KDocLinkParser
import org.jetbrains.kotlin.kmp.parser.KDocParser
import java.util.ArrayDeque

sealed class NewParserTestNode

class NewParserTestToken(val token: SyntaxElementType) : NewParserTestNode()

class NewParserTestParseNode(val production: SyntaxTreeBuilder.Production) : NewParserTestNode()

class NewTestParser : AbstractTestParser<NewParserTestNode>() {
    companion object {
        val kDocWhitespaces = setOf(SyntaxTokenTypes.WHITE_SPACE)
    }

    override fun parse(fileName: String, text: String, kDocOnly: Boolean): TestParseNode<out NewParserTestNode> {
        if (kDocOnly) {
            return parseKDocOnlyNodes(text).wrapRootsIfNeeded(text.length)
        } else {
            TODO("Implement new parser (KT-77144)")
        }
    }

    private fun parseKDocOnlyNodes(text: String): List<TestParseNode<out NewParserTestNode>> {
        val kotlinLexer = KotlinLexer()
        kotlinLexer.start(text)

        return buildList {
            var kotlinTokenType = kotlinLexer.getTokenType()
            while (kotlinTokenType != null) {
                if (kotlinTokenType == KtTokens.DOC_COMMENT) {
                    add(
                        parseToTestParseNode(
                            kotlinLexer.getTokenText(),
                            kotlinLexer.getTokenStart(),
                            KDocLexer(),
                            KDocParser,
                            kDocWhitespaces
                        )
                    )
                }

                kotlinLexer.advance()
                kotlinTokenType = kotlinLexer.getTokenType()
            }
        }
    }

    private fun convertToTestParseNode(builder: SyntaxTreeBuilder, start: Int): TestParseNode<out NewParserTestNode> {
        val productions = prepareProduction(builder).productionMarkers
        val tokens = builder.tokens

        val childrenStack = ArrayDeque<MutableList<TestParseNode<out NewParserTestNode>>>().apply {
            add(mutableListOf())
        }
        var prevTokenIndex = 0

        fun MutableList<TestParseNode<out NewParserTestNode>>.appendLeafNodes(lastTokenIndex: Int) {
            for (leafTokenIndex in prevTokenIndex until lastTokenIndex) {
                val tokenType = tokens.getTokenType(leafTokenIndex)!!
                val tokenStart = tokens.getTokenStart(leafTokenIndex) + start

                // Here is the extension point that can be used for sub-parsing or probably handling lazy elements
                val node = if (tokenType == KDocTokens.MARKDOWN_LINK) {
                    parseToTestParseNode(
                        tokens.getTokenText(leafTokenIndex)!!,
                        tokenStart,
                        KotlinLexer(),
                        KDocLinkParser,
                        emptySet(),
                    )
                } else {
                    TestParseNode(
                        tokenType.toString(),
                        tokenStart,
                        tokens.getTokenEnd(leafTokenIndex) + start,
                        NewParserTestToken(tokenType),
                        emptyList()
                    )
                }

                add(node)
            }
            prevTokenIndex = lastTokenIndex
        }

        for (productionIndex in 0 until productions.size) {
            val production = productions.getMarker(productionIndex)
            val isEndMarker = productions.isDoneMarker(productionIndex)
            val isErrorMarker = production.isErrorMarker()

            when {
                isEndMarker -> {
                    val children = childrenStack.pop().also {
                        it.appendLeafNodes(production.getEndTokenIndex())
                    }

                    childrenStack.peek().add(
                        TestParseNode(
                            production.getNodeType().toString(),
                            production.getStartOffset(),
                            production.getEndOffset(),
                            NewParserTestParseNode(production),
                            children,
                        )
                    )
                }

                isErrorMarker -> {
                    childrenStack.peek().let {
                        it.appendLeafNodes(production.getStartTokenIndex())
                        it.add(
                            TestParseNode(
                                production.getNodeType().toString(),
                                production.getStartOffset(),
                                production.getEndOffset(),
                                NewParserTestParseNode(production),
                                emptyList(),
                            )
                        )
                    }
                }

                else -> {
                    // start marker
                    childrenStack.peek().appendLeafNodes(production.getStartTokenIndex())
                    childrenStack.push(mutableListOf())
                }
            }
        }
        return childrenStack.single().single()
    }

    private fun parseToTestParseNode(
        charSequence: CharSequence,
        start: Int,
        lexer: LexerBase,
        parser: AbstractParser,
        whitespaces: Set<SyntaxElementType>,
    ): TestParseNode<out NewParserTestNode> {
        val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
            charSequence,
            whitespaces = whitespaces, comments = emptySet(),
            lexer
        ).withStartOffset(start).build()

        parser.parse(syntaxTreeBuilder)

        return convertToTestParseNode(syntaxTreeBuilder, start)
    }
}