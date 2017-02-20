// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = CoroutineIntrinsics.suspendCoroutineOrReturn { c ->
        c.resume(value)
        CoroutineIntrinsics.SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {
    val value = builder {
        try {
            outer@for (x in listOf("A", "B")) {
                try {
                    result += suspendWithResult(x)
                    for (y in listOf("C", "D")) {
                        try {
                            result += suspendWithResult(y)
                            if (y == "D") {
                                break@outer
                            }
                        }
                        finally {
                            result += "!"
                        }
                        result += "E"
                    }
                }
                finally {
                    result += "@"
                }
                result += "ignore"
            }
            result += "*"
        }
        finally {
            result += "finally"
        }
        result += "."
    }
    if (value != "AC!ED!@*finally.") return "fail: $value"

    return "OK"
}
