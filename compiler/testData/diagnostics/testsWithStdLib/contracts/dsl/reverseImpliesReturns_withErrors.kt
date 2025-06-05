// FIR_IDENTICAL
// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts, kotlin.contracts.ExperimentalExtendedContracts

import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        (encoded != null) implies (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

fun testParam(x: String?) {
    val xx: String? = null
    if (x == null)
        decode(x)<!UNSAFE_CALL!>.<!>length
}

fun testVar() {
    val x: String? = null
    decode(x)<!UNSAFE_CALL!>.<!>length
}

fun testLiteral() {
    decode(null)<!UNSAFE_CALL!>.<!>length
}

fun decodeFake(encoded: String?): String? {
    contract {
        (encoded == null) implies (returnsNotNull())
    }
    return if (encoded == null) "hello" else null
}

fun testFake() {
    decodeFake("bye")<!UNSAFE_CALL!>.<!>length
}

fun decodeString(encoded: Any): String? {
    contract {
        (encoded is String) implies (returnsNotNull())
    }
    return when (encoded) {
        is String -> encoded + "a"
        else -> null
    }
}

fun tesStringOrChar() {
    decodeString(42.0)<!UNSAFE_CALL!>.<!>length
}

fun decodeNotNumber(encoded: Any): String? {
    contract {
        (encoded !is Number) implies (returnsNotNull())
    }
    return when (encoded) {
        is Number -> null
        else -> encoded.toString()
    }
}

fun testNotNumber(x: Any) {
    if (x is Number)
        decodeNotNumber(x)<!UNSAFE_CALL!>.<!>length
    decodeNotNumber(42)<!UNSAFE_CALL!>.<!>length
}
