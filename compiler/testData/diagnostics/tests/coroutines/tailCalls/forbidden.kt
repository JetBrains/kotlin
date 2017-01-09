// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun nonSuspend() {}

suspend fun foo() {
    suspendCoroutineOrReturn { x: Continuation<Int> -> }

    nonSuspend()
}

suspend fun unitSuspend() {
    suspendCoroutineOrReturn { x: Continuation<Int> -> }
}

suspend fun baz(): Int = run {
    suspendCoroutineOrReturn { x: Continuation<Int> -> }
}
