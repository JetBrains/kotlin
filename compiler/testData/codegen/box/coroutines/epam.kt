// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// FULL_JDK
// COMMON_COROUTINES_TEST
// WITH_RUNTIME
// WITH_COROUTINES

import helpers.*
import COROUTINES_PACKAGE.*

class MyDeferred<T>(val t: suspend () -> T) {
    suspend fun await() = t()
}

fun <T> async(block: suspend () -> T) = MyDeferred(block)

inline fun <T1, T2, R> zip(source1: MyDeferred<T1>, source2: MyDeferred<T2>, crossinline zipper: (T1, T2) -> R) =
    async {
        zipper(source1.await(), source2.await())
    }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    val first = MyDeferred { 1 }
    val second = MyDeferred { 2 }
    var result = -1
    builder {
        result = zip(first, second) { firstValue: Int, secondValue: Int -> firstValue + secondValue }.await()
    }
    return if (result == 3) "OK" else "FAIL $result"
}