// !DIAGNOSTICS: -UNUSED_PARAMETER
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, smartcasts
 * NUMBER: 10
 * DESCRIPTION: Smartcasts with correspond contract function with default value in last parameter.
 * ISSUES: KT-26444
 * HELPERS: contractFunctions
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Int?, value_2: Int? = 10): Boolean {
    contract { returns(true) implies (value_1 != null) }
    return value_1 != null
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int? = 10, value_2: Int? = 10, value_3: Int? = 10): Boolean {
    contract { returns(true) implies (value_2 != null) }
    return value_1 != null
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1(value_1: Int?) {
    if (contracts.case_1(value_1)) {
        value_1.inc()
    }
}

// TESTCASE NUMBER: 2
fun case_2(value_1: Int?) {
    if (contracts.case_2(10, value_1)) {
        value_1.inc()
    }
}
