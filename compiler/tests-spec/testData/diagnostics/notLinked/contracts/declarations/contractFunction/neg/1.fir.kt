// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }<!>
    block()
    case_1(block)
}
