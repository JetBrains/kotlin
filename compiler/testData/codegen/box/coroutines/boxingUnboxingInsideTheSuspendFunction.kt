// WITH_STDLIB
// KT-60785
// WORKS_WHEN_VALUE_CLASS

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

OPTIONAL_JVM_INLINE_ANNOTATION
value class SomeValue(val a: String) {
    override fun toString() = when (a) {
        "fa" -> "O"
        "il" -> "K"
        else -> ""
    }
}

suspend fun foo() = mapOf(SomeValue("fa") to SomeValue("il"))

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object : Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {}
    })
}

fun box(): String {
    var result = ""

    builder {
        for ((k, v) in foo()) {
            result += "$k$v"
        }
    }

    return result
}