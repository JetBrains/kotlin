// !DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -NOTHING_TO_INLINE
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
// parameter is noinline
// parameter is suspend
// Block is NOT allowed to be called inside the body of owner inline function
// Block is allowed to be called from nested classes/lambdas (as common crossinlines)
// It is possible to call startCoroutine on the parameter
// suspend calls possible inside lambda matching to the parameter

inline fun test(noinline c: suspend () -> Unit)  {
    <!ILLEGAL_SUSPEND_FUNCTION_CALL!>c<!>()
    val o = object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }
    val l: suspend () -> Unit = { c() }
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = "OK"

fun box() {
    test {
        calculate()
    }
}
