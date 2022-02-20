// !JVM_DEFAULT_MODE: all
// TARGET_BACKEND: JVM
// WITH_STDLIB
// JVM_TARGET: 1.8
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

import kotlin.coroutines.*

interface IOk {
    fun ok(): String = "OK"
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlineClass<T: String>(val s: T) : IOk

suspend fun returnsUnboxed() = InlineClass("")

suspend fun test(): String = returnsUnboxed().ok()

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    var res = "FAIL"
    builder {
        res = test()
    }
    return res
}