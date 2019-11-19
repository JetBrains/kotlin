// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// WITH_COROUTINES
// COMMON_COROUTINES_TEST
import helpers.*
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*


class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        COROUTINE_SUSPENDED
    }

    var count = 0

    fun <T> log(value: T) {
        result += "$value"
    }

    fun check(): Boolean = count > 1

    suspend fun foo() { count++ }
}

fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.result += "return;"
    })
    return controller.result
}

suspend fun Controller.test() {
    var exception: Throwable? = null
    suspendLoop@do {
        log("slh;")
        foo()
        regularLoop@do {
            log("rlh;")
            if (!check()) {
                log("rlb;")
                break@regularLoop
            }

            log("rlc;")

            if (check()) {
                log("slb;")
                break@suspendLoop
            }

            log("fail1;")
        } while (false)

        log("slt;")
    } while (true)
}

fun box(): String {
    val res = builder {
        test()
    }

    if (res != "slh;rlh;rlb;slt;slh;rlh;rlc;slb;return;") return "FAIL: $res"
    return "OK"
}