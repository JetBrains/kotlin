// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class TestClass {
    suspend fun toInt(): Int = suspendCoroutineOrReturn { x ->
        x.resume(14)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = 0

    var instance: TestClass? = null

    builder {
        result = 42 + (instance?.toInt() ?: 0)
    }

    if (result != 42) return "fail 1: $result"

    instance = TestClass()
    builder {
        result = 42 + (instance?.toInt() ?: 0)
    }

    if (result != 56) return "fail 2: $result"

    return "OK"
}
