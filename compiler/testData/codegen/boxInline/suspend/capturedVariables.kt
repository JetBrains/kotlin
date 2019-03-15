// IGNORE_BACKEND: JVM_IR
// FILE: test.kt
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import COROUTINES_PACKAGE.*
import helpers.*

suspend inline fun test1(c: suspend () -> Unit) {
    c()
}

suspend inline fun test2(crossinline c: suspend () -> Unit) {
    val l: suspend () -> Unit = { c() }
    l()
}

// FILE: box.kt
// COMMON_COROUTINES_TEST

import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun calculate() = suspendCoroutineUninterceptedOrReturn<String> {
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
