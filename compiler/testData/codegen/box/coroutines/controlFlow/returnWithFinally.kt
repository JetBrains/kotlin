// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var result = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineUninterceptedOrReturn { c ->
        result += "suspend($value);"
        c.resume(value)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> String): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.result += "return($it);"
    })
    return controller.result
}

fun builderUnit(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.result += "return;"
    })
    return controller.result
}

fun <T> id(value: T) = value

fun box(): String {
    var value = builder {
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
    if (value != "suspend(OK);finally;return(OK);") return "fail1: $value"

    value = builderUnit {
        try {
            if (id(23) == 23) {
                suspendAndLog("OK")
                return@builderUnit
            }
        }
        finally {
            result += "finally;"
        }
        result += "afterFinally;"
        "shouldNotReach"
    }
    if (value != "suspend(OK);finally;return;") return "fail2: $value"

    return "OK"
}
