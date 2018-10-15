// !LANGUAGE: +AllowContractsForCustomFunctions +UseCallsInPlaceEffect
// !USE_EXPERIMENTAL: kotlin.contracts.ExperimentalContracts
// !DIAGNOSTICS: -INVISIBLE_REFERENCE -INVISIBLE_MEMBER

import kotlin.contracts.*

inline fun <T> myRun(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

inline fun <T, R> T.myLet(block: (T) -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block(this)
}

inline fun unknownRun(block: () -> Unit) = block()

fun getBool(): Boolean = false

fun threeLevelsReturnNoInitialization(x: Int?): Int? {
    // Inner always jumps to outer
    // And middle always calls inner
    // So, in fact, middle never finished normally
    // Hence 'y = 54' in middle is unreachable, and middle doesn't performs definite initalization
    // Hence, outer doesn't performs definite initialization
    val y: Int
    myRun outer@ {
        myRun middle@ {
            x.myLet inner@ {
                if (it == null) {
                    y = 42
                    return@outer Unit
                }
                else {
                    return@outer Unit
                }
            }
        }
        // Possible to report unreachable here
        <!UNREACHABLE_CODE!>y = 54<!>
    }
    return <!UNINITIALIZED_VARIABLE!>y<!>.inc()
}

fun threeLevelsReturnWithInitialization(x: Int?): Int? {
    val y: Int
    myRun outer@ {
        myRun middle@ {
            x.myLet inner@ {
                if (it == null) {
                    y = 42
                    return@outer Unit
                }
                else {
                    y = 34
                    return@outer Unit
                }
            }
        }
    }
    return y.inc()
}

fun threeLevelsReturnWithUnknown(x: Int?): Int? {
    val y: Int
    myRun outer@ {
        unknownRun middle@ {
            x.myLet inner@ {
                if (it == null) {
                    <!CAPTURED_VAL_INITIALIZATION!>y<!> = 42
                    return@outer Unit
                }
                else {
                    y = 34
                    return@outer Unit
                }
            }
        }
    }
    return <!UNINITIALIZED_VARIABLE!>y<!>.inc()
}