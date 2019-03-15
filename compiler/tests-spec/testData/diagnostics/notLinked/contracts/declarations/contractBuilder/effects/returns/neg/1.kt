// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 1
 * DESCRIPTION: Using equality with expressions in implies.
 * ISSUES: KT-26178
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?): Boolean {
    contract { returns(true) implies (x == <!ERROR_IN_CONTRACT_DESCRIPTION!>-.15f<!>) }
    return x !is Number
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?): Boolean {
    contract { returns(true) implies (x == <!ERROR_IN_CONTRACT_DESCRIPTION!>"..." + "."<!>) }
    return x !is Number
}

// TESTCASE NUMBER: 3
fun case_3(x: Int, y: Int): Boolean {
    contract { returns(true) implies (<!ERROR_IN_CONTRACT_DESCRIPTION!>x > y<!>) }
    return x > y
}

// TESTCASE NUMBER: 4
fun case_4(x: Any?, y: Any?): Boolean {
    contract { returns(true) implies (x == <!ERROR_IN_CONTRACT_DESCRIPTION!>y.toString()<!>) }
    return x !is Number
}
