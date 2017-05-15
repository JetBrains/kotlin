// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.startCoroutine
import kotlin.coroutines.experimental.intrinsics.*

interface TestInterface {
    suspend fun toInt(): Int = suspendCoroutineOrReturn { x ->
        x.resume(56)
        COROUTINE_SUSPENDED
    }
}

class TestClass2 : TestInterface {
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = -1
    builder {
        result = TestClass2().toInt()
    }

    if (result != 56) return "fail 1: $result"

    return "OK"
}

