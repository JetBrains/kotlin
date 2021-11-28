// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

import java.util.concurrent.Executor
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

class Wrapper<T>(val pool: Executor, private val cont: Continuation<T>) : Continuation<T> {
    override val context: CoroutineContext
        get() = cont.context

    override fun resumeWith(result: Result<T>) {
        // Check that there's no compiler exception here
        pool.execute { cont.resumeWith(result) }
    }
}

fun box() = "OK"
