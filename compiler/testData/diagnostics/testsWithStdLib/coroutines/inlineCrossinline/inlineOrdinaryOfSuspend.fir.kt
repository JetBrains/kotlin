// !DIAGNOSTICS: -UNUSED_VARIABLE -NOTHING_TO_INLINE -UNUSED_PARAMETER
// SKIP_TXT
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

interface SuspendRunnable {
    suspend fun run()
}

// Function is NOT suspend
// parameter is inline
// parameter is suspend
// Block is NOT allowed to be called inside the body of owner inline function
// Block is NOT allowed to be called from nested classes/lambdas (as common crossinlines)
// It is NOT possible to call startCoroutine on the parameter
// suspend calls possible inside lambda matching to the parameter

inline fun test(c: suspend () -> Unit) {
    c()
    val o = object: SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
    val l: suspend () -> Unit = { c() }
    c.<!USAGE_IS_NOT_INLINABLE!>startCoroutine<!>(EmptyContinuation)
}

fun builder(c: suspend () -> Unit) {}

suspend fun calculate() = "OK"

fun box() {
    test {
        calculate()
    }
}
