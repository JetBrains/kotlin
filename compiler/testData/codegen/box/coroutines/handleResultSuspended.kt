// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    var log = ""

    suspend fun <T> suspendAndLog(value: T): T = suspendCoroutineUninterceptedOrReturn { x ->
        log += "suspend($value);"
        x.resume(value)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> String): String {
    val controller = Controller()
    c.startCoroutine(controller, handleResultContinuation {
        controller.log += "return($it);"
    })
    return controller.log
}

fun box(): String {
    val result = builder { suspendAndLog("OK") }

    if (result != "suspend(OK);return(OK);") return "fail: $result"

    return "OK"
}
