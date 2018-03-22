// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*
// TREAT_AS_ONE_FILE

var res = "FAIL"

suspend fun suspendHere() = suspendCoroutineOrReturn<Unit> {
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