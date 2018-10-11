// !DIAGNOSTICS: -UNUSED_VARIABLE
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
// SKIP_TXT
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
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
    c.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>startCoroutine<!>(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() {
    test {
        <!ILLEGAL_SUSPEND_FUNCTION_CALL!>calculate<!>()
    }
}
