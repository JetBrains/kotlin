// !LANGUAGE: +SuspendConversion
// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM, JS, JS_IR
// IGNORE_BACKEND: JS_IR_ES6

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun useSuspendFun(fn : suspend () -> String) = fn()
suspend fun useSuspendFunInt(fn: suspend (Int) -> String) = fn(42)
suspend fun useSuspendFunStringString(fn: suspend (String, String) -> String) = fn("O", "K")

open class Test : () -> String, (Int) -> String {
    override fun invoke(): String = "OKEmpty"
    override fun invoke(p: Int) = "OK$p"
}

class Sub : Test(), (String, String) -> String {
    override fun invoke(p1: String, p2: String) = p1 + p2
}

fun box(): String {
    var test = "Failed"
    builder {
        test = useSuspendFun(Test())
    }

    if (test != "OKEmpty") return "failed 1"

    builder {
        test = useSuspendFunInt(Sub())
    }

    if (test != "OK42") return "failed 2"

    builder {
        test = useSuspendFunStringString(Sub())
    }

    return test
}
