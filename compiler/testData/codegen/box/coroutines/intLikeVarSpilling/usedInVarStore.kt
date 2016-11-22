class Controller {
    suspend fun suspendHere(x: Continuation<Unit>) {
        x.resume(Unit)
    }

    // INTERCEPT_RESUME_PLACEHOLDER
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    builder {
        val x = true
        suspendHere()
        val y: Boolean = x
        if (!y) throw IllegalStateException("fail 1")
    }

    builder {
        val x = '1'
        suspendHere()

        val y: Char = x
        if (y != '1') throw IllegalStateException("fail 2")
    }

    builder {
        val x: Byte = 1
        suspendHere()

        val y: Byte = x
        if (y != 1.toByte()) throw IllegalStateException("fail 3")
    }

    builder {
        val x: Short = 1

        suspendHere()

        val y: Short = x
        if (y != 1.toShort()) throw IllegalStateException("fail 4")
    }

    builder {
        val x: Int = 1
        suspendHere()

        val y: Int = x
        if (y != 1) throw IllegalStateException("fail 5")
    }

    return "OK"
}
