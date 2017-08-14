// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.FALSE)
fun notIsString(@IsInstance(String::class) x: Any?) = x !is String

@Returns(ConstantValue.FALSE)
fun notIsInt(@IsInstance(Int::class) x: Any?) = x !is Int

fun testDeMorgan(x: Any?) {
       // !(x !is String || x !is Int)
       // x is String && x is Int
    if (!(notIsString(x) || notIsInt(x))) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    }
}

fun testDeMorgan2(x: Any?) {
        // x !is String || x !is Int
    if (notIsString(x) || notIsInt(x)) {
        x.<!UNRESOLVED_REFERENCE!>length<!>
        x.<!UNRESOLVED_REFERENCE!>inc<!>()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
}