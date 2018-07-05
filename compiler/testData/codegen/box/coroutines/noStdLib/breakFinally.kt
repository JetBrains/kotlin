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
            var i1 = 0
            outer@while (i1 < 2) {
                val x = if (i1 == 0) "A" else "B"
                ++i1
                try {
                    result += suspendWithResult(x)
                    var i2 = 0
                    while (i2 < 2) {
                        val y = if (i2 == 0) "C" else "D"
                        ++i2
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
                    var i3 = 0
                    while (i3 < 2) {
                        val z = if (i3 == 0) "F" else "G"
                        ++i3
                        try {
                            result += suspendWithResult(z)
                            if (z == "G") {
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
