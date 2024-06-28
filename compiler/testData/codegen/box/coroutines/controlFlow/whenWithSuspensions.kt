// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

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
    callback.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(value: Result<Unit>) {
            val exception = value.exceptionOrNull() ?: return
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
