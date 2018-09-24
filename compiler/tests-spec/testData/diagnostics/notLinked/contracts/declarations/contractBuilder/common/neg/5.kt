// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)

 SECTION: contracts
 CATEGORY: declarations, contractBuilder, common
 NUMBER: 5
 DESCRIPTION: contracts with not allowed expressions in implies.
 */

import kotlin.contracts.*

fun case_1(): Boolean {
    contract { returns(true) implies (<!TYPE_MISMATCH, ERROR_IN_CONTRACT_DESCRIPTION!>-10<!>) }
    return true
}

fun case_2(): Boolean {
    contract { returnsNotNull() implies (return return <!RETURN_TYPE_MISMATCH!>return<!>) }
    return true
}

fun case_3(): Boolean {
    contract { returns(false) implies (<!TYPE_MISMATCH, ERROR_IN_CONTRACT_DESCRIPTION!>"..." + "$<!UNRESOLVED_REFERENCE!>value_1<!>"<!>) }
    return true
}

/*
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-25948
 */
fun case_4(): Boolean {
    contract { returns(null) implies throw Exception() }
    return true
}

/*
 UNEXPECTED BEHAVIOUR
 ISSUES: KT-26386
 */
fun case_5(): Boolean? {
    contract { returns(null) implies case_5() }
    return null
}

fun case_6(): Boolean? {
    contract { returns(null) implies <!TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH, ERROR_IN_CONTRACT_DESCRIPTION!>listOf(0)<!> }
    return null
}

fun case_7(value_1: Boolean): Boolean? {
    contract { returns(null) implies <!TYPE_MISMATCH, ERROR_IN_CONTRACT_DESCRIPTION!>contract { returns(null) implies (!value_1) }<!> }
    return null
}
