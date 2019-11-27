// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_COROUTINES
// FULL_JDK

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun returnsNull() = null

fun withLineBreak() = builder {
    returnsNull()
        ?: throw RuntimeException()
}

fun withoutLineBreak() = builder {
    returnsNull() ?: throw RuntimeException()
}

fun box(): String {
    try {
        withLineBreak()
        return "FAIL 0"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 19) return "FAIL 1 ${e.stackTrace[0].lineNumber}"
    }

    try {
        withoutLineBreak()
        return "FAIL 2"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 23) return "FAIL 3 ${e.stackTrace[0].lineNumber}"
    }

    return "OK"
}