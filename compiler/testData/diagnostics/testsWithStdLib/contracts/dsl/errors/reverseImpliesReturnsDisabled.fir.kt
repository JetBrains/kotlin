// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: -ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(encoded != null) implies (returnsNotNull())<!>
    }
    if (encoded == null) return null
    return encoded + "a"
}

fun test() {
    // smartcast
    val x = "hello"
    decode(x)<!UNSAFE_CALL!>.<!>length
}

/* GENERATED_FIR_TAGS: additiveExpression, contracts, equalityExpression, functionDeclaration, ifExpression,
lambdaLiteral, localProperty, nullableType, propertyDeclaration, smartcast, stringLiteral */
