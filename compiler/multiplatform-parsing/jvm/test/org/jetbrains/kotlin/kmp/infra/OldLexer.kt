/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kmp.infra

import com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.kdoc.lexer.KDocLexer
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens

class OldLexer : AbstractLexer<IElementType>() {
    override fun tokenize(text: String): List<Token<IElementType>> {
        val kotlinLexer = KotlinLexer()
        kotlinLexer.start(text)

        return buildList {
            var currentKotlinTokenType = kotlinLexer.tokenType
            while (currentKotlinTokenType != null) {
                val mainTokenStart = kotlinLexer.tokenStart
                val mainTokenEnd = kotlinLexer.tokenEnd

                val token = if (currentKotlinTokenType == KtTokens.DOC_COMMENT) {
                    val kDocLexer = KDocLexer()
                    kDocLexer.start(text.subSequence(mainTokenStart, mainTokenEnd))

                    val kDocTokens = buildList {
                        var currentKDocTokenType = kDocLexer.tokenType
                        while (currentKDocTokenType != null) {
                            add(
                                SingleToken(
                                    currentKDocTokenType.toString(),
                                    mainTokenStart + kDocLexer.tokenStart,
                                    mainTokenStart + kDocLexer.tokenEnd,
                                    currentKDocTokenType
                                )
                            )
                            kDocLexer.advance()
                            currentKDocTokenType = kDocLexer.tokenType
                        }
                    }

                    MultiToken(currentKotlinTokenType.toString(), mainTokenStart, mainTokenEnd, currentKotlinTokenType, kDocTokens)
                } else {
                    SingleToken(currentKotlinTokenType.toString(), mainTokenStart, mainTokenEnd, currentKotlinTokenType)
                }
                add(token)

                kotlinLexer.advance()
                currentKotlinTokenType = kotlinLexer.tokenType
            }
        }
    }
}