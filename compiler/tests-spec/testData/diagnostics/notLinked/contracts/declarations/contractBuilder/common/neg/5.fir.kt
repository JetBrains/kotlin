// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 5
 * DESCRIPTION: contracts with not allowed expressions in implies.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (<!ARGUMENT_TYPE_MISMATCH!>-10<!>)<!> }
    return true
}

// TESTCASE NUMBER: 2
fun case_2(): Boolean {
    contract { returnsNotNull() implies (return return return true) }
    return true
}

// TESTCASE NUMBER: 3
fun case_3(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies (<!ARGUMENT_TYPE_MISMATCH!>"..." + "$<!UNRESOLVED_REFERENCE!>value_1<!>"<!>)<!> }
    return true
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-26386
 */
fun case_4(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies <!ARGUMENT_TYPE_MISMATCH!>case_4()<!><!> }
    return null
}

// TESTCASE NUMBER: 5
fun case_5(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies <!ARGUMENT_TYPE_MISMATCH!>listOf(0)<!><!> }
    return null
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Boolean): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies <!ARGUMENT_TYPE_MISMATCH!><!CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies (!value_1) }<!><!> }
    return null
}

// TESTCASE NUMBER: 7
fun case_7(): Int {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(::case_7, InvocationKind.EXACTLY_ONCE)<!>
    }
    return 1
}

/*
 * TESTCASE NUMBER: 8
 * ISSUES: KT-26386
 */
fun case_8(): () -> Unit {
    contract {
        <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(case_8(), InvocationKind.EXACTLY_ONCE)<!>
    }
    return {}
}
