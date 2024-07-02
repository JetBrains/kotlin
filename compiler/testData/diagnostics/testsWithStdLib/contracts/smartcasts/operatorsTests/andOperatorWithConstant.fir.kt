// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

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




fun annotatedTrueAndTrue(x: Any?) {
    if (trueWhenString(x) && true) {
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedTrueAndFalse(x: Any?) {
    if (trueWhenString(x) && false) {
        // Unreachable
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseAndTrue(x: Any?) {
    if (falseWhenString(x) && true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.length
    }
}

fun annotatedFalseAndFalse(x: Any?) {
    if (falseWhenString(x) && false) {
        // Unreachable
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
