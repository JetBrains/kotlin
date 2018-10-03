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


fun notIsString(x: Any?): Boolean {
    contract {
        returns(false) implies (x is String)
    }
    return x !is String
}




fun testSimple(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>length<!>

    if (isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testSpilling(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>length<!>

    if (isString(x)) <!DEBUG_INFO_SMARTCAST!>x<!>.length

    x.<!UNRESOLVED_REFERENCE!>length<!>
}

fun testInversion(x: Any?) {
    if (notIsString(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun testInversionSpilling(x: Any?) {
    x.<!UNRESOLVED_REFERENCE!>length<!>

    if (notIsString(x)) else <!DEBUG_INFO_SMARTCAST!>x<!>.length

    x.<!UNRESOLVED_REFERENCE!>length<!>
}

