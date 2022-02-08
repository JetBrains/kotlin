// !OPT_IN: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    contract test@ {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}
