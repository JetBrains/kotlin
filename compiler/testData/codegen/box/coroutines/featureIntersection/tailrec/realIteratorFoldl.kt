// WITH_STDLIB
// WITH_COROUTINES
// DONT_RUN_GENERATED_CODE: JS
import helpers.*
import kotlin.coroutines.*

tailrec suspend fun <T, A> Iterator<T>.foldl(acc : A, foldFunction : (e : T, acc : A) -> A) : A =
        if (!hasNext()) acc
        else foldl(foldFunction(next(), acc), foldFunction)

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var sum = 0L
    builder {
        sum = (1..1000000).iterator().foldl(0) { e : Int, acc : Long ->
            acc + e
        }
    }

    return if (sum == 500000500000) "OK" else "FAIL: $sum"
}
