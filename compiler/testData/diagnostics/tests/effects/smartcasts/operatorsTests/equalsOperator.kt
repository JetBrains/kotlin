// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE
// !LANGUAGE: +ContractEffects

import kotlin.internal.*

@Returns(ConstantValue.FALSE)
fun myEqualsNull(@Equals(ConstantValue.NOT_NULL) x: Int?) = x == null

@Returns(ConstantValue.TRUE)
fun myEqualsNotNull(@Equals(ConstantValue.NOT_NULL) x: Int?) = x != null

fun testBasicEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNull(x)) {
        x<!UNSAFE_CALL!>.<!>inc()
    } else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

fun testBasicNotEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNotNull(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    } else {
        x<!UNSAFE_CALL!>.<!>inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

