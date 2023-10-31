// FIR_IDENTICAL
// !LANGUAGE: +ForbidExtensionCallsOnInlineFunctionalParameters
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

inline fun test(<!INLINE_SUSPEND_FUNCTION_TYPE_UNSUPPORTED!>c: suspend () -> Unit<!>) {
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>c<!>()
    val o = object: SuspendRunnable {
        override suspend fun run() {
            <!NON_LOCAL_RETURN_NOT_ALLOWED!>c<!>()
        }
    }
    val l: suspend () -> Unit = { <!NON_LOCAL_RETURN_NOT_ALLOWED!>c<!>() }
    <!USAGE_IS_NOT_INLINABLE!>c<!>.startCoroutine(EmptyContinuation)
}

fun builder(c: suspend () -> Unit) {}

suspend fun calculate() = "OK"

fun box() {
    test {
        calculate()
    }
}
