/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.platform.syntax.SyntaxElementType
import com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import com.intellij.platform.syntax.parser.prepareProduction
import com.intellij.platform.syntax.util.lexer.LexerBase
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KDocTokens
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.AbstractParser
import org.jetbrains.kotlin.kmp.parser.KDocLinkParser
import org.jetbrains.kotlin.kmp.parser.KDocParser
import org.jetbrains.kotlin.kmp.parser.KotlinParser
import org.jetbrains.kotlin.kmp.utils.Stack

sealed class NewParserTestNode

class NewParserTestToken(val token: SyntaxElementType) : NewParserTestNode()

class NewParserTestParseNode(val production: SyntaxTreeBuilder.Production) : NewParserTestNode()

class NewTestParser(parseMode: ParseMode) : AbstractTestParser<NewParserTestNode>(parseMode) {
    override fun parse(fileName: String, text: String): TestParseNode<out NewParserTestNode> {
        return if (parseMode == ParseMode.KDocOnly) {
            parseKDocOnlyNodes(text).wrapRootsIfNeeded(text.length)
        } else {
            val isLazy = parseMode == ParseMode.NoCollapsableAndKDoc
            val parser = KotlinParser(isScript(fileName), isLazy)
            parseToTestParseElement(text, 0, KotlinLexer(), parser)
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
                        parseToTestParseElement(
                            kotlinLexer.getTokenText(),
                            kotlinLexer.getTokenStart(),
                            KDocLexer(),
                            KDocParser,
                        )
                    )
                }

                kotlinLexer.advance()
                kotlinTokenType = kotlinLexer.getTokenType()
            }
        }
    }

    private fun convertToTestParseElement(builder: SyntaxTreeBuilder, start: Int): TestParseNode<out NewParserTestNode> {
        val productions = prepareProduction(builder).productionMarkers
        val tokens = builder.tokens

        val childrenStack = Stack<MutableList<TestParseNode<out NewParserTestNode>>>().apply {
            push(mutableListOf())
        }
        var prevTokenIndex = 0
        var lastErrorTokenIndex = -1

        fun MutableList<TestParseNode<out NewParserTestNode>>.appendLeafElements(lastTokenIndex: Int) {
            for (leafTokenIndex in prevTokenIndex until lastTokenIndex) {
                val tokenType = tokens.getTokenType(leafTokenIndex)!!
                val tokenStart = tokens.getTokenStart(leafTokenIndex) + start
                val tokenEnd = tokens.getTokenEnd(leafTokenIndex) + start

                if (tokenStart == tokenEnd) {
                    // LightTree and PSI builders ignores empty leaf tokens by default (for instance, `DANGLING_NEWLINE`)
                    continue
                }

                val node = when (tokenType) {
                    // `MARKDOWN_LINK` only can be encountered inside KDoc
                    KDocTokens.MARKDOWN_LINK if (parseMode.isParseKDoc) -> {
                        parseToTestParseElement(
                            tokens.getTokenText(leafTokenIndex)!!,
                            tokenStart,
                            KotlinLexer(),
                            KDocLinkParser,
                        )
                    }
                    KtTokens.DOC_COMMENT if (parseMode.isParseKDoc) -> {
                        parseToTestParseElement(
                            tokens.getTokenText(leafTokenIndex)!!,
                            tokenStart,
                            KDocLexer(),
                            KDocParser,
                        )
                    }
                    else -> {
                        TestParseNode(
                            tokenType.toString(),
                            tokenStart,
                            tokenEnd,
                            NewParserTestToken(tokenType),
                            emptyList()
                        )
                    }
                }

                add(node)
            }
            prevTokenIndex = lastTokenIndex
        }

        for (productionIndex in 0 until productions.size) {
            val production = productions.getMarker(productionIndex)

            when {
                productions.isDoneMarker(productionIndex) -> {
                    val lastChildren = childrenStack.pop()
                    val children = if (production.isCollapsed()) {
                        // Ignore collapsed elements
                        prevTokenIndex = production.getEndTokenIndex()
                        emptyList()
                    } else {
                        lastChildren.also { it.appendLeafElements(production.getEndTokenIndex()) }
                    }

                    // Here is the extension point to implement custom logic on finishing an element (for instance, creating a FIR node).
                    // We have a parent element type, its children, and a previously initialized state from start marker.
                    // Also, if such a bottom-up conversion is complicated, here we can initialize just an ordinary "super-light" tree
                    // and convert it later in a builder like existing PSI/LightTree builders.
                    // In addition, here we can skip whitespace or comment tokens that could improve performance a bit.

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

                production.isErrorMarker() -> {
                    val errorTokenIndex = production.getStartTokenIndex()
                    if (errorTokenIndex == lastErrorTokenIndex) {
                        // Prevent inserting of duplicated error elements (obey `PsiBuilderImpl.prepareLightTree` implementation)
                        continue
                    } else {
                        lastErrorTokenIndex = errorTokenIndex
                    }
                    childrenStack.peek().let {
                        it.appendLeafElements(errorTokenIndex)
                        it.add(
                            TestParseNode(
                                production.getNodeType().toString(),
                                production.getStartOffset(),
                                production.getEndOffset(),
                                NewParserTestParseNode(production),
                                emptyList(), // No children `isErrorMarker` is true only on leaf elements
                            )
                        )
                    }
                }

                else -> {
                    // Start marker

                    // Here is the extension point to implement custom logic on starting visiting an element.
                    // For instance, initialize some state during converting to FIR.
                    // Element type is known, it's `production.getNodeType()`

                    childrenStack.peek().appendLeafElements(production.getStartTokenIndex())
                    childrenStack.push(mutableListOf())
                }
            }
        }

        return childrenStack.pop().single()
    }

    private fun parseToTestParseElement(
        charSequence: CharSequence,
        start: Int,
        lexer: LexerBase,
        parser: AbstractParser,
    ): TestParseNode<out NewParserTestNode> {
        val syntaxTreeBuilder = SyntaxTreeBuilderFactory.builder(
            charSequence,
            whitespaces = parser.whitespaces,
            comments = parser.comments,
            lexer
        ).withStartOffset(start)
            .withWhitespaceOrCommentBindingPolicy(parser.whitespaceOrCommentBindingPolicy)
            .build()

        parser.parse(syntaxTreeBuilder)

        return convertToTestParseElement(syntaxTreeBuilder, start)
    }
}