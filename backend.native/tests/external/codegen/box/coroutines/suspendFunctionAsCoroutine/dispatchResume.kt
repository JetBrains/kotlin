// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    var log = ""
    var resumeIndex = 0

    suspend fun <T> suspendWithValue(value: T): T = suspendCoroutineOrReturn { continuation ->
        log += "suspend($value);"
        continuation.resume(value)
        COROUTINE_SUSPENDED
    }

    suspend fun suspendWithException(value: String): Unit = suspendCoroutineOrReturn { continuation ->
        log += "error($value);"
        continuation.resumeWithException(RuntimeException(value))
        COROUTINE_SUSPENDED
    }
}

abstract class ContinuationDispatcher : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    abstract fun <T> dispatchResume(value: T, continuation: Continuation<T>): Boolean
    abstract fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(this, continuation)
}

private class DispatchedContinuation<T>(
        val dispatcher: ContinuationDispatcher,
        val continuation: Continuation<T>
): Continuation<T> {
    override val context: CoroutineContext = continuation.context

    override fun resume(value: T) {
        if (!dispatcher.dispatchResume(value, continuation))
            continuation.resume(value)
    }

    override fun resumeWithException(exception: Throwable) {
        if (!dispatcher.dispatchResumeWithException(exception, continuation))
            continuation.resumeWithException(exception)
    }
}

fun test(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation(object: ContinuationDispatcher() {
        private fun dispatchResume(block: () -> Unit) {
            val id = controller.resumeIndex++
            controller.log += "before $id;"
            block()
            controller.log += "after $id;"
        }

        override fun <P> dispatchResume(data: P, continuation: Continuation<P>): Boolean {
            dispatchResume {
                continuation.resume(data)
            }
            return true
        }

        override fun dispatchResumeWithException(exception: Throwable, continuation: Continuation<*>): Boolean {
            dispatchResume {
                continuation.resumeWithException(exception)
            }
            return true
        }
    }))
    return controller.log
}

suspend fun Controller.foo() = suspendWithValue("") + suspendWithValue("O")

suspend fun Controller.test1() {
    val o = foo()
    val k = suspendWithValue("K")
    log += "$o$k;"
}

suspend fun Controller.test2() {
    try {
        foo()

        suspendWithException("OK")
        log += "ignore;"
    }
    catch (e: RuntimeException) {
        log += "${e.message};"
    }
}

fun box(): String {
    var result = test {
        test1()
    }
    if (result != "before 0;suspend();before 1;suspend(O);before 2;suspend(K);before 3;OK;after 3;after 2;after 1;after 0;") return "fail1: $result"

    result = test {
        test2()
    }
    if (result != "before 0;suspend();before 1;suspend(O);before 2;error(OK);before 3;OK;after 3;after 2;after 1;after 0;") return "fail2: $result"

    return "OK"
}
