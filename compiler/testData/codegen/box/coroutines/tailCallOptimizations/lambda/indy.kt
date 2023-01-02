// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION

// JVM_TARGET: 1.8
// LAMBDAS: INDY
// CHECK_BYTECODE_TEXT
// JVM_IR_TEMPLATES
// 1 java/lang/invoke/LambdaMetafactory

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

suspend fun suspendHere(v: String): String =
    suspendCoroutineUninterceptedOrReturn { x ->
        TailCallOptimizationChecker.saveStackTrace(x)
        x.resume(v)
        COROUTINE_SUSPENDED
    }

suspend fun runsLambda(s: String): String {
    val lambda: suspend(String) -> String = {
        suspendHere(it)
    }
    return lambda(s)
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = ""

    builder {
        result = runsLambda("OK")
    }

    return result
}
