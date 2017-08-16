// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.NULL)
fun nullWhenString(@IsInstance(String::class) x: Any?) = if (x is String) null else 42

fun testNullWhenString(x: Any?) {
    if (nullWhenString(x) == null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun testNullWhenStringInversed(x: Any?) {
    if (nullWhenString(x) != null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

@Returns(ConstantValue.NOT_NULL)
fun nullWhenNotString(@IsInstance(String::class) x: Any?) = if (x !is String) null else 42

fun testNullNotWhenString(x: Any?) {
    if (nullWhenNotString(x) == null) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

fun testNullWhenNotString(x: Any?) {
    if (nullWhenNotString(x) != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}