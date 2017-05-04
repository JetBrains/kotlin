// IGNORE_BACKEND: JS
// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
// CHECK_BYTECODE_LISTING
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

inline suspend fun suspendThere(v: String): String = suspendCoroutineOrReturn { x ->
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
