// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// LANGUAGE: +AllowContractsOnSomeOperators, +ConditionImpliesReturnsContracts
// ISSUE: KT-79355

import kotlin.contracts.*

operator fun Any?.inc(): Int? {
    contract {
        returns() implies (this@inc != null)
        (this@inc != null) implies returnsNotNull()
    }
    return (this as Int) + 1
}

fun test_inc_dec(ix1: Int?) {
    var x1 = ix1
    x1++
    x1.toChar()
}

/* GENERATED_FIR_TAGS: additiveExpression, asExpression, assignment, contractConditionalEffect,
contractImpliesReturnEffect, contracts, equalityExpression, funWithExtensionReceiver, functionDeclaration,
incrementDecrementExpression, integerLiteral, lambdaLiteral, localProperty, nullableType, operator, propertyDeclaration,
smartcast, thisExpression */
