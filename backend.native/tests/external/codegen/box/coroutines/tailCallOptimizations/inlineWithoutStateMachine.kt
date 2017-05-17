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

suspend fun suspendHere(): String = suspendThere("O")

// There is a kind of redundant state machine generated for complexSuspend:
// it's basically has the only suspend call just before return, but there is
// a redundant CHECKCAST String in the run's lambda, so we have to insert the state machine.
// TODO: Think of avoiding such redundant casts
suspend fun complexSuspend(): String {
    return run {
        suspendThere("K")
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere() + complexSuspend()
    }

    return result
}
