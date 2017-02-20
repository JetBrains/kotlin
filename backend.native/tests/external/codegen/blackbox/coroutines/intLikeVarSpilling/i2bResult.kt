// IGNORE_BACKEND: NATIVE
// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

class Controller {
    suspend fun suspendHere(): Unit = CoroutineIntrinsics.suspendCoroutineOrReturn { x ->
        x.resume(Unit)
        CoroutineIntrinsics.SUSPENDED
    }
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

private var byteResult: Byte = 0
fun setByteRes(x: Byte) {
    byteResult = x
}

fun foo(): Int = 1

fun box(): String {
    builder {
        val x: Byte = foo().toByte()
        suspendHere()
        setByteRes(x)
    }

    if (byteResult != 1.toByte()) return "fail 1"

    return "OK"
}
