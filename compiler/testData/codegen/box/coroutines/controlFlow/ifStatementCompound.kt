// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*


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
//    var value = builder {
//        if (suspendWithResult(true)) {
//            result = "OK"
//        }
//    }
    if (val value = builder { if (suspendWithResult(true)) { result = "OK" }}; value != "OK") return "fail: suspend as if condition: $value"


    if (val value = builder { for (x in listOf(true, false)) { if (x) { result += suspendWithResult("O") } else { result += "K" } } }; value != "OK") return "fail: suspend in then branch: $value"


    if (val value = builder { for (x in listOf(true, false)) { if (x) { result += "O" } else { result += suspendWithResult("K") } } };value != "OK") return "fail: suspend in else branch: $value"


    if (val  value = builder { for (x in listOf(true, false)) { if (x) { result += suspendWithResult("O") } else { result += suspendWithResult("K") } } };value != "OK") return "fail: suspend in both branches: $value"


    if (val value = builder {
        for (x in listOf(true, false)) {
            if (x) { result += suspendWithResult("O")}
            result += ";"
        }
    };value != "O;;") return "fail: suspend in then branch without else: $value"

    return "OK"
}
