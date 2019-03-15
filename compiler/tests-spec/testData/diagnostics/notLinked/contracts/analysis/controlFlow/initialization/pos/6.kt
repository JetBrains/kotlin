// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, analysis, controlFlow, initialization
 * NUMBER: 6
 * DESCRIPTION: Check the lack of CallsInPlace effect on the lambda in the parentheses.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26229
 * HELPERS: contractFunctions
 */

// FILE: contracts.kt

package contracts

import kotlin.contracts.*

// TESTCASE NUMBER: 3
inline fun case_3(block1: () -> Unit, block2: () -> Unit, block3: () -> Unit) {
    contract {
        callsInPlace(block1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block2, InvocationKind.AT_LEAST_ONCE)
        callsInPlace(block3, InvocationKind.EXACTLY_ONCE)
    }
    block1()
    block2()
    block2()
    block3()
}

// FILE: main.kt

import contracts.*

// TESTCASE NUMBER: 1
fun case_1() {
    val value_1: Int
    funWithExactlyOnceCallsInPlace({ <!CAPTURED_VAL_INITIALIZATION!>value_1<!> = 11 })
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 2
fun case_2() {
    var value_1: Int
    funWithAtLeastOnceCallsInPlace({ value_1 = 11 })
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
}

// TESTCASE NUMBER: 3
fun case_3() {
    val value_1: Int
    var value_2: Int
    val value_3: Int
    contracts.case_3({ <!CAPTURED_VAL_INITIALIZATION!>value_1<!> = 1 }, { value_2 = 2 }, { <!CAPTURED_VAL_INITIALIZATION!>value_3<!> = 3 })
    <!UNINITIALIZED_VARIABLE!>value_1<!>.inc()
    <!UNINITIALIZED_VARIABLE!>value_2<!>.inc()
    <!UNINITIALIZED_VARIABLE!>value_3<!>.inc()
}
