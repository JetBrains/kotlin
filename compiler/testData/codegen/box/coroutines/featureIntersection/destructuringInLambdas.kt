// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

data class A(val o: String) {
    operator fun component2(): String = "K"
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
