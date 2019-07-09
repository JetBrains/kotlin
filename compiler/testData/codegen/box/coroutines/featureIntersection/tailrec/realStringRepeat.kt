// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// DONT_RUN_GENERATED_CODE: JS
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*

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
