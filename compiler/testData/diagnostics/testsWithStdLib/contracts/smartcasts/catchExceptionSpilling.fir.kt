// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun myAssert(condition: Boolean) {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw kotlin.IllegalArgumentException("Assertion failed")
}

fun testWithCatch(x: Any?) {
    try {
        myAssert(x is String)
        x.length
    } catch (e: java.lang.IllegalArgumentException) { }

    x.<!UNRESOLVED_REFERENCE!>length<!>
}

/* GENERATED_FIR_TAGS: contractConditionalEffect, contracts, functionDeclaration, ifExpression, isExpression,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral, tryExpression */
