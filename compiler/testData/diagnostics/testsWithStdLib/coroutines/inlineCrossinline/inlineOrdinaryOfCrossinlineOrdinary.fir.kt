// !DIAGNOSTICS: -UNUSED_VARIABLE
// WITH_COROUTINES
// SKIP_TXT
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

// Function is NOT suspend
// parameter is crossinline
// parameter is NOT suspend
// Block is allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// It is NOT possible to call startCoroutine on the parameter
// suspend calls NOT possible inside lambda matching to the parameter

inline fun test(crossinline c: () -> Unit) {
    c()
    val o = object: Runnable {
        override fun run() {
            c()
        }
    }
    val l = { c() }
    c.<!USAGE_IS_NOT_INLINABLE!>startCoroutine<!>(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() {
    test {
        calculate()
    }
}
