// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM
// API_VERSION: 2.1

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun blackhole(vararg a: Any?) {}

val spilledVariables = mutableSetOf<Pair<String, String>>()

var c: Continuation<Unit>? = null

suspend fun saveSpilledVariables() = suspendCoroutineUninterceptedOrReturn<Unit> { continuation ->
    spilledVariables.clear()
    for (field in continuation.javaClass.declaredFields) {
        if (field.name != "label" && (field.name.length != 3 || field.name[1] != '$')) continue
        field.isAccessible = true
        spilledVariables += field.name to "${field.get(continuation)}"
    }
    c = continuation
    COROUTINE_SUSPENDED
}

suspend fun test() {
    val a = null
    // a is null, known at compile time, do not spill
    saveSpilledVariables()
    blackhole(a)
    saveSpilledVariables()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        test()
    }
    if (spilledVariables != setOf("label" to "1")) return "FAIL 1: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "2")) return "FAIL 2: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "2")) return "FAIL 3: $spilledVariables"

    return "OK"
}