// WITH_RUNTIME
// WITH_COROUTINES
// TARGET_BACKEND: JVM
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.SUSPENDED_MARKER
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn

class MyTest {
    suspend fun act(value: String): String = suspendCoroutineOrReturn {
        it.resume(value)
        SUSPENDED_MARKER
    }
}

inline suspend fun <T> testAsync(routine: suspend MyTest.() -> T): T = routine(MyTest())

inline suspend fun Iterable<String>.test() = testAsync {
    var sum = ""
    for (v in this@test) {
        sum += act(v)
    }
    sum
}

fun builder(x: suspend () -> Unit) {
    x.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = ""
    builder {
        res = listOf("O", "K").test()
    }

    return res
}
