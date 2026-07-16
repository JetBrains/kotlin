// WITH_STDLIB
// WITH_COROUTINES
// WORKS_WHEN_VALUE_CLASS

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

// Value class wrapping a primitive. On Wasm it is represented unboxed, so a coroutine
// class field holding one must be initialized with a zero of the underlying type rather
// than a null reference. Regression test for the shared suspend lambda class defaults.

OPTIONAL_JVM_INLINE_ANNOTATION
value class Offset(val packed: Long)

suspend fun suspendHere(): String = suspendCoroutineUninterceptedOrReturn { c ->
    c.resume("K")
    COROUTINE_SUSPENDED
}

// Two structurally similar suspend lambdas, each spilling a value class local across a
// suspension point, so they are candidates for merging into one shared coroutine class.
fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

var result = ""

fun box(): String {
    builder {
        val a = Offset(7L)
        val s = suspendHere()
        result += "O" + s + a.packed
    }
    builder {
        val b = Offset(9L)
        val s = suspendHere()
        result += "-" + s + b.packed
    }
    if (result != "OK7-K9") return "FAIL: $result"
    return "OK"
}
