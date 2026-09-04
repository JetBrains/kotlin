// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

suspend fun isSmall(x: Int): Boolean = x < 3

suspend fun lastSmall(): Int = sequenceOf(1, 2, 3).last { isSmall(it) }

fun box(): String {
    var r = 0
    suspend { r = lastSmall() }.startCoroutine(EmptyContinuation)
    return if (r == 2) "OK" else "Fail: $r"
}
