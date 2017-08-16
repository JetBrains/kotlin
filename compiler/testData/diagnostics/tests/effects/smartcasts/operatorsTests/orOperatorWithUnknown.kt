// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun trueWhenString(@IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.FALSE)
fun falseWhenString(@IsInstance(String::class) x: Any?) = x !is String

fun unknownFunction(x: Any?) = x == 42

fun annotatedTrue(x: Any?) {
    if (trueWhenString(x) || unknownFunction(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalse(x: Any?) {
    if (falseWhenString(x) || unknownFunction(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun annotatedTrueWithVariable(x: Any?, b: Boolean) {
    if (trueWhenString(x) || b) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun annotatedFalseWithVariable(x: Any?, b: Boolean) {
    if (falseWhenString(x) || b) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}