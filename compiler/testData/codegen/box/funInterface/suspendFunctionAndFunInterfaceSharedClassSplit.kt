// WITH_STDLIB

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun interface Susp {
    suspend fun run(): String
}

fun <T> runBlocking(c: suspend () -> T): T {
    var res: T? = null
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        res = it.getOrThrow()
    })
    return res!!
}

suspend fun ok(): String = "OK"

fun box(): String {
    val f: suspend () -> String = ::ok
    val s = Susp(::ok)

    val result = runBlocking {
        f() + s.run()
    }

    return if (result == "OKOK") "OK" else "FAIL: $result"
}
