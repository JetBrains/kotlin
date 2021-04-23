// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }<!>
    block()
    <!RECURSION_IN_INLINE!>case_1<!>(block)
}
