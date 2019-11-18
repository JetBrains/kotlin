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

suspend fun mightReturnNull(b: Boolean): String? {
    return if (b) null else "asdf"
}

fun throwOnSameLine(b: Boolean) = builder {
    if (mightReturnNull(b) == null) throw RuntimeException() else throw RuntimeException()
    throw RuntimeException()
}

fun box(): String {
    try {
        throwOnSameLine(true)
        return "FAIL 0"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 19) return "FAIL 1 ${e.stackTrace[0].lineNumber}"
    }

    try {
        throwOnSameLine(false)
        return "FAIL 2"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 19) return "FAIL 3 ${e.stackTrace[0].lineNumber}"
    }

    return "OK"
}