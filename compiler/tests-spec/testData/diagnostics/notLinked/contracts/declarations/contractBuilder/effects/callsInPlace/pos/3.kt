// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE -NO_REFLECTION_IN_CLASS_PATH
// !OPT_IN: kotlin.contracts.ExperimentalContracts

/*
 * KOTLIN DIAGNOSTICS NOT LINKED SPEC TEST (POSITIVE)
 *
 * SECTIONS: contracts, declarations, contractBuilder, effects, callsInPlace
 * NUMBER: 3
 * DESCRIPTION: Contract with 'this' in first parameter of CallsInPlace.
 * UNEXPECTED BEHAVIOUR
 * ISSUES: KT-26294
 */

import kotlin.contracts.*

// TESTCASE NUMBER: 1
inline fun <T : Function<*>> T.case_1(block: () -> Unit) {
    contract {
        callsInPlace(this@case_1, InvocationKind.EXACTLY_ONCE)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    (this@case_1 as kotlin.reflect.KFunction<*>).call()
}
