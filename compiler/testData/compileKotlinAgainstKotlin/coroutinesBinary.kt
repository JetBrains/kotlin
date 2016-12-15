// FILE: A.kt
package a
class Controller {
    suspend fun suspendHere() = CoroutineIntrinsics.suspendCoroutineOrReturn<String> { x ->
        x.resume("OK")
        Suspend
    }
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

// FILE: B.kt
import a.builder

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }

    return result
}
