// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 17
 * DESCRIPTION: contracts with Nothing expressions in implies.
 * DISCUSSION
 * ISSUES: KT-25948
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies throw Exception() }
    return true
}

// TESTCASE NUMBER: 2
fun case_2(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies return return return false }
    return true
}

// TESTCASE NUMBER: 3
fun case_3(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies return return return false && throw throw throw throw Exception() }
    return true
}
