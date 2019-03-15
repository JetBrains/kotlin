// IGNORE_BACKEND: JVM_IR
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
// TREAT_AS_ONE_FILE

import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var res = "FAIL"

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    res = "OK"
    it.resume(Unit)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    builder {
        suspendHere()
    }
    return res
}

// When we generate code of suspend function returning unit call
// we surround it with return unit markers.
// While denerating state machine, we remove them.
// 0 ICONST_2
// 0 ICONST_3
