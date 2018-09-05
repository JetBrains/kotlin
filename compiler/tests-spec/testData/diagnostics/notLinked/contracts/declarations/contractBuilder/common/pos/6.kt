// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 6
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