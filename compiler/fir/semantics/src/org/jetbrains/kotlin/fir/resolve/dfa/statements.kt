/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.DfaType
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

sealed class TypeStatement : Statement() {
    abstract override val variable: DataFlowVariable
    abstract val upperTypes: Set<ConeKotlinType>
    abstract val lowerTypes: Set<DfaType>

    val isEmpty: Boolean
        get() = upperTypes.isEmpty() && lowerTypes.isEmpty()

    val isNotEmpty: Boolean
        get() = !isEmpty

    final override fun toString(): String {
        return "$variable: ${renderType()}"
    }

    fun renderType(): String = listOfNotNull(upperTypesStringOrNull, lowerTypesStringOrNull).joinToString(" & ")

    val upperTypesOrNull: Set<ConeKotlinType>? get() = upperTypes.takeIf { it.isNotEmpty() }
    val lowerTypesOrNull: Set<DfaType>? get() = lowerTypes.takeIf { it.isNotEmpty() }

    private val upperTypesStringOrNull: String? get() = upperTypesOrNull?.joinToString(separator = " & ")
    private val lowerTypesStringOrNull: String? get() = lowerTypesOrNull?.joinToString(separator = " | ")?.let { "¬($it)" }
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
