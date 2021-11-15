// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

interface I1<A, B> {
    suspend fun f(a: A, b: B): String
}

class C<A> : I1<A, String> {
    override suspend fun f(a: A, b: String): String = b
}

fun box(): String {
    var result = "fail"
    suspend {
        result = (C<Unit>() as I1<Unit, String>).f(Unit, "OK")
    }.startCoroutine(EmptyContinuation)
    return result
}
