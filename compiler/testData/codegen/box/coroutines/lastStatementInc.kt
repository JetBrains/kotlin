// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { x ->
    x.resume("OK")
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    var wasHandleResultCalled = false
    c.startCoroutine(handleResultContinuation {
        wasHandleResultCalled = true
    })

    if (!wasHandleResultCalled) throw RuntimeException("fail 1")
}

fun box(): String {
    var result = 0

    builder {
        result++

        if (suspendHere() != "OK") throw RuntimeException("fail 2")

        result--
    }

    if (result != 0) return "fail 3"

    builder {
        --result

        if (suspendHere() != "OK") throw RuntimeException("fail 4")

        ++result
    }

    if (result != 0) return "fail 5"

    return "OK"
}
