// !DIAGNOSTICS: -UNUSED_VARIABLE
// SKIP_TXT
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

// Function is NOT suspend
// parameter is inline
// parameter is NOT suspend
// Block is allowed to be called inside the body of owner inline function
// Block is NOT allowed to be called from nested classes/lambdas (as common crossinlines)
// It is NOT possible to call startCoroutine on the parameter
// suspend calls possible inside lambda matching to the parameter

inline fun test(c: () -> Unit) {
    c()
    val o = object : Runnable {
        override fun run() {
            c()
        }
    }
    val l = { c() }
    c.startCoroutine(EmptyContinuation)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() {
    builder {
        test {
            calculate()
        }
    }
}
