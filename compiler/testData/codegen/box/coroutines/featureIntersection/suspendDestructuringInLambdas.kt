// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

data class A(val o: String) {
    operator suspend fun component2(): String = suspendCoroutineOrReturn { x ->
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
