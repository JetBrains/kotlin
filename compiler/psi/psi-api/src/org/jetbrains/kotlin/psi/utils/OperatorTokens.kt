/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi.utils

import com.google.common.collect.ImmutableBiMap
import org.jetbrains.kotlin.lexer.KtSingleValueToken
import org.jetbrains.kotlin.lexer.KtToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.util.*

object OperatorTokens {
    // If you add new unary, binary or assignment operators, add it to OperatorConventionNames as well
    private val UNARY_OPERATIONS = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.PLUSPLUS, OperatorNameConventions.INC)
        .put(KtTokens.MINUSMINUS, OperatorNameConventions.DEC)
        .put(KtTokens.PLUS, OperatorNameConventions.UNARY_PLUS)
        .put(KtTokens.MINUS, OperatorNameConventions.UNARY_MINUS)
        .put(KtTokens.EXCL, OperatorNameConventions.NOT)
        .build()

    @JvmField
    val UNARY_OPERATION_NAMES: Map<KtSingleValueToken, Name> = UNARY_OPERATIONS

    @JvmField
    val UNARY_OPERATION_TOKENS: Map<Name, KtSingleValueToken> = UNARY_OPERATIONS.inverse()

    private val BINARY_OPERATIONS = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.MUL, OperatorNameConventions.TIMES)
        .put(KtTokens.PLUS, OperatorNameConventions.PLUS)
        .put(KtTokens.MINUS, OperatorNameConventions.MINUS)
        .put(KtTokens.DIV, OperatorNameConventions.DIV)
        .put(KtTokens.PERC, OperatorNameConventions.REM)
        .put(KtTokens.RANGE, OperatorNameConventions.RANGE_TO)
        .put(KtTokens.RANGE_UNTIL, OperatorNameConventions.RANGE_UNTIL)
        .build()

    @JvmField
    val BINARY_OPERATION_NAMES: Map<KtSingleValueToken, Name> = BINARY_OPERATIONS

    @JvmField
    val BINARY_OPERATION_TOKENS: Map<Name, KtSingleValueToken> = BINARY_OPERATIONS.inverse()

    @JvmField
    val INCREMENT_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.PLUSPLUS,
            KtTokens.MINUSMINUS
        )
    )

    @JvmField
    val COMPARISON_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.LT,
            KtTokens.GT,
            KtTokens.LTEQ,
            KtTokens.GTEQ
        )
    )

    @JvmField
    val EQUALS_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.EQEQ,
            KtTokens.EXCLEQ
        )
    )

    @JvmField
    val IDENTITY_EQUALS_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.EQEQEQ,
            KtTokens.EXCLEQEQEQ
        )
    )

    @JvmField
    val IN_OPERATIONS: Set<KtSingleValueToken> = Collections.unmodifiableSet(
        setOf(
            KtTokens.IN_KEYWORD,
            KtTokens.NOT_IN
        )
    )

    private val ASSIGNMENT_OPERATIONS = ImmutableBiMap.builder<KtSingleValueToken, Name>()
        .put(KtTokens.MULTEQ, OperatorNameConventions.TIMES_ASSIGN)
        .put(KtTokens.DIVEQ, OperatorNameConventions.DIV_ASSIGN)
        .put(KtTokens.PERCEQ, OperatorNameConventions.REM_ASSIGN)
        .put(KtTokens.PLUSEQ, OperatorNameConventions.PLUS_ASSIGN)
        .put(KtTokens.MINUSEQ, OperatorNameConventions.MINUS_ASSIGN)
        .build()

    @JvmField
    val ASSIGNMENT_OPERATION_NAMES: Map<KtSingleValueToken, Name> = ASSIGNMENT_OPERATIONS

    @JvmField
    val ASSIGNMENT_OPERATION_TOKENS: Map<Name, KtSingleValueToken> = ASSIGNMENT_OPERATIONS.inverse()

    private val ASSIGNMENT_OPERATION_COUNTERPARTS = ImmutableBiMap.builder<KtSingleValueToken, KtSingleValueToken>()
        .put(KtTokens.MULTEQ, KtTokens.MUL)
        .put(KtTokens.DIVEQ, KtTokens.DIV)
        .put(KtTokens.PERCEQ, KtTokens.PERC)
        .put(KtTokens.PLUSEQ, KtTokens.PLUS)
        .put(KtTokens.MINUSEQ, KtTokens.MINUS)
        .build()

    @JvmField
    val OPERATIONS_FOR_ASSIGNMENTS: Map<KtSingleValueToken, KtSingleValueToken> = ASSIGNMENT_OPERATION_COUNTERPARTS

    @JvmField
    val ASSIGNMENTS_FOR_OPERATIONS: Map<KtSingleValueToken, KtSingleValueToken> = ASSIGNMENT_OPERATION_COUNTERPARTS.inverse()

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


    @JvmStatic
    fun operationName(token: KtToken): Name? {
        return operationName(token, unaryOperations = true, binaryOperations = true)
    }

    @JvmStatic
    fun operationName(token: KtToken, unaryOperations: Boolean, binaryOperations: Boolean): Name? {
        if (binaryOperations) {
            BINARY_OPERATION_NAMES[token]?.let { return it }
        }

        if (unaryOperations) {
            UNARY_OPERATION_NAMES[token]?.let { return it }
        }

        ASSIGNMENT_OPERATIONS[token]?.let { return it }

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

    @JvmStatic
    fun operationToken(name: Name): KtToken? {
        if (!isConventionName(name)) {
            return null
        }

        return BINARY_OPERATIONS.inverse()[name]
            ?: UNARY_OPERATIONS.inverse()[name]
            ?: ASSIGNMENT_OPERATIONS.inverse()[name]
    }

    @JvmStatic
    fun isConventionName(name: Name): Boolean {
        return CONVENTION_NAMES.contains(name)
                || OperatorNameConventions.COMPONENT_REGEX.matches(name.asString())
    }
}