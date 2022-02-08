// WITH_STDLIB
// WITH_COROUTINES
// DONT_RUN_GENERATED_CODE: JS
import helpers.*
import kotlin.coroutines.*

tailrec suspend fun sum(x: Long, sum: Long): Long {
    if (x == 0.toLong()) return sum
    return sum(x - 1, sum + x)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var sum: Long = 0L
    builder {
        sum = sum(1000000, 0)
    }
    if (sum != 500000500000.toLong()) return "Fail $sum"
    return "OK"
}
