import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

suspend fun s1(): Unit = suspendCoroutineOrReturn { x ->
    println("s1")
    x.resume(Unit)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

inline suspend fun inline_s2(x: Int): Unit {
    println(x)
    s1()
}

suspend fun s3(x: Int) {
    inline_s2(x)
}

fun main(args: Array<String>) {
    var result = 0

    builder {
        s3(117)
    }

    println(result)
}