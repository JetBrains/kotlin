// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// LANGUAGE: +WrapContinuationForTailCallFunctions

// kotlin package is being relocated in Android tests
// IGNORE_BACKEND: ANDROID

// Using internal ModuleNameRetriever in stdlib replacement
// DISABLE_IR_VISIBILITY_CHECKS: JVM
// PREFER_IN_TEST_OVER_STDLIB
// ALLOW_KOTLIN_PACKAGE

// FILE: TailCallAsyncStackTraceEntry.kt

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
internal fun <T> wrapContinuation(
    declaringClass: String, methodName: String, fileName: String, lineNumber: Int,
    spilledVariables: Array<Any?>,
    continuation: T,
): T where T : Continuation<Any?>, T : CoroutineStackFrame {
    return TailCallBaseContinuationImpl(
        declaringClass, methodName, fileName, lineNumber, spilledVariables, continuation
    ) as T
}

// FILE: test.kt
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

// KT-91567: the `$completion` of the inlined lambda is not a spilled variable, with or without inline scopes.
suspend inline fun call(block: suspend (Int) -> Int): Int = block(1)

private suspend fun foo(v: Int): Int {
    suspendThere()
    return v + 41
}

suspend fun first(): Int = call { v -> foo(v) } // tail-call opt, no state machine, wrapContinuation is invoked

var c: Continuation<Any?>? = null

suspend fun suspendThere(): Any? = suspendCoroutineUninterceptedOrReturn { x ->
    c = x
    TailCallOptimizationChecker.saveStackTrace(x)
    COROUTINE_SUSPENDED
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    builder {
        first()
    }
    TailCallOptimizationChecker.checkStateMachineIn("first")

    var firstHit = false

    while (c != null) {
        val cc = c
        if (cc is TailCallBaseContinuationImpl && cc.methodName == "first") {
            firstHit = true
            val names = cc.spilledVariables.filterIndexed { i, _ -> i % 2 == 0 }.map { (it as String).substringBefore('\\') }
            if ("v" !in names) return "FAIL 1 $names"
            if ("\$completion" in names) return "FAIL 2 $names"
        }
        c = (c as? BaseContinuationImpl)?.completion
    }

    if (!firstHit) return "FAIL 3 'first' is not present in completion chain"

    return "OK"
}
