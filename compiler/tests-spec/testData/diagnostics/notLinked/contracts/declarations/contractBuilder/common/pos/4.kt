// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORIES: declarations, contractBuilder, common
 NUMBER: 4
 DESCRIPTION: contracts with not allowed conditions with boolean constants or constant expressions in implies.
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26491
 */

import kotlin.contracts.*

fun case_1(): Boolean {
    contract { returns(true) implies true }
    return true
}

fun case_2(): Boolean {
    contract { returns(true) implies (true || false) }
    return true || false
}