// WITH_RUNTIME
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.experimental.startCoroutine

class Controller {
    suspend inline fun suspendInlineThrow(v: String): String = throw RuntimeException(v)
    suspend inline fun suspendInline(v: String) = v
}

fun builder(c: suspend Controller.() -> Unit) {
    c.startCoroutine(Controller(), EmptyContinuation)
}

class OK

fun box(): String {
    var result = ""

    builder {
        result = try { suspendInlineThrow("OK") } catch (e: RuntimeException) { e.message!! }
//        result = suspendInline("OK")
    }

    return result
}
