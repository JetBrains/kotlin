// This test depends on line numbers.
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// FULL_JDK
// WITH_COROUTINES

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER", "CANNOT_OVERRIDE_INVISIBLE_MEMBER")
package test
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

private fun BaseContinuationImpl.getSourceFileAndLineNumber(): Pair<String, Int> {
    return (getStackTraceElement()?.fileName ?: "") to (getStackTraceElement()?.lineNumber ?: -1)
}

suspend fun getSourceFileAndLineNumberFromContinuation() = suspendCoroutineUninterceptedOrReturn<Pair<String, Int>> {
    (it as BaseContinuationImpl).getSourceFileAndLineNumber()
}

var continuation: Continuation<*>? = null

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> {
    continuation = it
    COROUTINE_SUSPENDED
}

suspend fun dummy() {}

suspend fun named(): Pair<String, Int> {
    dummy()
    return getSourceFileAndLineNumberFromContinuation()
}

suspend fun suspended() {
    dummy()
    suspendHere()
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res: Any? = null
    builder {
        res = named()
    }
    if (res != Pair("runtimeDebugMetadata.kt", 33)) {
        return "" + res
    }
    builder {
        dummy()
        res = getSourceFileAndLineNumberFromContinuation()
    }
    if (res != Pair("runtimeDebugMetadata.kt", 55)) {
        return "" + res
    }

    builder {
        suspended()
    }
    res = (continuation!! as BaseContinuationImpl).getSourceFileAndLineNumber()
    if (res != Pair("runtimeDebugMetadata.kt", 38)) {
        return "" + res
    }
    return "OK"
}
