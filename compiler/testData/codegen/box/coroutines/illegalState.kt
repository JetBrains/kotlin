// WITH_RUNTIME
class Controller {
    suspend fun suspendHere(x: Continuation<Unit>) {
        x.resume(Unit)
    }
}

fun builder1(coroutine c: Controller.() -> Continuation<Unit>) {
    (c as Continuation<Unit>).resume(Unit)
}

fun builder2(coroutine c: Controller.() -> Continuation<Unit>) {
    val continuation = c(Controller())
    val declaredField = continuation!!.javaClass.getDeclaredField("label")
    declaredField.setAccessible(true)
    declaredField.set(continuation, -3)
    continuation.resume(Unit)
}

fun box(): String {

    try {
        builder1 {
            suspendHere()
        }
        return "fail 1"
    } catch (e: java.lang.IllegalStateException) {
        if (e.message != "call to 'resume' before 'invoke' with coroutine") return "fail 2: ${e.message!!}"
    }

    try {
        builder2 {
            suspendHere()
        }
        return "fail 3"
    } catch (e: java.lang.IllegalStateException) {
        if (e.message != "call to 'resume' before 'invoke' with coroutine") return "fail 4: ${e.message!!}"
        return "OK"
    }

    return "fail"
}
