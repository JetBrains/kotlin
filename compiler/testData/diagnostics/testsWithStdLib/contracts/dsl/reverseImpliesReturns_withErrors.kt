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

fun decodeStringOrChar(encoded: Any): String? {
    contract {
        (encoded is String || encoded is Char) implies (returnsNotNull())
    }
    return when (encoded) {
        is String -> encoded + "a"
        is Char -> encoded + "b"
        else -> null
    }
}

fun tesStringOrChar() {
    decodeStringOrChar(42.0)<!UNSAFE_CALL!>.<!>length
}
