// MODULE: controller
// FILE: controller.kt
package lib

@AllowSuspendExtensions
class Controller {
    suspend fun String.suspendHere(x: Continuation<String>) {
        x.resume(this)
    }

    inline suspend fun String.inlineSuspendHere(x: Continuation<String>) {
        suspendHere(x)
    }
}

suspend fun Controller.suspendExtension(v: String, x: Continuation<String>) {
    v.suspendHere(x)
}

inline suspend fun Controller.inlineSuspendExtension(v: String, x: Continuation<String>) {
    v.inlineSuspendHere(x)
}

// MODULE: main(controller)
// FILE: main.kt
import lib.*

suspend fun Controller.localSuspendExtension(v: String, x: Continuation<String>) {
    v.suspendHere(x)
}

inline suspend fun Controller.localInlineSuspendExtension(v: String, x: Continuation<String>) {
    v.inlineSuspendHere(x)
}

fun builder(coroutine c: Controller.() -> Continuation<Unit>) {
    c(Controller()).resume(Unit)
}

fun box(): String {
    var result = ""

    builder {
        if ("56".suspendHere() != "56") throw RuntimeException("fail 1")
        if ("28".inlineSuspendHere() != "28") throw RuntimeException("fail 2")

        if (suspendExtension("123") != "123") throw RuntimeException("fail 3")
        if (inlineSuspendExtension("234") != "234") throw RuntimeException("fail 4")

        if (localSuspendExtension("9123") != "9123") throw RuntimeException("fail 5")
        if (localInlineSuspendExtension("9234") != "9234") throw RuntimeException("fail 6")

        result = "OK"
    }

    return result
}
