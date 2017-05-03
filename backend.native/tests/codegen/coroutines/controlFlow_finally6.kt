import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resume(value: Any?) {}
    override fun resumeWithException(exception: Throwable) { throw exception }
}

suspend fun s1(): Int = suspendCoroutineOrReturn { x ->
    println("s1")
    x.resumeWithException(Error("error"))
    COROUTINE_SUSPENDED
}

fun f1(): Int {
    println("f1")
    return 117
}

fun f2(): Int {
    println("f2")
    return 1
}

fun f3(x: Int, y: Int): Int {
    println("f3")
    return x + y
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun main(args: Array<String>) {
    var result = 0

    builder {
        try {
            try {
                result = s1()
            } catch (t: ClassCastException) {
                result = f2()
            } finally {
                println("finally")
            }
        }
        catch(t: Error) {
            println(t.message)
        }
    }

    println(result)
}