// LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

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
        x.inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

fun testBasicNotEquals(x: Int?) {
    x<!UNSAFE_CALL!>.<!>inc()

    if (myEqualsNotNull(x)) {
        x.inc()
    }
    else {
        x<!UNSAFE_CALL!>.<!>inc()
    }

    x<!UNSAFE_CALL!>.<!>inc()
}

