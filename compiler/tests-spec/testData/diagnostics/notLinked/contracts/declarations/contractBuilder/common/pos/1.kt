// FIR_IDENTICAL
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 1
 * DESCRIPTION: Functions with simple contracts.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(value_1: Int?, block: () -> Unit): Boolean {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returns(true) implies (value_1 != null)
    }
    block()
    return value_1 != null
}

// TESTCASE NUMBER: 3
inline fun <T> T?.case_3(value_1: Int?, value_2: Boolean, value_3: Int?, block: () -> Unit): Boolean? {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        returns(true) implies (value_1 != null)
        returns(false) implies (!value_2)
        returnsNotNull() implies (this@case_3 != null && value_3 != null)
    }
    block()
    return value_1 != null
}
