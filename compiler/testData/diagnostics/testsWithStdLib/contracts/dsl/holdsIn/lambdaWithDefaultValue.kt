// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79157
import kotlin.contracts.contract

inline fun testDefaultLambda(
    a: String?,
    condition: Boolean = a is String,
    block: () -> Unit = { a<!UNSAFE_CALL!>.<!>length },         //KT-79157 should be OK
) {
    contract { condition holdsIn block }
    block()
}

inline fun testDefaultLambda2(
    a: String?,
    condition: Boolean,
    block: () -> Unit = { a<!UNSAFE_CALL!>.<!>length },         //KT-79157 should be OK
) {
    contract { condition holdsIn block }
    block()
}

fun usage(c: String?) {
    testDefaultLambda(c)
    testDefaultLambda2(c, c is String)
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, functionDeclaration, functionalType, inline, isExpression,
lambdaLiteral, nullableType */
