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
    x.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (myEqualsNull(x)) {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }
    else {
        x.inc()
    }

    x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

fun testBasicNotEquals(x: Int?) {
    x.<!INAPPLICABLE_CANDIDATE!>inc<!>()

    if (myEqualsNotNull(x)) {
        x.inc()
    }
    else {
        x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
    }

    x.<!INAPPLICABLE_CANDIDATE!>inc<!>()
}

