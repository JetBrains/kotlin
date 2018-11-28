// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractFunction
 * NUMBER: 1
 * DESCRIPTION: Use a contract function before the declaration it.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
fun case_1_1(x: Any?) {
    if (case_1_2(x)) {
        <!DEBUG_INFO_SMARTCAST!>x<!>.length
    }
}

// TESTCASE NUMBER: 2
fun case_2_1(x: Number?) {
    case_2_2(x)
    println(<!DEBUG_INFO_SMARTCAST!>x<!>.toByte())
}

// TESTCASE NUMBER: 1
fun case_1_2(x: Any?): Boolean {
    contract { returns(true) implies (x is String) }
    return x is String
}

// TESTCASE NUMBER: 2
fun case_2_2(x: Any?) {
    contract { returns() implies(x != null) }
    if (x == null) throw Exception()
}


