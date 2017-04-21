import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

suspend fun s1(): Int = suspendCoroutineOrReturn { x ->
    println("s1")
    x.resume(42)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun f1(): Int {
    println("f1")
    return 117
}

fun f2(): Int {
    println("f2")
    return 1
}

inline suspend fun inline_s2(): Int {
    var x = 0
    if (f1() > 0)
        x = s1()
    else x = f2()
    return x
}

fun main(args: Array<String>) {
    var result = 0

    builder {
        result = inline_s2()
    }

    println(result)
}
