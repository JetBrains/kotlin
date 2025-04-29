/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import fleet.com.intellij.platform.syntax.SyntaxElementType
import org.jetbrains.kotlin.kmp.lexer.KDocLexer
import org.jetbrains.kotlin.kmp.lexer.KotlinLexer
import org.jetbrains.kotlin.kmp.lexer.KtTokens

class NewLexer : AbstractLexer<SyntaxElementType>() {
    override fun tokenize(text: String): List<Token<SyntaxElementType>> {
        val kotlinLexer = KotlinLexer()
        kotlinLexer.start(text)

        return buildList {
            var currentKotlinTokenType = kotlinLexer.getTokenType()
            while (currentKotlinTokenType != null) {
                val mainTokenStart = kotlinLexer.getTokenStart()
                val mainTokenEnd = kotlinLexer.getTokenEnd()

                val token = if (currentKotlinTokenType == KtTokens.DOC_COMMENT) {
                    val kDocLexer = KDocLexer()
                    kDocLexer.start(text.subSequence(mainTokenStart, mainTokenEnd))

                    val kDocTokens = buildList {
                        var currentKDocTokenType = kDocLexer.getTokenType()
                        while (currentKDocTokenType != null) {
                            add(
                                SingleToken(
                                    currentKDocTokenType.toString(),
                                    mainTokenStart + kDocLexer.getTokenStart(),
                                    mainTokenStart + kDocLexer.getTokenEnd(),
                                    currentKDocTokenType
                                )
                            )
                            kDocLexer.advance()
                            currentKDocTokenType = kDocLexer.getTokenType()
                        }
                    }

                    MultiToken(currentKotlinTokenType.toString(), mainTokenStart, mainTokenEnd, currentKotlinTokenType, kDocTokens)
                } else {
                    SingleToken(currentKotlinTokenType.toString(), mainTokenStart, mainTokenEnd, currentKotlinTokenType)
                }
                add(token)

                kotlinLexer.advance()
                currentKotlinTokenType = kotlinLexer.getTokenType()
            }
        }
    }
}