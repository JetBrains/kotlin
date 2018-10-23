// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORIES: analysis, smartcasts
 NUMBER: 14
 DESCRIPTION: Check smartcast with non-null assertion for a contract function.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26856
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

fun case_1(value_1: Int?): Boolean? {
    contract {
        returns(true) implies (value_1 != null)
    }

    return value_1 != null
}

fun case_2(value_1: Int?): Boolean {
    contract {
        returns(false) implies (value_1 != null)
    }

    return value_1 != null
}

fun case_3(value_1: Int?): Boolean? {
    contract {
        returnsNotNull() implies (value_1 != null)
    }

    return value_1 != null
}

fun case_4(value_1: Any?): Boolean {
    contract {
        returnsNotNull() implies (value_1 is Number)
    }

    return value_1 is Number
}

// FILE: usages.kt

import contracts.*

fun case_1(value_1: Int?) {
    if (contracts.case_1(value_1)!!) {
        value_1<!UNSAFE_CALL!>.<!>inv()
    }
}

fun case_2(value_1: Int?) {
    if (!contracts.case_2(value_1)<!UNNECESSARY_NOT_NULL_ASSERTION!>!!<!>) {
        value_1<!UNSAFE_CALL!>.<!>inv()
    }
}

fun case_3(value_1: Int?) {
    if (contracts.case_3(value_1)!!) {
        value_1<!UNSAFE_CALL!>.<!>inv()
    }
}

fun case_4(value_1: Any?) {
    if (<!SENSELESS_COMPARISON!>contracts.case_4(value_1) != null<!>) {
        <!DEBUG_INFO_SMARTCAST!>value_1<!>.toByte()
    }
}
