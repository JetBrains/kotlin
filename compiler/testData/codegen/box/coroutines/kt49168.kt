// WITH_STDLIB
// WITH_COROUTINES
// IGNORE_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*

fun interface Print {
    suspend fun print(msg: String): String
}

object Context : Print by Print(::id)

fun id(x: String): String = x

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "Fail"
    builder {
        result = Context.print("OK")
    }
    return result
}
