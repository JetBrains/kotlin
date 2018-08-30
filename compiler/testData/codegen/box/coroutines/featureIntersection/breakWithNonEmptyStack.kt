// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class A {
    var result = mutableListOf("O", "K", null)
    suspend fun foo(): String? = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume(result.removeAt(0))
        COROUTINE_SUSPENDED
    }
}

var result = ""

suspend fun append(ignore: String, x: String) {
    result += x
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun bar() {
    val a = A()
    while (true) {
        append("ignore", a.foo() ?: break)
    }
}

fun box(): String {
    builder {
        bar()
    }

    return result
}

