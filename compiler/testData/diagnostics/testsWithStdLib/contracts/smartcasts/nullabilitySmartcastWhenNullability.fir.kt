// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !OPT_IN: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

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
        x.dec()
    }

    if (nullWhenNull(x) != null) {
        x.dec()
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
        <!SENSELESS_COMPARISON!>x == null<!>
    }
    else {
        x<!UNSAFE_CALL!>.<!>dec()
    }

    if (notNullWhenNotNull(x) != null) {
        x<!UNSAFE_CALL!>.<!>dec()
    }
    else {
        <!SENSELESS_COMPARISON!>x == null<!>
    }

    x<!UNSAFE_CALL!>.<!>dec()
}
