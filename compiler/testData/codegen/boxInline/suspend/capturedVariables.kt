// FILE: test.kt
// WITH_RUNTIME

import kotlin.coroutines.experimental.*

suspend inline fun test1(c: suspend () -> Unit) {
    c()
}

suspend inline fun test2(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = { c() }
    l()
}

// FILE: box.kt

import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context: CoroutineContext
            get() = EmptyCoroutineContext

        override fun resume(value: Unit) {
        }

        override fun resumeWithException(exception: Throwable) {
            throw exception
        }
    })
}

suspend fun calculate() = suspendCoroutineOrReturn<String> {
    it.resume("OK")
    COROUTINE_SUSPENDED
}

fun box() : String {
    var res = "FAIL 1"
    builder {
        val a = 1
        test2 {
            val b = 2
            test1 {
                val c = a + b // 3
                run {
                    val a = c + 1 // 4
                    test1 {
                        val b = c + c // 6
                        test2 {
                            val c = b - a // 2
                            res = "${calculate()} $a$b$c"
                        }
                    }
                }
            }
        }
    }
    if (res != "OK 462") return res
    return "OK"
}