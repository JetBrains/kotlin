// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var result = "FAIL"
}

fun faz(): String { throw IllegalStateException("OK") }

inline fun exceptionGet(a: () -> String): String {
    try {
        return a()
    } catch (e: Throwable) {
        return e.message!!
    }
}

suspend fun baz() {}

suspend fun Controller.bar() {
    result = exceptionGet { faz() }
    exceptionGet { baz(); "ignored" }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {
    return builder() { bar() }
}
