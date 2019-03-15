// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun reassignmentInUsualFlow() {
    val x: Int
    myRun { x = 42 }
    <!VAL_REASSIGNMENT!>x<!> = 43
    x.inc()
}

fun reassignment() {
    val x = 42
    myRun {
        <!VAL_REASSIGNMENT!>x<!> = 43
    }
    x.inc()
}

