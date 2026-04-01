// NB This test depends on line numbers
// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK
// DISABLE_IR_VISIBILITY_CHECKS: ANY
@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
package test

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

suspend fun foo() {
    suspendHere(); suspendHere()
    /*
    A
    LOT
    OF
    EMPTY
    LINES
     */
    suspendHere()
    suspendHere()
}

val lambda: suspend () -> Unit = {
    suspendHere(); suspendHere()
    /*
    A
    LOT
    OF
    EMPTY
    LINES
     */
    suspendHere()
    suspendHere()
    println()
}

var continuation: Continuation<Unit>? = null

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    continuation = it
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun Continuation<*>?.nextLineNumber(): Int {
    val base = this as? BaseContinuationImpl ?: return -1
    return base.getNextLineNumber()
}

fun box(): String {
    builder {
        foo()
    }
    if (continuation.nextLineNumber() != 14) {
        return "FAIL 1 ${continuation.nextLineNumber()}"
    }
    continuation?.resume(Unit)
    if (continuation.nextLineNumber() != 22) {
        return "FAIL 2 ${continuation.nextLineNumber()}"
    }
    continuation?.resume(Unit)
    if (continuation.nextLineNumber() != 23) {
        return "FAIL 3 ${continuation.nextLineNumber()}"
    }
    continuation?.resume(Unit)
    if (continuation.nextLineNumber() != 24) {
        return "FAIL 4 ${continuation.nextLineNumber()}"
    }
    builder {
        lambda()
    }
    if (continuation.nextLineNumber() != 27) {
        return "FAIL 5 ${continuation.nextLineNumber()}"
    }
    continuation?.resume(Unit)
    if (continuation.nextLineNumber() != 35) {
        return "FAIL 6 ${continuation.nextLineNumber()}"
    }
    continuation?.resume(Unit)
    if (continuation.nextLineNumber() != 36) {
        return "FAIL 7 ${continuation.nextLineNumber()}"
    }
    continuation?.resume(Unit)
    if (continuation.nextLineNumber() != 37) {
        return "FAIL 8 ${continuation.nextLineNumber()}"
    }
    return "OK"
}
