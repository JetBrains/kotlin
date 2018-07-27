// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.internal.ContractsDsl

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

fun getBoolean(): Boolean = false

fun test() {
    val x: Int

    if (getBoolean())
        myRun {
            while (getBoolean()) {
                do {
                    myRun {
                        if (getBoolean()) {
                            x = 42
                        }
                        else {
                            x = 43
                        }
                    }
                    break
                } while (getBoolean())
                myRun { x.inc() }
                myRun { x = 42 }
                break
            }
            x = 42
        }
    else
        myRun {
            x = 42
        }

    x.inc()
}