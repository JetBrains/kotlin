// WITH_STDLIB
import kotlin.coroutines.*

suspend fun coroutineScope(c: suspend () -> Unit) {
    c()
}

var counter = 0

suspend fun whatever() = coroutineScope {
    repeat(10) { // repeat hides a loop, that plays a part in the compiler crash
        run {
            counter++
            return@repeat // required to reproduce the crash
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        whatever()
    }
    return if (counter != 10) "FAIL $counter" else "OK"
}