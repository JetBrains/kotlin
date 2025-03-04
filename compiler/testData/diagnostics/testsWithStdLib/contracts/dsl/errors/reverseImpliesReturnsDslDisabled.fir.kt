// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +AllowReverseImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        (encoded != null) <!OPT_IN_USAGE_ERROR!>implies<!> (returnsNotNull())
    }
    if (encoded == null) return null
    return encoded + "a"
}

fun test() {
    // smartcast
    val x = "hello"
    decode(x).length
}

