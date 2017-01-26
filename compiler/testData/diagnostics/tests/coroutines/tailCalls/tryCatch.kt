// !DIAGNOSTICS: -UNUSED_PARAMETER
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun nonSuspend() {}

suspend fun baz(): Int = 1

suspend fun tryCatch(): Int {
    return try {
        suspendCoroutineOrReturn { x: Continuation<Int> -> }
    } catch (e: Exception) {
        baz() // another suspend function
    }
}

suspend fun tryFinally(): Int {
    return try {
        suspendCoroutineOrReturn { x: Continuation<Int> -> }
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
        suspendCoroutineOrReturn { x: Continuation<Int> -> }
    } catch (e: Exception) {
        baz() // another suspend function
    } finally {
        baz()
    }
}
