/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("UnstableApiUsage")

package org.jetbrains.kotlin.kmp.infra

import fleet.com.intellij.platform.syntax.SyntaxElementType
import fleet.com.intellij.platform.syntax.element.SyntaxTokenTypes
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilder
import fleet.com.intellij.platform.syntax.parser.SyntaxTreeBuilderFactory
import fleet.com.intellij.platform.syntax.parser.prepareProduction
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens
import org.jetbrains.kotlin.kmp.parser.KDocParser
import java.util.ArrayDeque

sealed class NewParserTestNode

class NewParserTestToken(val token: SyntaxElementType) : NewParserTestNode()

class NewParserTestParseNode(val production: SyntaxTreeBuilder.Production) : NewParserTestNode()

class NewParser : AbstractParser<NewParserTestNode>() {
    override fun parseKDocOnlyNodes(fileName: String, text: String): List<TestParseNode<NewParserTestNode>> {
        val kotlinLexer = KotlinLexer()
        kotlinLexer.start(text)

        return buildList {
            var currentKotlinTokenType = kotlinLexer.getTokenType()
            while (currentKotlinTokenType != null) {
                if (currentKotlinTokenType == KtTokens.DOC_COMMENT) {
                    val start = kotlinLexer.getTokenStart()

                    val kDocParser = KDocParser()

                    val kDocBuilder = SyntaxTreeBuilderFactory.builder(
                        kotlinLexer.getTokenText(),
                        whitespaces = setOf(SyntaxTokenTypes.WHITE_SPACE), comments = emptySet(),
                        KDocLexer()
                    ).withStartOffset(start).build()

                    kDocParser.parse(kDocBuilder)

                    add(convertToTestParseNode(kDocBuilder, start))
                }

                kotlinLexer.advance()
                currentKotlinTokenType = kotlinLexer.getTokenType()
            }
        }
    }

    private fun convertToTestParseNode(builder: SyntaxTreeBuilder, start: Int): TestParseNode<NewParserTestNode> {
        val productions = prepareProduction(builder).productionMarkers
        val tokens = builder.tokens

        val childrenStack = ArrayDeque<MutableList<TestParseNode<NewParserTestNode>>>().apply {
            add(mutableListOf())
        }
        var prevTokenIndex = 0

        fun MutableList<TestParseNode<NewParserTestNode>>.appendLeafNodes(lastTokenIndex: Int) {
            for (leafTokenIndex in prevTokenIndex until lastTokenIndex) {
                val tokenType = tokens.getTokenType(leafTokenIndex)!!

                // Here is the extension point that can be used for subparsing (see the next commit for instance) or probably handling lazy elements

                add(
                    TestParseNode(
                        tokenType.toString(),
                        tokens.getTokenStart(leafTokenIndex) + start,
                        tokens.getTokenEnd(leafTokenIndex) + start,
                        NewParserTestToken(tokenType),
                        emptyList()
                    )
                )
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

                    val testParseNode = TestParseNode(
                        production.getNodeType().toString(),
                        production.getStartOffset(),
                        production.getEndOffset(),
                        NewParserTestParseNode(production),
                        children,
                    )

                    childrenStack.peek().add(testParseNode)
                }

                isErrorMarker -> {
                    // TODO: Implement errors handling
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

    override fun parse(fileName: String, text: String): TestParseNode<NewParserTestNode> {
        TODO("Implement new parser (KT-77144)")
    }
}