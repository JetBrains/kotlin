// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

suspend fun one(): Int = 1

suspend fun sum(): Int {
    var s = 0
    for (x in sequence { yield(1); yield(2) }) s += x + one()
    return s
}

fun box(): String {
    var r = 0
    suspend { r = sum() }.startCoroutine(EmptyContinuation)
    return if (r == 5) "OK" else "Fail: $r"
}
