/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import kotlinx.collections.immutable.PersistentSet
import org.jetbrains.kotlin.fir.DfaType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// --------------------------------------- Facts ---------------------------------------

data class PersistentTypeStatement(
    override val variable: DataFlowVariable,
    override val upperTypes: PersistentSet<ConeKotlinType>,
    override val lowerTypes: PersistentSet<DfaType>,
) : TypeStatement()

class MutableTypeStatement(
    override val variable: DataFlowVariable,
    override val upperTypes: MutableSet<ConeKotlinType> = linkedSetOf(),
    override val lowerTypes: MutableSet<DfaType> = linkedSetOf(),
) : TypeStatement()

// --------------------------------------- Aliases ---------------------------------------

typealias TypeStatements = Map<DataFlowVariable, TypeStatement>

// --------------------------------------- DSL ---------------------------------------

infix fun DataFlowVariable.eq(constant: Boolean): OperationStatement =
    OperationStatement(this, if (constant) Operation.EqTrue else Operation.EqFalse)

@Suppress("UNUSED_PARAMETER")
infix fun DataFlowVariable.eq(constant: Nothing?): OperationStatement =
    OperationStatement(this, Operation.EqNull)

@Suppress("UNUSED_PARAMETER")
infix fun DataFlowVariable.notEq(constant: Nothing?): OperationStatement =
    OperationStatement(this, Operation.NotEqNull)

infix fun OperationStatement.implies(effect: Statement): Implication = Implication(this, effect)

infix fun RealVariable.valueNotEq(symbol: FirBasedSymbol<*>): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.Symbol(symbol)))

infix fun RealVariable.valueNotEq(boolean: Boolean): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = linkedSetOf(DfaType.BooleanLiteral(boolean)))

infix fun RealVariable.typeEq(type: ConeKotlinType): MutableTypeStatement =
    MutableTypeStatement(this, if (type is ConeErrorType) linkedSetOf() else linkedSetOf(type))

infix fun RealVariable.typeNotEq(type: ConeKotlinType): MutableTypeStatement =
    MutableTypeStatement(this, lowerTypes = if (type is ConeErrorType) linkedSetOf() else linkedSetOf(DfaType.Cone(type)))


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
