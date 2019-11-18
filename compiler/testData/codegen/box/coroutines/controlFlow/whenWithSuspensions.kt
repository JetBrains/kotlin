// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

var result = ""

fun id(s: String) { result += s }

suspend fun bar() = Unit

suspend fun foo(a: Int) {
    when (a) {
        0 -> {
            id("0")
            bar() // slice switch
        }
        1, 2 -> {
            id("$a")
        }
        else -> Unit
    }
}

fun builder(callback: suspend () -> Unit) {
    callback.startCoroutine(object : ContinuationAdapter<Unit>() {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resume(value: Unit) = Unit
        override fun resumeWithException(exception: Throwable) {
            id("FAIL WITH EXCEPTION: ${exception.message}")
        }
    })
}

fun box():String {
    id("a")
    builder {
        foo(0)
        foo(1)
        foo(2)
    }
    id("b")
    if (result != "a012b") return "FAIL: $result"
    return "OK"
}