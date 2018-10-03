// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

fun exhaustive(x: Any?) {
    when (isString(x)) {
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when(!isString(x)) {
        true -> x.<!UNRESOLVED_REFERENCE!>length<!>
        false -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun smartcastInElse(x: Any?) {
    when (isString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }

    when (!isString(x)) {
        true -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}