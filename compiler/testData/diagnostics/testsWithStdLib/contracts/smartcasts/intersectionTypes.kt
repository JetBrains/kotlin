// !LANGUAGE: +ReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun notIsString(x: Any?): Boolean {
    contract {
        returns(false) implies (x is String)
    }
    return x !is String
}

fun notIsInt(x: Any?): Boolean {
    contract {
        returns(false) implies (x is Int)
    }
    return x !is Int
}

fun testDeMorgan(x: Any?) {
       // !(x !is String || x !is Int)
       // x is String && x is Int
    if (!(notIsString(x) || notIsInt(x))) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
}

fun testDeMorgan2(x: Any?) {
        // x !is String || x !is Int
    if (notIsString(x) || notIsInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
}