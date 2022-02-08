// IGNORE_BACKEND: JVM
// WITH_STDLIB
// WITH_COROUTINES
// KT-27449

import helpers.EmptyContinuation
import kotlin.coroutines.*

var result = "Fail"

suspend fun doAction() {
    suspend fun run(
        a: String,
        f: suspend (String) -> Unit = { input -> result = input }
    ) {
        f(a)
    }

    run("OK")
}

fun box(): String {
    suspend {
        doAction()
    }.startCoroutine(EmptyContinuation)
    return result
}
