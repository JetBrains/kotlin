// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, initialization
 * NUMBER: 5
 * DESCRIPTION: Smart initialization with correspond contract function with default value before lambda.
 * ISSUES: KT-26444
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1(x: Double = 1.0, block: () -> Unit): Double {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return x
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    contracts.case_1 { value_1 = 10 }
    value_1.inc()
}
