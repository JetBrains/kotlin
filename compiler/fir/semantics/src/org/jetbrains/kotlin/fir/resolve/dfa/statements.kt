/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.ConeKotlinType

sealed class Statement<T : Statement<T>> {
    abstract fun invert(): T
    abstract val variable: DataFlowVariable
}

/*
 * Examples:
 * d == Null
 * d != Null
 * d == True
 * d == False
 */
data class OperationStatement(override val variable: DataFlowVariable, val operation: Operation) : Statement<OperationStatement>() {
    override fun invert(): OperationStatement {
        return OperationStatement(variable, operation.invert())
    }

    override fun toString(): String {
        return "$variable $operation"
    }
}

abstract class TypeStatement : Statement<TypeStatement>() {
    abstract override val variable: RealVariable
    abstract val exactType: Set<ConeKotlinType>
    abstract val exactNotType: Set<ConeKotlinType>

    abstract operator fun plus(other: TypeStatement): TypeStatement
    abstract val isEmpty: Boolean
    val isNotEmpty: Boolean get() = !isEmpty

    override fun toString(): String {
        return "$variable: $exactType, $exactNotType"
    }
}

class Implication(
    val condition: OperationStatement,
    val effect: Statement<*>
) {
    override fun toString(): String {
        return "$condition -> $effect"
    }
}

enum class Operation {
    EqTrue, EqFalse, EqNull, NotEqNull;

    fun invert(): Operation = when (this) {
        EqTrue -> EqFalse
        EqFalse -> EqTrue
        EqNull -> NotEqNull
        NotEqNull -> EqNull
    }

    override fun toString(): String = when (this) {
        EqTrue -> "== True"
        EqFalse -> "== False"
        EqNull -> "== Null"
        NotEqNull -> "!= Null"
    }
}
