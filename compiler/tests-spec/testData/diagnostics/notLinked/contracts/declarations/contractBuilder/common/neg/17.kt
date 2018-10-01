// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORIES: declarations, contractBuilder, common
 NUMBER: 17
 DESCRIPTION: contracts with Nothing expressions in implies.
 DISCUSSION
 ISSUES: KT-25948
 */

import kotlin.contracts.*

fun case_1(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies throw Exception() }
    return true
}

fun case_2(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies return return return false }
    return true
}

fun case_3(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returns(null) implies return return return false && throw throw throw throw Exception() }
    return true
}
