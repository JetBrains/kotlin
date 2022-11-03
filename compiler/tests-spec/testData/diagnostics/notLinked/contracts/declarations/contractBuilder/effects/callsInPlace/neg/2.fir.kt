// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

// TESTCASE NUMBER: 2
inline fun case_2(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
    return block()
}
