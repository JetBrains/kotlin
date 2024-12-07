// WITH_STDLIB
// FULL_JDK
// TARGET_BACKEND: JVM
// API_VERSION: LATEST

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
    val a = "a"
    saveSpilledVariables()
    blackhole(a)
    // a is dead, cleanup
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
    if (spilledVariables != setOf("label" to "1", "L$0" to "a")) return "FAIL 1: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "2", "L$0" to "null")) return "FAIL 2: $spilledVariables"
    c?.resume(Unit)
    if (spilledVariables != setOf("label" to "2", "L$0" to "null")) return "FAIL 3: $spilledVariables"

    return "OK"
}

fun main() {
    println(box())
}