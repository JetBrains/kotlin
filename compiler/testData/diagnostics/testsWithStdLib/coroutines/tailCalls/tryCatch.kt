// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER
// COMMON_COROUTINES_TEST
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun nonSuspend() {}

suspend fun baz(): Int = 1

suspend fun tryCatch(): Int {
    return try {
        suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> -> }
    } catch (e: Exception) {
        baz() // another suspend function
    }
}

suspend fun tryFinally(): Int {
    return try {
        suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> -> }
    } finally {
        nonSuspend()
    }
}

suspend fun returnInFinally(): Int {
    try {
    } finally {
        // Probably this is too restrictive, but it does not matter much
        return baz()
    }
}

suspend fun tryCatchFinally(): Int {
    return try {
        suspendCoroutineUninterceptedOrReturn { x: Continuation<Int> -> }
    } catch (e: Exception) {
        baz() // another suspend function
    } finally {
        baz()
    }
}
