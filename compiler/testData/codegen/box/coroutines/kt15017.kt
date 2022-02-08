// WITH_STDLIB
// WITH_COROUTINES
import helpers.*
import kotlin.coroutines.startCoroutine

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
        result = try { suspendInlineThrow("O") } catch (e: RuntimeException) { e.message!! }
        result += suspendInline("K")
    }

    return result
}
