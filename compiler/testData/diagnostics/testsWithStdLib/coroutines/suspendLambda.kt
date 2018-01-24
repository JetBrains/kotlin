import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.startCoroutine

fun bar() {
    suspend {
        println()
    }.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = TODO("not implemented")

        override fun resume(value: Unit) {
            TODO("not implemented")
        }

        override fun resumeWithException(exception: Throwable) {
            TODO("not implemented")
        }
    })
}
