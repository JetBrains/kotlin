// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER
// COMMON_COROUTINES_TEST
// SKIP_TXT
// WITH_COROUTINES
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

// Function is suspend
// parameter is inline
// parameter is NOT suspend
// Block is allowed to be called inside the body of owner inline function
// Block is NOT allowed to be called from nested classes/lambdas (as common crossinlines)
// It is NOT possible to call startCoroutine on the parameter
// suspend calls possible inside lambda matching to the parameter
suspend inline fun test(c: () -> Unit) {
    c()
    val o = object: Runnable {
        override fun run() {
            <!NON_LOCAL_RETURN_NOT_ALLOWED!>c<!>()
        }
    }
    val l = { <!NON_LOCAL_RETURN_NOT_ALLOWED!>c<!>() }
    c.<!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>startCoroutine<!>(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun builder(c: suspend () -> Unit) {}

fun box() {
    builder {
        test {
            calculate()
        }
    }
}
