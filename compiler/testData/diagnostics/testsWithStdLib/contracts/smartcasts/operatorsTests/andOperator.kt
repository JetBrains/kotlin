// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !WITH_NEW_INFERENCE

import kotlin.internal.contracts.*

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
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
    x.<!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
}

fun truefalse(x: Any?) {
    if (trueWhenString(x) && falseWhenInt(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        x.<!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
}

fun falsetrue(x: Any?) {
    if (falseWhenString(x) && trueWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
}

fun falsefalse(x: Any?) {
    if (falseWhenString(x) && falseWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
    else {
        // Note that we can't argue that we have any of smartcasts here,
        // because we don't know which one of both arguments was 'false' to bring us here
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!NI;NONE_APPLICABLE, OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>inc<!>()
    }
}