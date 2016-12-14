// WITH_RUNTIME
// WITH_COROUTINES
class Controller {
    suspend fun suspendHere(): Unit = suspendWithCurrentContinuation { x ->
        x.resume(Unit)
        SUSPENDED
    }

    // INTERCEPT_RESUME_PLACEHOLDER
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
