// WITH_STDLIB
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

inline class IC(val s: String)

suspend fun foo(x: String = "OK") = IC(x)

fun box(): String {
    var res = ""
    builder {
        res = foo().s
    }
    return res
}
