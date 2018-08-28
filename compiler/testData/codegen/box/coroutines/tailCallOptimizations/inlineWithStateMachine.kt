// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
// CHECK_BYTECODE_LISTING
// CHECK_NEW_COUNT: function=suspendHere count=1
// CHECK_NEW_COUNT: function=mainSuspend count=1
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

inline suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume(v)
    COROUTINE_SUSPENDED
}

// There's no state machine in the suspendHere, since it's inline
inline suspend fun suspendHere(): String = suspendThere("O") + suspendThere("K")
// There should be a state machine for mainSuspend as it has two suspend non-tail calls inlined
suspend fun mainSuspend() = suspendHere()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = mainSuspend()
    }

    return result
}
