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

fun annotatedTrueOrTrue(x: Any?) {
    if (trueWhenString(x) || true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        // Unreachable
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedTrueOrFalse(x: Any?) {
    if (trueWhenString(x) || false) {
        x.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseOrTrue(x: Any?) {
    if (falseWhenString(x) || true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        // Unreachable
        x.length
    }
}

fun annotatedFalseOrFalse(x: Any?) {
    if (falseWhenString(x) || false) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        x.length
    }
}