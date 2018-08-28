// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

fun builder(c: suspend () -> Unit): Unit {
    c.startCoroutine(handleResultContinuation {
        finished = true
    })
}

fun box(): String {
    builder {
        var i = 0
        var j = 0
        outer@while (i < 10) {
            while (j < 10) {
                if (i + j > 3) break@outer
                log += "$i,$j;"
                suspendAndContinue()
                j++
            }
            log += "i++;"
            i++
        }
        log += "done;"
        suspendAndContinue()
    }

    while (!finished) {
        log += "@;"
        postponed()
    }

    if (log != "0,0;@;0,1;@;0,2;@;0,3;@;done;@;") return "fail: $log"

    return "OK"
}

suspend fun suspendAndContinue(): Unit = suspendCoroutineUninterceptedOrReturn { c ->
    postponed = {
        c.resume(Unit)
    }
    COROUTINE_SUSPENDED
}

var postponed: () -> Unit = {}
var finished = false
var log = ""
