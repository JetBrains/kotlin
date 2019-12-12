/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa.new

import org.jetbrains.kotlin.fir.contracts.description.ConeBooleanConstantReference
import org.jetbrains.kotlin.fir.contracts.description.ConeConstantReference
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirOperation
import org.jetbrains.kotlin.fir.references.FirResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.calls.ConeInferenceContext
import org.jetbrains.kotlin.fir.resolve.dfa.Condition
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeTypeIntersector
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.coneTypeUnsafe
import org.jetbrains.kotlin.resolve.calls.NewCommonSuperTypeCalculator
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun ConeInferenceContext.commonSuperTypeOrNull(types: List<ConeKotlinType>): ConeKotlinType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> with(NewCommonSuperTypeCalculator) {
            commonSuperType(types) as ConeKotlinType
        }
    }
}

fun ConeInferenceContext.intersectTypesOrNull(types: List<ConeKotlinType>): ConeKotlinType? {
    return when (types.size) {
        0 -> null
        1 -> types.first()
        else -> ConeTypeIntersector.intersectTypes(this, types)
    }
}

@UseExperimental(ExperimentalContracts::class)
fun DataFlowVariable.isSynthetic(): Boolean {
    contract {
        returns(true) implies (this@isSynthetic is SyntheticVariable)
        returns(false) implies (this@isSynthetic is RealVariable)
    }
    return this is SyntheticVariable
}

@UseExperimental(ExperimentalContracts::class)
fun DataFlowVariable.isReal(): Boolean {
    contract {
        returns(true) implies (this@isReal is RealVariable)
        returns(false) implies (this@isReal is SyntheticVariable)
    }
    return this is RealVariable
}

operator fun DataFlowInfo.plus(other: DataFlowInfo?): DataFlowInfo = other?.let { this + other } ?: this

fun MutableKnownFacts.addInfo(variable: RealVariable, info: DataFlowInfo) {
    put(variable, info.asMutableInfo()) { it.apply { this += info } }
}

fun MutableKnownFacts.mergeInfo(other: Map<RealVariable, DataFlowInfo>) {
    other.forEach { (variable, info) ->
        addInfo(variable, info)
    }
}

@UseExperimental(ExperimentalContracts::class)
internal inline fun <K, V> MutableMap<K, V>.put(key: K, value: V, remappingFunction: (existing: V) -> V) {
    contract {
        callsInPlace(remappingFunction, InvocationKind.AT_MOST_ONCE)
    }
    val existing = this[key]
    if (existing == null) {
        put(key, value)
    } else {
        put(key, remappingFunction(existing))
    }
}

internal val FirExpression.coneType: ConeKotlinType? get() = typeRef.coneTypeSafe()

internal fun FirOperation.invert(): FirOperation = when (this) {
    FirOperation.EQ -> FirOperation.NOT_EQ
    FirOperation.NOT_EQ -> FirOperation.EQ
    FirOperation.IDENTITY -> FirOperation.NOT_IDENTITY
    FirOperation.NOT_IDENTITY -> FirOperation.IDENTITY
    else -> throw IllegalArgumentException("$this can not be inverted")
}

internal fun FirOperation.isEq(): Boolean {
    return when (this) {
        FirOperation.EQ, FirOperation.IDENTITY -> true
        FirOperation.NOT_EQ, FirOperation.NOT_IDENTITY -> false
        else -> throw IllegalArgumentException("$this should not be there")
    }
}

internal fun FirFunctionCall.isBooleanNot(): Boolean {
    val symbol = calleeReference.safeAs<FirResolvedNamedReference>()?.resolvedSymbol as? FirNamedFunctionSymbol ?: return false
    return symbol.callableId == FirDataFlowAnalyzer.KOTLIN_BOOLEAN_NOT
}

internal fun ConeConstantReference.toCondition(): Condition = when (this) {
    ConeConstantReference.NULL -> Condition.EqNull
    ConeConstantReference.NOT_NULL -> Condition.NotEqNull
    ConeBooleanConstantReference.TRUE -> Condition.EqTrue
    ConeBooleanConstantReference.FALSE -> Condition.EqFalse
    else -> throw IllegalArgumentException("$this can not be transformed to Condition")
}