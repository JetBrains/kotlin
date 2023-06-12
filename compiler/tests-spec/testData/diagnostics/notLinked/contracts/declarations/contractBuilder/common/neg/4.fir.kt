// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 4
 * DESCRIPTION: contracts with not allowed conditions with constants in implies.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returnsNotNull() implies (<!NULL_FOR_NONNULL_TYPE!>null<!>)<!> }
    return true
}

// TESTCASE NUMBER: 2
fun case_2(): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(false) implies <!ARGUMENT_TYPE_MISMATCH!>0.000001<!><!> }
    return true
}

// TESTCASE NUMBER: 3
fun case_3(): Boolean? {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(null) implies <!ARGUMENT_TYPE_MISMATCH!>""<!><!> }
    return null
}
