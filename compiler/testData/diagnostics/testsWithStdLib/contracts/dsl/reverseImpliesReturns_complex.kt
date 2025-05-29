// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

fun decodeNotNumber(encoded: Any): String? {
    contract {
        (encoded !is Number) implies (returnsNotNull())
    }
    return when (encoded) {
        is Number -> null
        else -> encoded.toString()
    }
}

fun test1(x: Any) {
    if (x !is Number)
        decodeNotNumber(x).length
    else
        decodeNotNumber(x)<!UNSAFE_CALL!>.<!>length
}

fun test2() {
    decodeNotNumber("abc").length
    decodeNotNumber(42)<!UNSAFE_CALL!>.<!>length
}

fun decodeNumberNotInt(encoded: Any): String? {
    contract {
        (encoded is Number && encoded !is Int) implies (returnsNotNull())
    }
    return when (encoded) {
        is Int -> null
        is Number -> encoded.toString()
        else -> null
    }
}

fun test3(x: Any) {
    if (x is Number)
        if (x is Int)
            decodeNumberNotInt(x)<!UNSAFE_CALL!>.<!>length
    else
        decodeNumberNotInt(x).length
    else
        decodeNumberNotInt(x)<!UNSAFE_CALL!>.<!>length
}

fun test4() {
    decodeNumberNotInt("abc")<!UNSAFE_CALL!>.<!>length
    decodeNumberNotInt(42)<!UNSAFE_CALL!>.<!>length
    decodeNumberNotInt(42.0).length
}
