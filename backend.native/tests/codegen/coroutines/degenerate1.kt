import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

suspend fun s1() {
    println("s1")
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun main(args: Array<String>) {
    builder {
        s1()
    }
}