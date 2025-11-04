// TARGET_BACKEND: JVM
// IGNORE_BACKEND: ANDROID
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// API_VERSION: LATEST

// Does not work with IR inliner: Unknown structure of ADAPTER_FOR_CALLABLE_REFERENCE:
// IGNORE_INLINER: IR

// Using internal ModuleNameRetriever in stdlib replacement
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR
// PREFER_IN_TEST_OVER_STDLIB

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
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

var result = "Fail"

class Wrapper(val action: suspend () -> Unit) {
    init {
        action.startCoroutine(Continuation(EmptyCoroutineContext) { it.getOrThrow() })
    }
}

suspend fun suspendThere(v: String): String = suspendCoroutineUninterceptedOrReturn { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    result = v
    x.resume(v)
}

inline suspend fun some(a: String = "OK") = suspendThere(a)

fun box(): String {
    // Instead of box$some, we generate TestKt$box$1.invoke, which is not tail-call. See KT-78480
    Wrapper(::some)
    TailCallOptimizationChecker.checkStateMachineIn("invoke")
    return result
}
