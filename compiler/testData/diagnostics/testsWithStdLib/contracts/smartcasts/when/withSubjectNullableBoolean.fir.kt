// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun safeIsString(x: Any?): Boolean? {
    <!WRONG_IMPLIES_CONDITION!>contract {
        returns(true) implies (x is String)
    }<!>
    return x?.let { it is String }
}

fun elseWithNullableResult(x: Any?) {
    when (safeIsString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.length
    }

    when (safeIsString(x)) {
        true -> x.length
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        true -> x.length
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        true -> x.length
        null -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun exhaustiveWithNullableResult(x: Any?) {
    when (safeIsString(x)) {
        true -> x.length
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        null -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        true -> x.length
        null -> x.length
    }

    when (safeIsString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        null -> x.length
        true -> x.length
    }
}