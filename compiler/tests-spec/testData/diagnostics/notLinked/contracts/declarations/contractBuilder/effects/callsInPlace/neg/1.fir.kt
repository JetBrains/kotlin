// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, callsInPlace
 * NUMBER: 1
 * DESCRIPTION: contract functions with CallsInPlace with dynamic InvocationKind.
 * ISSUES: KT-26152
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1, 2, 3, 4
val invocationKind: InvocationKind = InvocationKind.EXACTLY_ONCE

object SampleObject {
    val invocationKind = InvocationKind.EXACTLY_ONCE
}

// TESTCASE NUMBER: 1
inline fun case_1(invocationKind: InvocationKind, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, invocationKind)<!> }
    return block()
}

// TESTCASE NUMBER: 2
inline fun <T : InvocationKind> case_2(invocationKind: T, block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, invocationKind)<!> }
    return block()
}

// TESTCASE NUMBER: 3
inline fun case_3(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, invocationKind)<!> }
    return block()
}

// TESTCASE NUMBER: 4
inline fun case_4(block: () -> Unit) {
    contract { <!ERROR_IN_CONTRACT_DESCRIPTION!>callsInPlace(block, SampleObject.invocationKind)<!> }
    return block()
}
