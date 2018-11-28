// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

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
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returnsNotNull() implies (<!NULL_FOR_NONNULL_TYPE!>null<!>) }
    return true
}

// TESTCASE NUMBER: 2
fun case_2(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(false) implies <!CONSTANT_EXPECTED_TYPE_MISMATCH!>0.000001<!> }
    return true
}

// TESTCASE NUMBER: 3
fun case_3(): Boolean? {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies <!TYPE_MISMATCH!>""<!> }
    return null
}
