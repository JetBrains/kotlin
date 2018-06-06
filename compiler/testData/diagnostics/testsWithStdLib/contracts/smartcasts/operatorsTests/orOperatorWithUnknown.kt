// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.internal.contracts.*

fun trueWhenString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

fun falseWhenString(x: Any?): Boolean {
    contract {
        returns(false) implies (x is String)
    }
    return x !is String
}

fun unknownFunction(x: Any?) = x == 42




fun annotatedTrue(x: Any?) {
    if (trueWhenString(x) || unknownFunction(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalse(x: Any?) {
    if (falseWhenString(x) || unknownFunction(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun annotatedTrueWithVariable(x: Any?, b: Boolean) {
    if (trueWhenString(x) || b) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseWithVariable(x: Any?, b: Boolean) {
    if (falseWhenString(x) || b) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}