// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class TestClass {
    suspend fun suspendHere(): String = suspendCoroutineOrReturn { x ->
        x.resume("K")
        SUSPENDED_MARKER
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun foo(x: String, y: String?) = x + (y ?: "")

fun box(): String {
    var result = ""

    var instance: TestClass? = null

    builder {
        result = foo("OK", instance?.suspendHere())
    }

    if (result != "OK") return "fail 1: $result"

    result = ""
    instance = TestClass()
    builder {
        result = foo("O", instance?.suspendHere())
    }

    if (result != "OK") return "fail 2: $result"

    return "OK"
}
