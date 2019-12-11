// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun isString(x: Any?): Boolean {
    contract {
        returns(true) implies (x is String)
    }
    return x is String
}

fun exhaustive(x: Any?) {
    when {
        isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
        !isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when {
        !isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
        isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun smartcastInElse(x: Any?) {
    when {
        !isString(x) -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}