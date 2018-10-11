// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !WITH_NEW_INFERENCE

import kotlin.contracts.*

fun myEqualsNull(x: Int?): Boolean {
    contract {
        returns(false) implies (x != null)
    }
    return x == null
}

fun myEqualsNotNull(x: Int?): Boolean {
    contract {
        returns(true) implies (x != null)
    }
    return x != null
}

fun testBasicEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNull(x)) {
        x<!UNSAFE_CALL!>.<!>inc()
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

fun testBasicNotEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNotNull(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.inc()
    }
    else {
        x<!UNSAFE_CALL!>.<!>inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

