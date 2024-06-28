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

fun trueWhenInt(x: Any?): Boolean {
    contract {
        returns(true) implies (x is Int)
    }
    return x is Int
}

fun falseWhenString(x: Any?): Boolean {
    contract {
        returns(false) implies (x is String)
    }
    return x !is String
}

fun falseWhenInt(x: Any?): Boolean {
    contract {
        returns(false) implies (x is Int)
    }
    return x !is Int
}


// ==== Actual tests ====

fun truetrue(x: Any?) {
    if (trueWhenString(x) && trueWhenInt(x)) {
        x.length
        x.inc()
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.<!UNRESOLVED_REFERENCE!>inc<!>()
}

fun truefalse(x: Any?) {
    if (trueWhenString(x) && falseWhenInt(x)) {
        x.length
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun falsetrue(x: Any?) {
    if (falseWhenString(x) && trueWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.inc()
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun falsefalse(x: Any?) {
    if (falseWhenString(x) && falseWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
    else {
        // Note that we can't argue that we have any of smartcasts here,
        // because we don't know which one of both arguments was 'false' to bring us here
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!NONE_APPLICABLE!>inc<!>()
    }
}
