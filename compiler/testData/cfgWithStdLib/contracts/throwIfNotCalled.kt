// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts

import kotlin.contracts.*

inline fun myRun(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

inline fun <T> unknownRun(block: () -> T): T = block()

fun throwIfNotCalled() {
    val x: Int
    myRun outer@ {
        unknownRun {
            myRun {
                x = 42
                return@outer
            }
        }
        throw java.lang.IllegalArgumentException()
    }
    println(<!UNINITIALIZED_VARIABLE!>x<!>)
}