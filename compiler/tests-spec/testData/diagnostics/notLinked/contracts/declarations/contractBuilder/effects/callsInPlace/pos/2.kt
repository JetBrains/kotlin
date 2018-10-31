// !DIAGNOSTICS: -UNUSED_VARIABLE
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, callsInPlace
 * NUMBER: 2
 * DESCRIPTION: Contract with 'this' in first parameter of CallsInPlace.
 * DISCUSSION
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26294
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun <T : Function0<*>> T.case_1(block: () -> Unit) {
    contract {
        callsInPlace(this@case_1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    this@case_1()
}
