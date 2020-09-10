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
        x.<!INAPPLICABLE_CANDIDATE!>dec<!>()
    }
    else {
        x.dec()
    }

    if (nullWhenNull(x) != null) {
        x.dec()
    }
    else {
        x.<!INAPPLICABLE_CANDIDATE!>dec<!>()
    }

    x.<!INAPPLICABLE_CANDIDATE!>dec<!>()
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
        x == null
    }
    else {
        x.<!INAPPLICABLE_CANDIDATE!>dec<!>()
    }

    if (notNullWhenNotNull(x) != null) {
        x.<!INAPPLICABLE_CANDIDATE!>dec<!>()
    }
    else {
        x == null
    }

    x.<!INAPPLICABLE_CANDIDATE!>dec<!>()
}
