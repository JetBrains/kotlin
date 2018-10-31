// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, common
 * NUMBER: 2
 * DESCRIPTION: Functions with contract and builder lambda in parentheses.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract({ callsInPlace(block, InvocationKind.EXACTLY_ONCE) })
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract(builder = { callsInPlace(block, InvocationKind.EXACTLY_ONCE) })
    return block()
}
