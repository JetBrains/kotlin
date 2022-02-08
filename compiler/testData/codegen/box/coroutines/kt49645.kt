// WITH_STDLIB
// WITH_COROUTINES
// TARGET_BACKEND: JVM

import helpers.*
import kotlin.coroutines.*

interface Cont1 {
    suspend fun flaf() = "O"

    suspend fun toResult(): Result<String> {
        val x = flaf()
        return Result.success(x + "K")
    }
}

@JvmInline
private value class ContImpl(val a: String) : Cont1

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "FAIL"
    builder {
        result = ContImpl("A").toResult().getOrThrow()
    }
    return result
}
