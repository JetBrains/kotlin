package codegen.coroutines.controlFlow_finally6

import kotlin.test.*

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

open class EmptyContinuation(override val context: CoroutineContext = EmptyCoroutineContext) : Continuation<Any?> {
    companion object : EmptyContinuation()
    override fun resumeWith(result: SuccessOrFailure<Any?>) { result.getOrThrow() }
}

suspend fun s1(): Int = suspendCoroutineUninterceptedOrReturn { x ->
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

@Test fun runTest() {
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