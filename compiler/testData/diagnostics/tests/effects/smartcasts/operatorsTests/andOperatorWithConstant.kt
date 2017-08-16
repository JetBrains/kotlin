// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun trueWhenString(@IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.FALSE)
fun falseWhenString(@IsInstance(String::class) x: Any?) = x !is String

fun annotatedTrueAndTrue(x: Any?) {
    if (trueWhenString(x) && true) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedTrueAndFalse(x: Any?) {
    if (trueWhenString(x) && false) {
        // Unreachable
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseAndTrue(x: Any?) {
    if (falseWhenString(x) && true) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun annotatedFalseAndFalse(x: Any?) {
    if (falseWhenString(x) && false) {
        // Unreachable
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}
