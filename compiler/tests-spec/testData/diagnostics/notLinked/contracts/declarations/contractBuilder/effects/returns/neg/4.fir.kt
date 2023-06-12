// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, returns
 * NUMBER: 4
 * DESCRIPTION: Using equality with literals in implies.
 * ISSUES: KT-26178
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Any?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x == .15f)<!> }
    return x == .15f
}

// TESTCASE NUMBER: 2
fun case_2(x: Any?) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns() implies (x == "...")<!> }
    if (x != "...") throw Exception()
}

// TESTCASE NUMBER: 3
fun case_3(x: Any?): Boolean {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>returns(true) implies (x == '-')<!> }
    return x == '-'
}
