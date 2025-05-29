// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

fun decodeStringAndNumButNotInt(encoded: Any): String? {
    contract {
        ((encoded is Number && encoded !is Int) || encoded is String) implies (returnsNotNull())
    }
    return when (encoded) {
        is Int -> null
        is Number -> encoded.toString()
        is String -> encoded
        else -> null
    }
}

fun test5(x: Any) {
    if (x is Number)
        if (x is Int)
            decodeStringAndNumButNotInt(x).length // should be error
        else
            decodeStringAndNumButNotInt(x).length
    else if (x is String)
        decodeStringAndNumButNotInt(x).length
    else
        decodeStringAndNumButNotInt(x)<!UNSAFE_CALL!>.<!>length
}

fun test6() {
    decodeStringAndNumButNotInt("abc").length
    decodeStringAndNumButNotInt(42).length // should be error
    decodeStringAndNumButNotInt(42.0).length
}
