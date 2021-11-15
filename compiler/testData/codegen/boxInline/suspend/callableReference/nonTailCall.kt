// WITH_STDLIB
// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// FILE: 1.kt
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.*

class Delayed(val run: suspend() -> Unit)

inline fun asyncThenAddK(crossinline block: suspend () -> Unit) =
    Delayed {
        block()
        result += "K"
    }

suspend fun pause(): Unit = suspendCoroutineUninterceptedOrReturn { cont ->
    cont.resume(Unit)
    COROUTINE_SUSPENDED
}

var result = ""

suspend fun addO(): Unit {
    pause()
    result += "O"
}

// FILE: 2.kt
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.*
import helpers.*

fun box(): String {
    suspend { asyncThenAddK(::addO).run() }.startCoroutine(EmptyContinuation)
    return result
}
