// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, callsInPlace
 * NUMBER: 1
 * DESCRIPTION: contract functions with CallsInPlace effects with different invocation kinds.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_LEAST_ONCE) }
    return block()
}

// TESTCASE NUMBER: 4
inline fun case_4(block: () -> Unit) {
    contract { callsInPlace(block, InvocationKind.UNKNOWN) }
    return block()
}
