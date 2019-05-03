// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -UNREACHABLE_CODE -UNUSED_EXPRESSION
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

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
    contract { returns(true) implies (<!ERROR_IN_CONTRACT_DESCRIPTION, TYPE_MISMATCH!>-10<!>) }
    return true
}

// TESTCASE NUMBER: 2
fun case_2(): Boolean {
    <!ERROR_IN_CONTRACT_DESCRIPTION!>contract<!> { returnsNotNull() implies (return return return true) }
    return true
}

// TESTCASE NUMBER: 3
fun case_3(): Boolean {
    contract { returns(false) implies (<!ERROR_IN_CONTRACT_DESCRIPTION, TYPE_MISMATCH!>"..." + "$<!UNRESOLVED_REFERENCE!>value_1<!>"<!>) }
    return true
}

/*
 * TESTCASE NUMBER: 4
 * ISSUES: KT-26386
 */
fun case_4(): Boolean? {
    contract { returns(null) implies <!ERROR_IN_CONTRACT_DESCRIPTION, TYPE_MISMATCH!>case_4()<!> }
    return null
}

// TESTCASE NUMBER: 5
fun case_5(): Boolean? {
    contract { returns(null) implies <!ERROR_IN_CONTRACT_DESCRIPTION, TYPE_INFERENCE_EXPECTED_TYPE_MISMATCH!>listOf(0)<!> }
    return null
}

// TESTCASE NUMBER: 6
fun case_6(value_1: Boolean): Boolean? {
    contract { returns(null) implies <!ERROR_IN_CONTRACT_DESCRIPTION, TYPE_MISMATCH!><!CONTRACT_NOT_ALLOWED, CONTRACT_NOT_ALLOWED!>contract<!> { returns(null) implies (!value_1) }<!> }
    return null
}

// TESTCASE NUMBER: 7
fun case_7(): Int {
    contract {
        callsInPlace(<!ERROR_IN_CONTRACT_DESCRIPTION!>::case_7<!>, InvocationKind.EXACTLY_ONCE)
    }
    return 1
}

/*
 * TESTCASE NUMBER: 8
 * ISSUES: KT-26386
 */
fun case_8(): () -> Unit {
    contract {
        callsInPlace(<!ERROR_IN_CONTRACT_DESCRIPTION!>case_8()<!>, InvocationKind.EXACTLY_ONCE)
    }
    return {}
}