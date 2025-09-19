// Corresponds to KT-45166, KT-45320, KT-45490, and KT-45954.
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

interface I<T> {
    suspend fun f(x: T): Int = 42
}

class C : I<Int>

fun box(): Boolean {
    var result = -1
    suspend {
        result = (C() as I<Int>).f(-1)
    }.startCoroutine(EmptyContinuation)
    return result == 42
}
