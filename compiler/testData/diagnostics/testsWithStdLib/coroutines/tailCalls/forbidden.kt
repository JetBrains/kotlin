// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

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
