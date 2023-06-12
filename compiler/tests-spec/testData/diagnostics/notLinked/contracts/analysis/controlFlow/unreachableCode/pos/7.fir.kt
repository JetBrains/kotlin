// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, unreachableCode
 * NUMBER: 7
 * DESCRIPTION: Smart initialization with correspond contract function with default value before lambda.
 * ISSUES: KT-26444
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Double = 1.0, block: () -> Unit): Double {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }<!>
    return x
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1() {
    contracts.case_1 { throw Exception() }
    println(1)
}
