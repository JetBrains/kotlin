// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

fun <T> inPlace(block: () -> T): T {
    contract {
        <!INAPPLICABLE_CANDIDATE!>callsInPlace<!>(block)
    }
    return block()
}

fun reassignmentAndNoInitializaiton() {
    val x: Int
    inPlace { x = 42 }
    x.inc()
}