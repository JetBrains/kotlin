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

suspend fun lock(owner: Controller) {
    owner.result += "L"
}

fun unlock(owner: Controller) {
    owner.result += "U"
}

public suspend inline fun doInline(owner: Controller, action: () -> Unit): Unit {
    lock(owner)
    try {
        return action()
    } finally {
        unlock(owner)
    }
}


fun builder(c: suspend Controller.() -> Unit): String {
    val controller = Controller()
    c.startCoroutine(controller, EmptyContinuation)
    return controller.result
}

fun box(): String {
    val value = builder {
        doInline(this) {
            result += "X"
        }
    }
    if (value != "LXU") return "fail: $value"

    return "OK"
}
