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

/**
 * The precedence levels of Kotlin's binary operators, ordered from the highest-binding ([AS]) to the lowest-binding
 * ([ASSIGNMENT]).
 *
 * [higherPriority] links each level to the next tighter-binding one, and [tokens] lists the operator tokens that belong
 * to the level.
 */
enum class BinaryOperationPrecedence(val higherPriority: BinaryOperationPrecedence?, vararg val tokens: KtToken) {
    /** The `as` and `as?` cast operators. */
    AS(null, KtTokens.AS_KEYWORD, KtTokens.AS_SAFE),

    /** The multiplicative operators `*`, `/`, and `%`. */
    MULTIPLICATIVE(AS, KtTokens.MUL, KtTokens.DIV, KtTokens.PERC),

    /** The additive operators `+` and `-`. */
    ADDITIVE(MULTIPLICATIVE, KtTokens.PLUS, KtTokens.MINUS),

    /** The range operators `..` and `..<`. */
    RANGE(ADDITIVE, KtTokens.RANGE, KtTokens.RANGE_UNTIL),

    /** Infix function calls (an identifier used as an operator). */
    INFIX(RANGE, KtTokens.IDENTIFIER),

    /** The elvis operator `?:`. */
    ELVIS(INFIX, KtTokens.ELVIS),

    /** The `in`, `!in`, `is`, and `!is` operators. */
    IN_OR_IS(ELVIS, KtTokens.IN_KEYWORD, KtTokens.NOT_IN, KtTokens.IS_KEYWORD, KtTokens.NOT_IS),

    /** The comparison operators `<`, `>`, `<=`, and `>=`. */
    COMPARISON(IN_OR_IS, KtTokens.LT, KtTokens.GT, KtTokens.LTEQ, KtTokens.GTEQ),

    /** The equality operators `==`, `!=`, `===`, and `!==`. */
    EQUALITY(COMPARISON, KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ),

    /** The conjunction operator `&&`. */
    CONJUNCTION(EQUALITY, KtTokens.ANDAND),

    /** The disjunction operator `||`. */
    DISJUNCTION(CONJUNCTION, KtTokens.OROR),

    /** The assignment operators `=`, `+=`, `-=`, `*=`, `/=`, and `%=`. */
    ASSIGNMENT(DISJUNCTION, KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ),
    ;

    /** The set of operator tokens that belong to this precedence level. */
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

        /**
         * Maps each binary operator token to its [BinaryOperationPrecedence] for O(1) lookup. Soft-keyword identifiers
         * are not included; use [TOKEN_TO_BINARY_PRECEDENCE_MAP_WITH_SOFT_IDENTIFIERS] when they are needed.
         */
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