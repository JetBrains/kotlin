// WITH_RUNTIME
// WITH_COROUTINES
// TREAT_AS_ONE_FILE
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(): Unit = suspendWithCurrentContinuation { x ->
        x.resume(Unit)
        SUSPENDED
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
