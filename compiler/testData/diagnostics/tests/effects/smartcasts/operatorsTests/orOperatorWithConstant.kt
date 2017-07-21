// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun trueWhenString(@IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.FALSE)
fun falseWhenString(@IsInstance(String::class) x: Any?) = x !is String

fun annotatedTrueOrTrue(x: Any?) {
    if (trueWhenString(x) || true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        // Unreachable
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedTrueOrFalse(x: Any?) {
    if (trueWhenString(x) || false) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseOrTrue(x: Any?) {
    if (falseWhenString(x) || true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        // Unreachable
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseOrFalse(x: Any?) {
    if (falseWhenString(x) || false) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}