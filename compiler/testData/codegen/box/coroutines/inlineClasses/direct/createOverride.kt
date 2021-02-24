// WITH_RUNTIME
// KJS_WITH_FULL_RUNTIME

import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

inline class IC(val s: String)

suspend fun <T> List<T>.onEach(c: suspend (T) -> Unit) {
    for (e in this) {
        c(e)
    }
}

fun box(): String {
    var res = ""
    builder {
        listOf(IC("O"), IC("K")).onEach { res += it.s }
    }
    return res
}