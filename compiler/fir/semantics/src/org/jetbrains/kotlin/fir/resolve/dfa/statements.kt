/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType

sealed class Statement {
    abstract val variable: DataFlowVariable
}

/*
 * Examples:
 * d == Null
 * d != Null
 * d == True
 * d == False
 * d != <symbol>
 */
data class OperationStatement(override val variable: DataFlowVariable, val operation: Operation) : Statement() {
    override fun toString(): String {
        return "$variable $operation"
    }
}

/**
 * Essentially, an optimized set of [OperationStatement]s with [Operation.NotEqSymbol].
 * Exists as a separate statement because:
 *
 * - A literal set of [OperationStatement]s with [Operation.NotEqSymbol] would contain redundant noise such as
 *   `variable: DataFlowVariable` in every single statement or the extra reference that [Operation.NotEqSymbol]
 *   itself is
 * - When used as an [Implication] premise, we'd like to only deal with one-value inequality statements,
 *   but when considering inequalities of a variable, we'd like to have the set of all the values.
 * - We want our API to only allow storing inequalities to values, not arbitrary [OperationStatement]s
 */
sealed class ValueStatement : Statement() {
    abstract override val variable: RealVariable
    abstract val exactNonValues: Set<FirBasedSymbol<*>>

    override fun toString(): String {
        return "$variable !in [${exactNonValues.joinToString(" & ")}]"
    }
}

sealed class TypeStatement : Statement() {
    abstract override val variable: RealVariable
    abstract val exactType: Set<ConeKotlinType>
    abstract val exactNonType: Set<ConeKotlinType>

    val isEmpty: Boolean
        get() = exactType.isEmpty() && exactNonType.isEmpty()

    val isNotEmpty: Boolean
        get() = !isEmpty

    final override fun toString(): String {
        return "$variable: ${renderType()}"
    }

    fun renderType(): String = listOfNotNull(exactTypeStringOrNull, exactNonTypeStringOrNull).joinToString(" & ")

    val exactTypeOrNull: Set<ConeKotlinType>? get() = exactType.takeIf { it.isNotEmpty() }
    val exactNonTypeOrNull: Set<ConeKotlinType>? get() = exactNonType.takeIf { it.isNotEmpty() }

    private val exactTypeStringOrNull: String? get() = exactTypeOrNull?.joinToString(separator = " & ")
    private val exactNonTypeStringOrNull: String? get() = exactNonTypeOrNull?.joinToString(separator = " | ")?.let { "¬($it)" }
}

class Implication(
    val condition: OperationStatement,
    val effect: Statement
) {
    override fun toString(): String {
        return "$condition -> $effect"
    }
}

sealed class Operation {
    data object EqTrue : Operation()
    data object EqFalse : Operation()
    data object EqNull : Operation()
    data object NotEqNull : Operation()

    /**
     * Exists separately from [NotEqNull] because the latter is used in contract
     * descriptions, where everything is resolved to the point that we keep no [FirExpression]s.
     * You could say that [NotEqNull] and [NotEqSymbol] are optimized corner cases
     * of some more general `NotEq`, which we don't need to support yet.
     *
     * Additionally, only [NotEqNull] is considered for smart-casting; inequality to
     * other values is only used when verifying `when` exhaustiveness.
     *
     * [symbol] should point to a singleton with adequate equality.
     * That is, to a construction unique up to `equals()` (like enum entries).
     * A counter-example: you can have multiple non-equal deserialized instances of `object O`.
     */
    data class NotEqSymbol(val symbol: FirBasedSymbol<*>) : Operation()

    fun valueIfKnown(given: Operation): Boolean? = when (this) {
        EqTrue, EqFalse -> if (given == NotEqNull) null else given == this
        EqNull -> given == EqNull
        NotEqNull -> given != EqNull
        is NotEqSymbol -> given is NotEqSymbol && given.symbol == symbol
    }

    override fun toString(): String = when (this) {
        EqTrue -> "== True"
        EqFalse -> "== False"
        EqNull -> "== Null"
        NotEqNull -> "!= Null"
        is NotEqSymbol -> "!= $symbol"
    }
}
