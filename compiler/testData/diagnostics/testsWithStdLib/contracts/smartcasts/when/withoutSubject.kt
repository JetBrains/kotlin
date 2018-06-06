// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

fun exhaustive(x: Any?) {
    when {
        isString(x) -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        !isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when {
        !isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
        isString(x) -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun smartcastInElse(x: Any?) {
    when {
        !isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}