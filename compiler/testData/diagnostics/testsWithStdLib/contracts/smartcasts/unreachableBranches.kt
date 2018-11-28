// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

// ===== Definitions ====
fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}


// ==== Actual tests =======

fun implicitAlwaysFalse(x: Any?) {
    if (isString(x) && !isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun implicitAlwaysFalseSpilling(x: Any?) {
    if (isString(x) && !isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}