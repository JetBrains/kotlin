// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.suspendCoroutineOrReturn
import kotlin.coroutines.experimental.intrinsics.COROUTINE_SUSPENDED

fun box(): String {
    async {
        log += "1;"
        wait()
        log += "2;"
    }

    while (postponed != null) {
        log += "suspended;"
        postponed!!()
    }

    if (log != "1;wait2;suspended;wait;suspended;2;") return "fail: $log"

    return "OK"
}

fun async(f: suspend () -> Unit) {
    f.startCoroutine(handleResultContinuation {
        postponed = null
    })
}

suspend fun wait(): Unit {
    wait2()
    log += "wait;"
    return suspendCoroutineOrReturn { c ->
        postponed = { c.resume(Unit) }
        COROUTINE_SUSPENDED
    }
}

suspend fun wait2(): Unit = suspendCoroutineOrReturn { c ->
    log += "wait2;"
    postponed = { c.resume(Unit) }
    COROUTINE_SUSPENDED
}

var postponed: (() -> Unit)? = { }

var log = ""