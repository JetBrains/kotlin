class Controller {
    suspend fun suspendHere(x: Continuation<Unit>) {
        x.resume(Unit)
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

private var byteResult: Byte = 0
fun setByteRes(x: Byte) {
    byteResult = x
}

fun box(): String {
    builder {
        val a = byteArrayOf(1)
        val x = a[0]
        suspendHere()
        setByteRes(x)
    }

    if (byteResult != 1.toByte()) return "fail 1"

    return "OK"
}
