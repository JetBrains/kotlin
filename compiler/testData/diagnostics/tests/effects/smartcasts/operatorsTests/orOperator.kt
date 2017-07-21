// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun trueWhenString(@IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.TRUE)
fun trueWhenInt(@IsInstance(Int::class) x: Any?) = x is Int

@Returns(ConstantValue.FALSE)
fun falseWhenString(@IsInstance(String::class) x: Any?) = x !is String

@Returns(ConstantValue.FALSE)
fun falseWhenInt(@IsInstance(Int::class) x: Any?) = x !is Int

fun truetrue(x: Any?) {
    if (trueWhenString(x) || trueWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun truefalse(x: Any?) {
    if (trueWhenString(x) || falseWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
}

fun falsetrue(x: Any?) {
    if (falseWhenString(x) || trueWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun falsefalse(x: Any?) {
    if (falseWhenString(x) || falseWhenInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
}