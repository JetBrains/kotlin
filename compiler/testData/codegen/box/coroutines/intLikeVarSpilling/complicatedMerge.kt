// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.*
import kotlin.coroutines.experimental.intrinsics.*

class Controller {
    suspend fun suspendHere(): Unit = suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        COROUTINE_SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun foo() = true

private var booleanResult = false
fun setBooleanRes(x: Boolean) {
    booleanResult = x
}

fun box(): String {
    builder {
        val x = true
        val y = false
        suspendHere()
        setBooleanRes(if (foo()) x else y)
    }

    if (!booleanResult) return "fail 1"

    return "OK"
}
