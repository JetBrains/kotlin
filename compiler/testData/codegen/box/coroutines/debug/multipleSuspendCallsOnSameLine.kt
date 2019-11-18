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

suspend fun mightThrow(b: Boolean): Int {
    if (b) throw RuntimeException()
    return 1
}

fun multipleCalls(b: Boolean) = builder {
    mightThrow(b) + mightThrow(!b)
}

fun multipleCalls2(b: Boolean) = builder {
    mightThrow(b) + mightThrow(!b)
    throw RuntimeException()
}

var i = 0

suspend fun throwEverySecondCall(): Int {
    if ((i++ % 2) == 1) throw RuntimeException()
    return 1
}

fun multipleCalls3() = builder {
    throwEverySecondCall() + throwEverySecondCall()
    throwEverySecondCall()
}

fun multipleCalls4() = builder {
    throwEverySecondCall() + throwEverySecondCall()
}

fun box(): String {
    try {
        multipleCalls(true)
        return "FAIL 0"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 15) return "FAIL 1 ${e.stackTrace[0].lineNumber}"
        if (e.stackTrace[1].lineNumber != 20) return "FAIL 2 ${e.stackTrace[1].lineNumber}"
    }

    try {
        multipleCalls(false)
        return "FAIL 3"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 15) return "FAIL 4 ${e.stackTrace[0].lineNumber}"
        if (e.stackTrace[1].lineNumber != 20) return "FAIL 5 ${e.stackTrace[1].lineNumber}"
    }

    try {
        multipleCalls2(true)
        return "FAIL 6"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 15) return "FAIL 7 ${e.stackTrace[0].lineNumber}"
        if (e.stackTrace[1].lineNumber != 24) return "FAIL 8 ${e.stackTrace[1].lineNumber}"
    }

    try {
        multipleCalls2(false)
        return "FAIL 9"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 15) return "FAIL 10 ${e.stackTrace[0].lineNumber}"
        if (e.stackTrace[1].lineNumber != 24) return "FAIL 11 ${e.stackTrace[1].lineNumber}"
    }

    try {
        multipleCalls3()
        return "FAIL 12"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 31) return "FAIL 13 ${e.stackTrace[0].lineNumber}"
        if (e.stackTrace[1].lineNumber != 36) return "FAIL 14 ${e.stackTrace[1].lineNumber}"
    }

    try {
        multipleCalls4()
        return "FAIL 15"
    } catch (e: RuntimeException) {
        if (e.stackTrace[0].lineNumber != 31) return "FAIL 16 ${e.stackTrace[0].lineNumber}"
        if (e.stackTrace[1].lineNumber != 41) return "FAIL 17 ${e.stackTrace[1].lineNumber}"
    }

    return "OK"
}