// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> inPlace(block: () -> T): T {
    contract {
        callsInPlace(block)
    }
    return block()
}

fun reassignmentAndNoInitializaiton() {
    val x: Int
    inPlace { <!VAL_REASSIGNMENT!>x<!> = 42 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}