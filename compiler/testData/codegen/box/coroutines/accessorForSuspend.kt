// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    suspend private fun a(): String = suspendCoroutineOrReturn { x ->
        x.resume("OK")
        SUSPENDED_MARKER
    }

    suspend fun myfun(): String {
        var result = "fail"
        builder {
            result = a()
        }

        return result
    }
}

fun box(): String {
    var result = ""

    builder {
        result = A().myfun()
    }

    return result
}
