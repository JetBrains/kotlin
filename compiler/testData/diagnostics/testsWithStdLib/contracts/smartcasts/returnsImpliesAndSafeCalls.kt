// ISSUE: KT-59689
// FIR_IDENTICAL
// DIAGNOSTICS: -DEBUG_INFO_SMARTCAST
// OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class A {
    fun returnsTrue(x: Any?): Boolean {
        contract { returns(true) implies (x is String) }
        return x is String
    }

    fun returnsFalse(x: Any?): Boolean {
        contract { returns(false) implies (x is String) }
        return x !is String
    }

    fun returnsNotNull(x: Any?): Any? {
        contract { returnsNotNull() implies (x is String) }
        return when (x) {
            is String -> ""
            else -> null
        }
    }

    fun returnsNull(x: Any?): Any? {
        contract { returns(null) implies (x is String) }
        return when (x) {
            is String -> null
            else -> ""
        }
    }
}

fun test_returnsTrue(a: A?, s: Any?, b: Boolean) {
    if (a?.returnsTrue(s) == true) {
        s.length
    }
    if (a?.returnsTrue(s) == true && b) {
        s.length
    }
}

fun test_returnsFalse(a: A?, s: Any?, b: Boolean) {
    if (a?.returnsFalse(s) == false) {
        s.length
    }
    if (a?.returnsFalse(s) == false && b) {
        s.length
    }
}

fun test_returnsNotNull(a: A?, s: Any?, b: Boolean) {
    if (a?.returnsNotNull(s) != null) {
        s.length
    }
    if (a?.returnsNotNull(s) != null && b) {
        s.length
    }
}

fun test_returnsNull(a: A?, s: Any?, b: Boolean) {
    if (a?.returnsNull(s) == null) {
        s.<!UNRESOLVED_REFERENCE!>length<!> // should be an error
    }
    if (a?.returnsNull(s) == null && b) {
        s.<!UNRESOLVED_REFERENCE!>length<!> // should be an error
    }
}
