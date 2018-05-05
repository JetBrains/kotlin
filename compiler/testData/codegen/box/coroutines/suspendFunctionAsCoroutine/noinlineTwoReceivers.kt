// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.COROUTINE_SUSPENDED
import COROUTINES_PACKAGE.intrinsics.suspendCoroutineOrReturn

class MyTest {
    suspend fun act(value: String): String = suspendCoroutineOrReturn {
        it.resume(value)
        COROUTINE_SUSPENDED
    }
}

suspend fun <T> testAsync(routine: suspend MyTest.() -> T): T = routine(MyTest())

suspend fun Iterable<String>.test() = testAsync {
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
