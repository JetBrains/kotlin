// !LANGUAGE: +AllowContractsForCustomFunctions +UseReturnsEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun safeIsString(x: Any?): Boolean? {
    contract {
        returns(true) implies (x is String)
    }
    return x?.let { it is String }
}

fun elseWithNullableResult(x: Any?) {
    when (safeIsString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        null -> x.<!UNRESOLVED_REFERENCE!>length<!>
        else -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }
}

fun exhaustiveWithNullableResult(x: Any?) {
    when (safeIsString(x)) {
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        null -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
        null -> x.<!UNRESOLVED_REFERENCE!>length<!>
    }

    when (safeIsString(x)) {
        false -> x.<!UNRESOLVED_REFERENCE!>length<!>
        null -> x.<!UNRESOLVED_REFERENCE!>length<!>
        true -> <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}