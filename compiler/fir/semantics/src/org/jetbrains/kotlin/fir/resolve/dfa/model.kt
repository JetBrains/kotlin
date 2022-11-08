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
    fun copy(): MutableTypeStatement = MutableTypeStatement(variable, LinkedHashSet(exactType), LinkedHashSet(exactNotType))
}

fun Implication.invertCondition(): Implication = Implication(condition.invert(), effect)

// --------------------------------------- Aliases ---------------------------------------

typealias TypeStatements = Map<RealVariable, TypeStatement>

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

infix fun OperationStatement.implies(effect: Statement): Implication = Implication(this, effect)

infix fun RealVariable.typeEq(type: ConeKotlinType): MutableTypeStatement =
    MutableTypeStatement(this) andTypeEq type

infix fun RealVariable.typeNotEq(type: ConeKotlinType): MutableTypeStatement =
    MutableTypeStatement(this) andTypeNotEq type

infix fun MutableTypeStatement.andTypeEq(type: ConeKotlinType): MutableTypeStatement =
    this.apply {
        if (type !is ConeErrorType) {
            exactType += type
        }
    }

infix fun MutableTypeStatement.andTypeNotEq(type: ConeKotlinType): MutableTypeStatement =
    this.apply {
        require(exactNotType.isEmpty()) { "statement $this already has a negation; use `logicSystem.and`" }
        if (type !is ConeErrorType) {
            exactNotType += type
        }
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
