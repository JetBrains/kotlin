// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// CHECK_STATE_MACHINE

// FILE: inline.kt

import helpers.*

interface SuspendRunnable {
    suspend fun run()
}

inline fun inlineMe1(crossinline c: suspend () -> Unit) =
    object : SuspendRunnable {
        override suspend fun run() {
            c()
            c()
        }
    }

inline fun inlineMe2(crossinline c: suspend () -> Unit) = suspend {
    c()
    c()
}

inline suspend fun inlineMe3(crossinline c: suspend () -> Unit) {
    c()
    c()
}

inline suspend fun inlineMe4(c: suspend () -> Unit) {
    c()
    c()
}

inline fun inlineMe5(noinline c: suspend () -> Unit) =
    object : SuspendRunnable {
        override suspend fun run() {
            c()
            c()
        }
    }

inline fun inlineMe6(noinline c: suspend () -> Unit) = suspend {
    c()
    c()
}

inline suspend fun inlineMe7(noinline c: suspend () -> Unit) {
    c()
    c()
}

inline fun inlineMe11(crossinline c: suspend () -> Unit) = inlineMe1(c)
inline fun inlineMe12(crossinline c: suspend () -> Unit) = inlineMe2(c)
inline suspend fun inlineMe13(crossinline c: suspend () -> Unit) = inlineMe3(c)
inline suspend fun inlineMe14(crossinline c: suspend () -> Unit) = inlineMe4(c)

// FILE: box.kt
import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

fun box(): String {
    StateMachineChecker.reset()
    val lambda = suspend {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
    }

    builder {
        val r = inlineMe1(lambda)
        r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe2(lambda)()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe3(lambda)
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe4(lambda)
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        val r = inlineMe5(lambda)
        r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe6(lambda)()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe7(lambda)
    }
    StateMachineChecker.check(numberOfSuspensions = 4)

    StateMachineChecker.reset()
    builder {
        val r = inlineMe11 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
        r.run()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe12 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }()
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe13 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 4)
    StateMachineChecker.reset()
    builder {
        inlineMe14 {
            StateMachineChecker.suspendHere()
            StateMachineChecker.suspendHere()
        }
    }
    StateMachineChecker.check(numberOfSuspensions = 4)

    return "OK"
}
