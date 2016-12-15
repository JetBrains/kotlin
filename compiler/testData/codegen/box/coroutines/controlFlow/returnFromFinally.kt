// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*


// Does not work in JVM backend, probably due to bug. It's not clear which behaviour is right.
// TODO: fix the bug and enable for JVM backend
// TARGET_BACKEND: JS

class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendWithCurrentContinuation { c ->
        result += "suspend($value);"
        c.resume(value)
        SUSPENDED
    }
}

fun builder(coroutine c: () -> String): String {
    val controller = Controller()
    c.startCoroutine(handleResult {
        controller.result += "return($value);"
    })
    return controller.result
}

fun <T> id(value: T) = value

fun box(): String {
    val value = builder {
        try {
            if (id(23) == 23) {
                return@builder suspendAndLog("OK")
            }
        }
        finally {
            result += "finally;"
        }
        result += "afterFinally;"
        "shouldNotReach"
    }
    if (value != "suspend(OK);finally;return(OK);") return "fail: $value"

    return "OK"
}
