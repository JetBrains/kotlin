// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

data class A(val o: String) {
    operator suspend fun component2(): String = suspendCoroutineUninterceptedOrReturn { x ->
        x.resume("K")
        COROUTINE_SUSPENDED
    }
}
fun builder(c: suspend (A) -> Unit) {
    (c as (suspend A.() -> Unit)).startCoroutine(A("O"), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        (x, y) ->
        result = x + y
    }

    return result
}
