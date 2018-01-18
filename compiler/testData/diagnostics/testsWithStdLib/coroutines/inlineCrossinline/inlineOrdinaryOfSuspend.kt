// !DIAGNOSTICS: -UNUSED_VARIABLE -NOTHING_TO_INLINE -UNUSED_PARAMETER
// SKIP_TXT
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(<!PARAMETER_NAME_CHANGED_ON_OVERRIDE!>data<!>: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

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
