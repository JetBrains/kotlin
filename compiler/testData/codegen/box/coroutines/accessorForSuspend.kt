// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

class A {
    suspend private fun a(): String = suspendCoroutineOrReturn { x ->
        x.resume("OK")
        COROUTINE_SUSPENDED
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
