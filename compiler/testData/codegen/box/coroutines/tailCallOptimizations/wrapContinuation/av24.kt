// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// API_VERSION: LATEST

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    x.resume(v)
    COROUTINE_SUSPENDED
}

suspend fun suspendHere(): String = suspendThere("OK")

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = suspendHere()
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("suspendHere")

    return result
}

// CHECK_BYTECODE_TEXT
// One in suspendHere, one in suspendThere and one in TailCallOptimizationCheckerClass.saveStackTrace
// 3 INVOKESTATIC kotlin/coroutines/jvm/internal/TailCallAsyncStackTraceEntryKt.wrapContinuation.*