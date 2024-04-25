// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

fun test(x: Any?) {
    if (isString(x)) {
        x.length
    }
}

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}


