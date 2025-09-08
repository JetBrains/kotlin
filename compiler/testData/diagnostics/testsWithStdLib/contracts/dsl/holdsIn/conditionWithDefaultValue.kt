// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +HoldsInContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts
// ISSUE: KT-79157
import kotlin.contracts.contract

inline fun testDefaultArguments(a: String?, condition: Boolean = a is String, block: () -> Unit) {
    contract { condition holdsIn block }
    block()
}

fun usageWithDefaultArguments(c: String?) {
    testDefaultArguments(c) {
        c<!UNSAFE_CALL!>.<!>length      //KT-79157 should be OK
    }
}

/* GENERATED_FIR_TAGS: contractHoldsInEffect, contracts, functionDeclaration, functionalType, inline, isExpression,
lambdaLiteral, nullableType */
