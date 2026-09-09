/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.utils

import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

/**
 * Maps between Kotlin operator tokens and the operator-convention function names they desugar to (for example, `+` maps to `plus`), and
 * groups the operator tokens by category (unary, binary, assignment, comparison, and so on).
 */
object OperatorTokens {
    /**
     * Maps each unary operator token (such as `++` or `-`) to its operator-convention function name (such as `inc` or `unaryMinus`).
     */
    @JvmField
    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well
    val UNARY_OPERATION_NAMES: Map<KtSingleValueToken, Name> = Collections.unmodifiableMap(
        linkedMapOf(
            KtTokens.PLUSPLUS to OperatorNameConventions.INC,
            KtTokens.MINUSMINUS to OperatorNameConventions.DEC,
            KtTokens.PLUS to OperatorNameConventions.UNARY_PLUS,
            KtTokens.MINUS to OperatorNameConventions.UNARY_MINUS,
            KtTokens.EXCL to OperatorNameConventions.NOT,
        )
    )

    /**
     * The inverse of [UNARY_OPERATION_NAMES]: maps each unary operator function name to its token.
     */
    @JvmField
    val UNARY_OPERATION_TOKENS: Map<Name, KtSingleValueToken> = UNARY_OPERATION_NAMES.inverted()

    /**
     * Maps each binary operator token (such as `*`, `+`, or `..`) to its operator-convention function name (such as `times`, `plus`,
     * or `rangeTo`).
     */
    @JvmField
    val BINARY_OPERATION_NAMES: Map<KtSingleValueToken, Name> = Collections.unmodifiableMap(
        linkedMapOf(
            KtTokens.MUL to OperatorNameConventions.TIMES,
            KtTokens.PLUS to OperatorNameConventions.PLUS,
            KtTokens.MINUS to OperatorNameConventions.MINUS,
            KtTokens.DIV to OperatorNameConventions.DIV,
            KtTokens.PERC to OperatorNameConventions.REM,
            KtTokens.RANGE to OperatorNameConventions.RANGE_TO,
            KtTokens.RANGE_UNTIL to OperatorNameConventions.RANGE_UNTIL,
        )
    )

    /**
     * The inverse of [BINARY_OPERATION_NAMES]: maps each binary operator function name to its token.
     */
    @JvmField
    val BINARY_OPERATION_TOKENS: Map<Name, KtSingleValueToken> = BINARY_OPERATION_NAMES.inverted()

