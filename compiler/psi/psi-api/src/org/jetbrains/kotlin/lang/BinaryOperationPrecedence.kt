/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lang

import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import java.util.*

enum class BinaryOperationPrecedence(val higherPriority: BinaryOperationPrecedence?, vararg val tokens: KtToken) {
    AS(null, KtTokens.AS_KEYWORD, KtTokens.AS_SAFE),
    MULTIPLICATIVE(AS, KtTokens.MUL, KtTokens.DIV, KtTokens.PERC),
    ADDITIVE(MULTIPLICATIVE, KtTokens.PLUS, KtTokens.MINUS),
    RANGE(ADDITIVE, KtTokens.RANGE, KtTokens.RANGE_UNTIL),
    INFIX(RANGE, KtTokens.IDENTIFIER),
    ELVIS(INFIX, KtTokens.ELVIS),
    IN_OR_IS(ELVIS, KtTokens.IN_KEYWORD, KtTokens.NOT_IN, KtTokens.IS_KEYWORD, KtTokens.NOT_IS),
    COMPARISON(IN_OR_IS, KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ),
    EQUALITY(COMPARISON, KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ),
    CONJUNCTION(EQUALITY, KtTokens.ANDAND),
    DISJUNCTION(CONJUNCTION, KtTokens.OROR),
    ASSIGNMENT(DISJUNCTION, KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ),
    ;

    @Suppress("unused") // Used in IntelliJ
    val tokenSet: TokenSet = TokenSet.create(*tokens)

    companion object {
        /**
         * Defines a map, where each token is mapped on its binary precedence.<p>
         *
         * It's used for fast lookup over binary precedences by a provided token.
         * It works with O(1) complexity in a given use-site instead of O(N) where N is a number of binary precedences (currently 12).
         */
        @JvmField
        val TOKEN_TO_BINARY_PRECEDENCE_MAP_WITH_SOFT_IDENTIFIERS: Map<KtToken, BinaryOperationPrecedence> =
            getTokensToBinaryPrecedenceMap(includeSoftIdentifiers = true)

        @JvmField
        val TOKEN_TO_BINARY_PRECEDENCE_MAP: Map<KtToken, BinaryOperationPrecedence> =
            getTokensToBinaryPrecedenceMap(includeSoftIdentifiers = false)

        private fun getTokensToBinaryPrecedenceMap(includeSoftIdentifiers: Boolean): Map<KtToken, BinaryOperationPrecedence> {
            val result = HashMap<KtToken, BinaryOperationPrecedence>()

            fun register(elementType: IElementType, precedence: BinaryOperationPrecedence) {
                require(elementType is KtToken)

                val existingPrecedence = result.put(elementType, precedence)
                require(existingPrecedence == null) {
                    "All binary precedences have unique operations. The $elementType is already assigned to $existingPrecedence."
                }
            }

            for (entry in entries) {
                for (type in entry.tokens) {
                    register(type, entry)

                    if (type === KtTokens.IDENTIFIER && includeSoftIdentifiers) {
                        // Soft keywords work as identifiers (it's actual for INFIX functions).
                        // However, they are being remapped to IDENTIFIER during parsing.
                        for (softKeyword in KtTokens.SOFT_KEYWORDS.getTypes()) {
                            register(softKeyword, entry)
                        }
                    }
                }
            }

            return Collections.unmodifiableMap(result)
        }
    }
}