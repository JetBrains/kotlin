// WITH_STDLIB
// WITH_COROUTINES
import kotlin.coroutines.*

fun launch(block: suspend () -> String): String {
    var result = ""
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it.getOrThrow() })
    return result
}

suspend fun f(a: Int = 0): String? = null

suspend fun g(b: Boolean): String? = if (b) f() else f()

fun box() : String {
    return launch {
        if (g(false) != null) "Fail 1"
        else if (g(true) != null) "Fail 2"
        else "OK"
    }
}
