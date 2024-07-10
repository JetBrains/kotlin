/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

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
 */
data class OperationStatement(override val variable: DataFlowVariable, val operation: Operation) : Statement() {
    override fun toString(): String {
        return "$variable $operation"
    }
}

enum class ResultStatement {
    UNKNOWN,
    SUCCESS,
    FAILURE,
    INCONGRUENT;

    infix fun and(other: ResultStatement): ResultStatement = when {
        this == other -> this
        this == UNKNOWN -> other
        other == UNKNOWN -> this
        else -> INCONGRUENT
    }

    infix fun or(other: ResultStatement): ResultStatement = when {
        this == other -> this
        else -> UNKNOWN
    }
}

sealed class TypeStatement : Statement() {
    abstract override val variable: RealVariable
    abstract val exactType: Set<ConeKotlinType>
    abstract val resultStatement: ResultStatement

    val isEmpty: Boolean
        get() = exactType.isEmpty()

    val isNotEmpty: Boolean
        get() = !isEmpty

    final override fun toString(): String =
        if (resultStatement == ResultStatement.UNKNOWN)
            "$variable: ${exactType.joinToString(separator = " & ")}"
        else
            "$variable: ${exactType.joinToString(separator = " & ")} ($resultStatement)"
}

class Implication(
    val condition: OperationStatement,
    val effect: Statement
) {
    override fun toString(): String {
        return "$condition -> $effect"
    }
}

enum class Operation {
    EqTrue, EqFalse, EqNull, NotEqNull;

    fun valueIfKnown(given: Operation): Boolean? = when (this) {
        EqTrue, EqFalse -> if (given == NotEqNull) null else given == this
        EqNull -> given == EqNull
        NotEqNull -> given != EqNull
    }

    override fun toString(): String = when (this) {
        EqTrue -> "== True"
        EqFalse -> "== False"
        EqNull -> "== Null"
        NotEqNull -> "!= Null"
    }
}
