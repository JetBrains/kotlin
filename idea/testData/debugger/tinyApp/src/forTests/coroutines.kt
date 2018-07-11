package forTests

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: SuccessOrFailure<Any?>) { result.getOrThrow() }
}


fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class WaitFinish(
        private val timeout: Long = 5,
        private val unit: TimeUnit = TimeUnit.SECONDS
) {
    private val cdl = CountDownLatch(1)

    fun finish() {
        cdl.countDown()
    }

    fun waitEnd() {
        if (!cdl.await(timeout, unit)) {
            throw java.lang.IllegalStateException("Too long wait in debugger test!")
        }

        Thread.sleep(10)
    }
}