    /**
     * The increment and decrement operator tokens (`++` and `--`).
     */
    @JvmField
    val INCREMENT_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.PLUSPLUS,
            KtTokens.MINUSMINUS
        )
    )

    /**
     * The comparison operator tokens (`<`, `>`, `<=`, and `>=`), all of which desugar to `compareTo`.
     */
    @JvmField
    val COMPARISON_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.LT,
            KtTokens.GT,
            KtTokens.LTEQ,
            KtTokens.GTEQ
        )
    )

    /**
     * The structural equality operator tokens (`==` and `!=`), which desugar to `equals`.
     */
    @JvmField
    val EQUALS_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.EQEQ,
            KtTokens.EXCLEQ
        )
    )

    /**
     * The referential equality operator tokens (`===` and `!==`).
     */
    @JvmField
    val IDENTITY_EQUALS_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ
        )
    )

    /**
     * The containment operator tokens (`in` and `!in`), which desugar to `contains`.
     */
    @JvmField
    val IN_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.IN_KEYWORD,
            KtTokens.NOT_IN
        )
    )

    /**
     * Maps each augmented assignment operator token (such as `+=` or `*=`) to its operator-convention function name (such as `plusAssign`
     * or `timesAssign`).
     */
    @JvmField
    val ASSIGNMENT_OPERATION_NAMES: Map<KtSingleValueToken, Name> = Collections.unmodifiableMap(
        linkedMapOf(
            KtTokens.MULTEQ to OperatorNameConventions.TIMES_ASSIGN,
            KtTokens.DIVEQ to OperatorNameConventions.DIV_ASSIGN,
            KtTokens.PERCEQ to OperatorNameConventions.REM_ASSIGN,
            KtTokens.PLUSEQ to OperatorNameConventions.PLUS_ASSIGN,
            KtTokens.MINUSEQ to OperatorNameConventions.MINUS_ASSIGN,
        )
    )

    /**
     * The inverse of [ASSIGNMENT_OPERATION_NAMES]: maps each augmented assignment function name to its token.
     */
    @JvmField
    val ASSIGNMENT_OPERATION_TOKENS: Map<Name, KtSingleValueToken> = ASSIGNMENT_OPERATION_NAMES.inverted()

    /**
     * Maps each augmented assignment token (such as `+=`) to the corresponding binary operator token (such as `+`).
     */
    @JvmField
    val OPERATIONS_FOR_ASSIGNMENTS: Map<KtSingleValueToken, KtSingleValueToken> = Collections.unmodifiableMap(
        linkedMapOf(
            KtTokens.MULTEQ to KtTokens.MUL,
            KtTokens.DIVEQ to KtTokens.DIV,
            KtTokens.PERCEQ to KtTokens.PERC,
            KtTokens.PLUSEQ to KtTokens.PLUS,
            KtTokens.MINUSEQ to KtTokens.MINUS,
        )
    )

    /**
     * The inverse of [OPERATIONS_FOR_ASSIGNMENTS]: maps each binary operator token (such as `+`) to the corresponding augmented assignment
     * token (such as `+=`).
     */
    @JvmField
    val ASSIGNMENTS_FOR_OPERATIONS: Map<KtSingleValueToken, KtSingleValueToken> = OPERATIONS_FOR_ASSIGNMENTS.inverted()

    /**
     * The set of fixed function names that carry a special operator meaning in Kotlin. This includes names such as `get`, `set`,
     * `invoke`, `iterator`, `equals`, and `compareTo`, together with every unary, binary, and assignment operator name. Dynamically named
     * `componentN` conventions are recognized by [isConventionName] but are not members of this set.
     */
    @JvmField
    val CONVENTION_NAMES: Set<Name> = Collections.unmodifiableSet(
        buildSet {
            addAll(
                setOf(
                    OperatorNameConventions.GET_VALUE,
                    OperatorNameConventions.SET_VALUE,
                    OperatorNameConventions.PROVIDE_DELEGATE,
                    OperatorNameConventions.EQUALS,
                    OperatorNameConventions.COMPARE_TO,
                    OperatorNameConventions.CONTAINS,
                    OperatorNameConventions.INVOKE,
                    OperatorNameConventions.ITERATOR,
                    OperatorNameConventions.GET,
                    OperatorNameConventions.SET,
                    OperatorNameConventions.NEXT,
                    OperatorNameConventions.HAS_NEXT,
                    OperatorNameConventions.OF,
                )
            )
            addAll(UNARY_OPERATION_NAMES.values)
            addAll(BINARY_OPERATION_NAMES.values)
            addAll(ASSIGNMENT_OPERATION_NAMES.values)
        }
    )


    /**
     * Returns the operator-convention function name that the given [token] desugars to, considering unary, binary, and assignment operators
     * as well as comparison, equality, and containment operators, or `null` if [token] is not an operator.
     */
    @JvmStatic
    fun operationName(token: KtToken): Name? {
        return operationName(token, unaryOperations = true, binaryOperations = true)
    }

    /**
     * Returns the operator-convention function name that the given [token] desugars to, or `null` if [token] is not a matching operator.
     * [unaryOperations] and [binaryOperations] control whether unary and binary operator tokens are considered; assignment, comparison,
     * equality, and containment operators are always considered.
     */
    @JvmStatic
    fun operationName(token: KtToken, unaryOperations: Boolean, binaryOperations: Boolean): Name? {
        if (binaryOperations) {
            BINARY_OPERATION_NAMES[token]?.let { return it }
        }

        if (unaryOperations) {
            UNARY_OPERATION_NAMES[token]?.let { return it }
        }

        ASSIGNMENT_OPERATION_NAMES[token]?.let { return it }

        if (COMPARISON_OPERATIONS.contains(token)) {
            return OperatorNameConventions.COMPARE_TO
        }

        if (EQUALS_OPERATIONS.contains(token)) {
            return OperatorNameConventions.EQUALS
        }

        if (IN_OPERATIONS.contains(token)) {
            return OperatorNameConventions.CONTAINS
        }

        return null
    }

    /**
     * Returns the operator token corresponding to the given operator-convention function [name] (for binary, unary, or assignment
     * operators), or `null` if [name] is not such an operator name.
     */
    @JvmStatic
    fun operationToken(name: Name): KtToken? {
        if (!isConventionName(name)) {
            return null
        }

        return BINARY_OPERATION_TOKENS[name]
            ?: UNARY_OPERATION_TOKENS[name]
            ?: ASSIGNMENT_OPERATION_TOKENS[name]
    }

    /**
     * Returns `true` if the given [name] is an operator-convention function name — either one of [CONVENTION_NAMES] or a `componentN`
     * destructuring name.
     */
    @JvmStatic
    fun isConventionName(name: Name): Boolean {
        return CONVENTION_NAMES.contains(name)
                || OperatorNameConventions.COMPONENT_REGEX.matches(name.asString())
    }

    /** Builds the inverse of this map, requiring the values to be unique the way a bidirectional map does. */
    private fun <K : Any, V : Any> Map<K, V>.inverted(): Map<V, K> {
        val result = LinkedHashMap<V, K>(size)
        for ((key, value) in this) {
            val previous = result.put(value, key)
            check(previous == null) { "Duplicate value: $value" }
        }
        return Collections.unmodifiableMap(result)
    }
}
