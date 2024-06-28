/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.dfa

import org.jetbrains.kotlin.contracts.description.LogicOperationKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.contracts.description.*
import org.jetbrains.kotlin.fir.resolve.substitution.ConeSubstitutor
import org.jetbrains.kotlin.fir.types.canBeNull
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isNullableNothing

fun ConeConstantReference.toOperation(): Operation? = when (this) {
    ConeContractConstantValues.WILDCARD -> null
    ConeContractConstantValues.NULL -> Operation.EqNull
    ConeContractConstantValues.NOT_NULL -> Operation.NotEqNull
    ConeContractConstantValues.TRUE -> Operation.EqTrue
    ConeContractConstantValues.FALSE -> Operation.EqFalse
    else -> throw IllegalArgumentException("$this can not be transformed to Operation")
}

// Returns `null` if the statement is always false.
fun LogicSystem.approveContractStatement(
    statement: ConeBooleanExpression,
    arguments: Array<out DataFlowVariable?>, // 0 = receiver (null if doesn't exist)
    substitutor: ConeSubstitutor?,
    approveOperationStatement: (OperationStatement) -> TypeStatements
): TypeStatements? {
    fun DataFlowVariable.processEqNull(isEq: Boolean): TypeStatements =
        approveOperationStatement(OperationStatement(this, if (isEq) Operation.EqNull else Operation.NotEqNull))

    fun ConeBooleanExpression.visit(inverted: Boolean): TypeStatements? = when (this) {
        is ConeBooleanConstantReference ->
            if (inverted == (this == ConeContractConstantValues.TRUE)) null else mapOf()
        is ConeLogicalNot -> arg.visit(inverted = !inverted)
        is ConeIsInstancePredicate ->
            arguments.getOrNull(arg.parameterIndex + 1)?.let {
                val isType = inverted == isNegated
                val substitutedType = substitutor?.substituteOrNull(type) ?: type
                when {
                    substitutedType.isAny -> it.processEqNull(!isType)
                    substitutedType.isNullableNothing -> it.processEqNull(isType)
                    else -> {
                        // x is (T & Any) || x !is T? => x != null
                        val fromNullability = if ((isType && !type.canBeNull(session)) || (!isType && type.isMarkedNullable))
                            it.processEqNull(false)
                        else
                            mapOf()
                        if (isType && it is RealVariable) {
                            andForTypeStatements(fromNullability, mapOf(it to (it typeEq substitutedType)))
                        } else {
                            fromNullability
                        }
                    }
                }
            } ?: mapOf()
        is ConeIsNullPredicate ->
            arguments.getOrNull(arg.parameterIndex + 1)?.processEqNull(inverted == isNegated) ?: mapOf()
        is ConeBooleanValueParameterReference ->
            arguments.getOrNull(parameterIndex + 1)?.let { approveOperationStatement(it eq !inverted) } ?: mapOf()
        is ConeBinaryLogicExpression -> {
            val a = left.visit(inverted)
            val b = right.visit(inverted)
            val isAnd = inverted != (kind == LogicOperationKind.AND)
            when {
                a == null -> b.takeIf { !isAnd } // false || b == b; false && b = false
                b == null -> a.takeIf { !isAnd } // a || false == a; a && false = false
                isAnd -> andForTypeStatements(a, b)
                else -> orForTypeStatements(a, b)
            }
        }
        else -> mapOf()
    }

    return statement.visit(inverted = false)
}
