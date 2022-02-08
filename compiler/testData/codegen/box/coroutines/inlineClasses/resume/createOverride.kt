// WITH_STDLIB

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

var c: Continuation<Any>? = null

fun box(): String {
    var res = ""
    builder {
        listOf(IC("O"), IC("K")).onEach { res += suspendCoroutine<String> { cont ->
            @Suppress("UNCHECKED_CAST")
            c = cont as Continuation<Any>
        }}
    }
    c?.resume("O")
    c?.resume("K")
    return res
}