// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendWithResult(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        c.resume(value)
        COROUTINE_SUSPENDED
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
                    for (y in listOf("F", "G")) {
                        try {
                            result += suspendWithResult(y)
                            if (y == "G") {
                                break
                            }
                        }
                        finally {
                            result += "?"
                        }
                        result += "H"
                    }
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
    if (value != "AC!ED!@F?HG?*finally.") return "fail: $value"

    return "OK"
}
