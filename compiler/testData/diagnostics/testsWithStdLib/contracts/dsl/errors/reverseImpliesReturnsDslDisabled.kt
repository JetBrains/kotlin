// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ConditionImpliesReturnsContracts
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun decode(encoded: String?): String? {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>(encoded != null) <!OPT_IN_USAGE_ERROR!>implies<!> (returnsNotNull())<!>
    }
    if (encoded == null) return null
    return <!DEBUG_INFO_SMARTCAST!>encoded<!> + "a"
}

fun test() {
    // smartcast
    val x = "hello"
    decode(x)<!UNSAFE_CALL!>.<!>length
}

