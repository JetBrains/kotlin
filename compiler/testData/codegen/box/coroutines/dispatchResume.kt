// WITH_STDLIB
// WITH_COROUTINES
// FULL_JDK
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var log = ""
    var resumeIndex = 0

    var callback: (() -> Unit)? = null

    suspend fun <T> suspendWithValue(value: T): T = suspendCoroutine { continuation ->
        log += "suspend($value);"
        callback = {
            continuation.resume(value)
        }
    }

    suspend fun suspendWithException(value: String): Unit = suspendCoroutine { continuation ->
        log += "error($value);"
        callback = {
            continuation.resumeWithException(RuntimeException(value))
        }
    }
}

abstract class ContinuationDispatcher : AbstractCoroutineContextElement(ContinuationInterceptor), ContinuationInterceptor {
    abstract fun <T> dispatchResumeWith(value: Result<T>, continuation: Continuation<T>): Boolean
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> = DispatchedContinuation(this, continuation)
}

private class DispatchedContinuation<T>(
        val dispatcher: ContinuationDispatcher,
        val continuation: Continuation<T>
): Continuation<T> {
    override val context: CoroutineContext = continuation.context

    override fun resumeWith(value: Result<T>) {
        if (!dispatcher.dispatchResumeWith(value, continuation))
            continuation.resumeWith(value)
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

        override fun <P> dispatchResumeWith(data: Result<P>, continuation: Continuation<P>): Boolean {
            dispatchResume {
                continuation.resumeWith(data)
            }
            return true
        }
    }))

    while (controller.callback != null) {
        val c = controller.callback!!
        controller.callback = null
        c()
    }

    return controller.log
}

fun box(): String {
    var result = test {
        val o = suspendWithValue("O")
        val k = suspendWithValue("K")
        log += "$o$k;"
    }
    if (result != "before 0;suspend(O);after 0;before 1;suspend(K);after 1;before 2;OK;after 2;") return "fail1: $result"

    result = test {
        try {
            suspendWithException("OK")
            log += "ignore;"
        }
        catch (e: RuntimeException) {
            log += "${e.message};"
        }
    }
    if (result != "before 0;error(OK);after 0;before 1;OK;after 1;") return "fail2: $result"

    return "OK"
}
