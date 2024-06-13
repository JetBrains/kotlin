// FIR_IDENTICAL
// LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// OPT_IN: kotlin.contracts.ExperimentalContracts
// DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun functionWithSideEffects(x: Int): Int = x + 1 // ...and some other useful side-effects

fun log(s: String) = Unit // some logging or println or whatever returning Unit

fun implicitCastWithIf(s: String) {
    myRun { if (s == "") functionWithSideEffects(42) else log(s) }
}
