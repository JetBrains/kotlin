// WITH_RUNTIME
// WITH_COROUTINES
// IGNORE_BACKEND_WITHOUT_CHECK: JS
import helpers.*
import kotlin.coroutines.experimental.*

tailrec suspend fun String.repeat(num : Int, acc : StringBuilder = StringBuilder()) : String =
        if (num == 0) acc.toString()
        else repeat(num - 1, acc.append(this))

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var s: String = ""
    builder {
        s = "a".repeat(10000)
    }
    return if (s.length == 10000) "OK" else "FAIL: ${s.length}"
}
