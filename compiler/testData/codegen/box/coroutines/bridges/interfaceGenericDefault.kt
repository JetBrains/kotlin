// Corresponds to KT-45166, KT-45320, KT-45490, and KT-45954.
// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

interface I<T> {
    suspend fun f(x: T): String = "OK"
}

class C : I<String>

fun box(): String {
    var result = "Fail"
    suspend {
        result = (C() as I<String>).f("Fail")
    }.startCoroutine(EmptyContinuation)
    return result
}
