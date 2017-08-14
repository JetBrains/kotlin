// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.TRUE)
fun isString(@IsInstance(String::class) x: Any?) = x is String

@Returns(ConstantValue.FALSE)
fun notIsString(@IsInstance(String::class) x: Any?) = x !is String

@Returns(ConstantValue.FALSE)
fun notIsInt(@IsInstance(Int::class) x: Any?) = x !is Int


fun implicitAlwaysFalse(x: Any?) {
    if (isString(x) && !isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    } else {
        x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun implicitAlwaysFalseSpilling(x: Any?) {
    if (isString(x) && !isString(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
    x.<!UNRESOLVED_REFERENCE!>length<!>
}