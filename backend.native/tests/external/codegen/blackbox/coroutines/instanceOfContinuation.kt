// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
// WITH_REFLECT
import kotlin.coroutines.*

class Controller {
    suspend fun runInstanceOf(): Boolean = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        val y: Any = x
        x.resume(x is Continuation<*>)
        CoroutineIntrinsics.SUSPENDED
    }

    suspend fun runCast(): Boolean = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        val y: Any = x
        x.resume(Continuation::class.isInstance(y as Continuation<*>))
        CoroutineIntrinsics.SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = runInstanceOf().toString() + "," + runCast().toString()
    }

    if (result != "true,true") return "fail: $result"

    return "OK"
}
