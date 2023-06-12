// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts
 * NUMBER: 15
 * DESCRIPTION: Check smartcasts working if type checking for contract function is used
 * ISSUES: KT-27241
 * HELPERS: contractFunctions
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1_1(cond: Boolean): Any {
    contract { returns(true) implies cond }
    return cond
}
fun case_1_2(value: Any): Boolean {
    contract { returns(true) implies (value is Boolean) }
    return value is Boolean
}

// TESTCASE NUMBER: 2
fun case_2(cond: Boolean): Any {
    contract { returns(true) implies cond }
    return cond
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value: Any) {
    if (contracts.case_1_2(contracts.case_1_1(value is Char))) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(value.<!UNRESOLVED_REFERENCE!>category<!>)
    }
}

// TESTCASE NUMBER: 2
fun case_2(value: Any) {
    if (contracts.case_2(value is Char) is Boolean) {
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(value.<!UNRESOLVED_REFERENCE!>category<!>)
    }
}

// TESTCASE NUMBER: 3
fun case_3(value: String?) {
    if (<!USELESS_IS_CHECK!>!value.isNullOrEmpty() is Boolean<!>) {
        value<!UNSAFE_CALL!>.<!>length
    }
}
