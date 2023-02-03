// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*

fun box(): String {
    async {
        val a = foo(23)
        log(a)
        val b = foo(42)
        log(b)
    }
    while (!finished) {
        log("--")
        proceed()
    }

    if (result != "suspend:23;--;23;suspend:42;--;42;--;done;") return "fail: $result"

    return "OK"
}

var result = ""

fun log(message: Any) {
    result += "$message;"
}

var proceed: () -> Unit = { }
var finished = false

suspend fun bar(x: Int): Int = suspendCoroutine { c ->
    log("suspend:$x")
    proceed = { c.resume(x) }
}

inline suspend fun foo(x: Int) = bar(x)

fun async(a: suspend () -> Unit) {
    a.startCoroutine(object : Continuation<Unit> {
        override fun resumeWith(value: Result<Unit>) {
            proceed = {
                log("done")
                finished = true
            }
        }

        override val context = EmptyCoroutineContext
    })
}
