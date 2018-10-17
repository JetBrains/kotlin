// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

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
    if (trueWhenString(x) && unknownFunction(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalse(x: Any?) {
    if (falseWhenString(x) && unknownFunction(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedTrueWithVariable(x: Any?, b: Boolean) {
    if (trueWhenString(x) && b) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseWithVariable(x: Any?, b: Boolean) {
    if (falseWhenString(x) && b) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
