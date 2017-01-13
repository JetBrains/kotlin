// WITH_RUNTIME
// WITH_COROUTINES
// TREAT_AS_ONE_FILE
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

class Controller {
    suspend fun suspendHere(): Unit = suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        SUSPENDED_MARKER
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
