// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
// DONT_RUN_GENERATED_CODE: JS
import helpers.*
import COROUTINES_PACKAGE.*

tailrec suspend fun withWhen(counter : Int, d : Any) : Int =
    if (counter == 0) {
        0
    }
    else if (counter == 5) {
        withWhen(counter - 1, 999)
    }
    else
        when (d) {
            is String -> withWhen(counter - 1, "is String")
            is Number -> withWhen(counter, "is Number")
            else -> throw IllegalStateException()
        }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box() : String {
    var res = -1
    builder {
        res = withWhen(100000, "test")
    }
    return if (res == 0) "OK" else "FAIL"
}
