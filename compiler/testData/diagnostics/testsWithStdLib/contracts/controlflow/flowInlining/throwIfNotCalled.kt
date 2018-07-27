// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

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
                <!CAPTURED_VAL_INITIALIZATION!>x<!> = 42
                return@outer
            }
        }
        throw java.lang.IllegalArgumentException()
    }
    // x *is* initialized here, because if myRun was never called -> exception
    // were thrown and control flow wouldn't be here
    println(x)
}

fun catchThrowIfNotCalled() {
    val x: Int
    try {
        myRun outer@ {
            unknownRun {
                myRun {
                    <!CAPTURED_VAL_INITIALIZATION!>x<!> = 42
                    return@outer
                }
            }
            throw java.lang.IllegalArgumentException()
        }
    } catch (ignored: java.lang.IllegalArgumentException) { }

    // x *isn't* initialized here!
    println(<!UNINITIALIZED_VARIABLE!>x<!>)
}