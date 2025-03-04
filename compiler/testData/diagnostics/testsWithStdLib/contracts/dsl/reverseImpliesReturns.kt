// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
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

fun testVar() {
    val x: String = ""
    decode(x).length
}

fun testParam(x: String?) {
    if (x != null)
        decode(x).length
}

fun testLiteral() {
    decode("").length
}

fun decodeFake(encoded: String?): String? {
    contract {
        (encoded == null) implies (returnsNotNull())
    }
    return if (encoded == null) "hello" else null
}

fun testFake() {
    val x = null
    decodeFake(x).length
}

fun decodeIfString(encoded: Any): String? {
    contract {
        (encoded is String) implies (returnsNotNull())
    }
    return when (encoded) {
        is String -> encoded + "a"
        else -> null
    }
}

fun tesStringOrChar() {
    decodeIfString("abc").length
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
    if (x !is Number)
        decodeNotNumber(x).length
    decodeNotNumber("abc").length
}

