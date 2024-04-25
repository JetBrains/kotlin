// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun nonSuspend() {}

suspend fun foo() {
    suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> -> }

    nonSuspend()
}

suspend fun unitSuspend() {
    suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> -> }
}

suspend fun baz(): Int = run {
    suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> -> }
}
