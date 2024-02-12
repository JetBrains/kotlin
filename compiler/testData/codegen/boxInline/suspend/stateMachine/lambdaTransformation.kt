// CHECK_STATE_MACHINE
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// FILE: test.kt

import helpers.*
import kotlin.coroutines.*

const val DEBUG = false
inline fun inlineFun(b: () -> Unit) {
    if (DEBUG) {
        inlineFunReal(b)
    }
}

inline fun inlineFunReal(b: () -> Unit) {
    try {
        b()
    } finally {
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

// FILE: box.kt
import helpers.*

class Sample {
    fun test() {
        inlineFun {
            builder {
                inlineFun {
                    suspendFun()
                    suspendFun()
                }
            }
        }
    }

    suspend fun suspendFun() = StateMachineChecker.suspendHere()
}

fun box(): String {
    StateMachineChecker.reset()
    Sample().test()
    StateMachineChecker.check(0, checkFinished = false)
    return "OK"
}
