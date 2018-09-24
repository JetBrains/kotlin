// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER
// !WITH_NEW_INFERENCE

import kotlin.contracts.*

fun nullWhenNull(x: Int?): Int? {
    contract {
        returnsNotNull() implies (x != null)
    }
    return x?.inc()
}

fun testNullWhenNull(x: Int?) {
    if (nullWhenNull(x) == null) {
        x<!UNSAFE_CALL!>.<!>dec()
    }
    else {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }

    if (nullWhenNull(x) != null) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.dec()
    }
    else {
        x<!UNSAFE_CALL!>.<!>dec()
    }

    x<!UNSAFE_CALL!>.<!>dec()
}

// NB. it is the same function as `nullWhenNull`, but annotations specifies other facet of the function behaviour
fun notNullWhenNotNull (x: Int?): Int? {
    contract {
        returns(null) implies (x == null)
    }
    return x?.inc()
}

fun testNotNullWhenNotNull (x: Int?) {
    if (notNullWhenNotNull(x) == null) {
        <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>
    }
    else {
        x<!UNSAFE_CALL!>.<!>dec()
    }

    if (notNullWhenNotNull(x) != null) {
        x<!UNSAFE_CALL!>.<!>dec()
    }
    else {
        <!SENSELESS_COMPARISON!><!DEBUG_INFO_CONSTANT!>x<!> == null<!>
    }

    x<!UNSAFE_CALL!>.<!>dec()
}