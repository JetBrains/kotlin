// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (NEGATIVE)
 *
 * SECTIONS: contracts, declarations, contractFunction
 * NUMBER: 1
 * DESCRIPTION: Check that recursion isn't allowed in contract functions with CallsInPlace effect.
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun case_1(block: () -> Unit) {
    <!WRONG_INVOCATION_KIND!>contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }<!>
    block()
    <!RECURSION_IN_INLINE!>case_1<!>(block)
}
