// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


fun builder(c: suspend () -> Int): Int {
    var res = 0
    c.startCoroutine(handleResultContinuation {
        res = it
    })

    return res
}

fun box(): String {
    var result = ""

    val handledResult = builder {
        result = "OK"
        56
    }

    if (handledResult != 56) return "fail 1: $handledResult"

    return result
}
