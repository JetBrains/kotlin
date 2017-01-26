// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

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
