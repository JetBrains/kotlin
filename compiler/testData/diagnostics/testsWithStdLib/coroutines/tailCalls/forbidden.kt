// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
