/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// --------------------------------------- Facts ---------------------------------------

class MutableTypeStatement(
    override val variable: RealVariable,
    override val exactType: MutableSet<ConeKotlinType> = linkedSetOf(),
    override val exactNotType: MutableSet<ConeKotlinType> = linkedSetOf()
) : TypeStatement() {
    override fun invert(): MutableTypeStatement {
        return MutableTypeStatement(
            variable,
            LinkedHashSet(exactNotType),
            LinkedHashSet(exactType)
        )
    }

    fun copy(): MutableTypeStatement = MutableTypeStatement(variable, LinkedHashSet(exactType), LinkedHashSet(exactNotType))
}

fun Implication.invertCondition(): Implication = Implication(condition.invert(), effect)

// --------------------------------------- Aliases ---------------------------------------

typealias TypeStatements = Map<RealVariable, TypeStatement>
typealias MutableTypeStatements = MutableMap<RealVariable, MutableTypeStatement>

fun TypeStatements.asMutableStatements(): MutableTypeStatements =
    mapValuesTo(mutableMapOf()) { it.value.asMutableStatement() }

// --------------------------------------- DSL ---------------------------------------

infix fun DataFlowVariable.eq(constant: Boolean?): OperationStatement {
    val condition = when (constant) {
        true -> Operation.EqTrue
        false -> Operation.EqFalse
        null -> Operation.EqNull
    }
    return OperationStatement(this, condition)
}

infix fun DataFlowVariable.notEq(constant: Boolean?): OperationStatement {
    val condition = when (constant) {
        true -> Operation.EqFalse
        false -> Operation.EqTrue
        null -> Operation.NotEqNull
    }
    return OperationStatement(this, condition)
}

infix fun OperationStatement.implies(effect: Statement<*>): Implication = Implication(this, effect)

infix fun RealVariable.typeEq(type: ConeKotlinType): TypeStatement =
    if (type !is ConeErrorType) {
        MutableTypeStatement(this, linkedSetOf<ConeKotlinType>().apply { this += type }, HashSet())
    } else {
        MutableTypeStatement(this)
    }

infix fun RealVariable.typeNotEq(type: ConeKotlinType): TypeStatement =
    if (type !is ConeErrorType) {
        MutableTypeStatement(this, linkedSetOf(), LinkedHashSet<ConeKotlinType>().apply { this += type })
    } else {
        MutableTypeStatement(this)
    }

// --------------------------------------- Utils ---------------------------------------

@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isSynthetic(): Boolean {
    contract {
        returns(true) implies (this@isSynthetic is SyntheticVariable)
    }
    return this is SyntheticVariable
}

@OptIn(ExperimentalContracts::class)
fun DataFlowVariable.isReal(): Boolean {
    contract {
        returns(true) implies (this@isReal is RealVariable)
    }
    return this is RealVariable
}
