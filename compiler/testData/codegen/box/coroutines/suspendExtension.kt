// WITH_RUNTIME
// WITH_COROUTINES
import kotlin.coroutines.*

@AllowSuspendExtensions
class Controller {
    suspend fun String.suspendHere(): String = suspendWithCurrentContinuation { x ->
        x.resume(this)
        SUSPENDED
    }

    inline suspend fun String.inlineSuspendHere(): String = suspendHere()

    // INTERCEPT_RESUME_PLACEHOLDER
}

suspend fun Controller.suspendExtension(v: String): String = v.suspendHere()

inline suspend fun Controller.inlineSuspendExtension(v: String): String = v.inlineSuspendHere()

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        if ("56".suspendHere() != "56") throw RuntimeException("fail 1")
        if ("28".inlineSuspendHere() != "28") throw RuntimeException("fail 2")

        if (suspendExtension("123") != "123") throw RuntimeException("fail 3")
        result = inlineSuspendExtension("OK")
    }

    return result
}
