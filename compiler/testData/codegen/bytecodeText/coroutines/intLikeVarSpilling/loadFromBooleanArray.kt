// WITH_RUNTIME
// COMMON_COROUTINES_TEST
// WITH_COROUTINES
import helpers.*
// TREAT_AS_ONE_FILE
import COROUTINES_PACKAGE.*
import COROUTINES_PACKAGE.intrinsics.*

class Controller {
    suspend fun suspendHere(): Unit = suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

private var booleanResult = false
fun setBooleanRes(x: Boolean) {
    booleanResult = x
}

fun box(): String {
    builder {
        val a = booleanArrayOf(true)
        val x = a[0]
        suspendHere()
        setBooleanRes(x)
    }

    if (!booleanResult) return "fail 1"

    return "OK"
}

// 1 PUTFIELD .*\.Z\$0 : Z
