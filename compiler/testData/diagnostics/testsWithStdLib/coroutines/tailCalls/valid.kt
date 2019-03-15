// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun baz() = 1

suspend fun foo() {}

suspend fun bar0() = baz()
suspend fun bar01(): Int {
    return baz()
}

suspend fun bar1() {
    return if (1.hashCode() > 0) {
        foo()
    }
    else suspendCoroutineUninterceptedOrReturn { x: Continuation<Unit> -> }
}

suspend fun bar2() =
        if (1.hashCode() > 0) {
            foo()
        }
        else suspendCoroutineUninterceptedOrReturn { x: Continuation<Unit> -> }

suspend fun bar3() =
        when {
            true -> { foo() }
            else -> suspendCoroutineUninterceptedOrReturn { x: Continuation<Unit> -> }
        }
