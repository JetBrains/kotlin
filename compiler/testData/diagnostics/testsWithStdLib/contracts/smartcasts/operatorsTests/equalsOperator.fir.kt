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
    x.<!AMBIGUITY!>inc<!>()

    if (myEqualsNull(x)) {
        x.<!AMBIGUITY!>inc<!>()
    }
    else {
        x.<!AMBIGUITY!>inc<!>()
    }

    x.<!AMBIGUITY!>inc<!>()
}

fun testBasicNotEquals(x: Int?) {
    x.<!AMBIGUITY!>inc<!>()

    if (myEqualsNotNull(x)) {
        x.<!AMBIGUITY!>inc<!>()
    }
    else {
        x.<!AMBIGUITY!>inc<!>()
    }

    x.<!AMBIGUITY!>inc<!>()
}

