// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
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
