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
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

@RestrictsSuspension
abstract class Scope {
    abstract suspend fun emit(s: String)
}

class ScopeImpl : Scope(), Continuation<Unit> {
    var result = ""
    var next: Continuation<Unit>? = null
    var done = false

    override suspend fun emit(s: String): Unit = suspendCoroutineUninterceptedOrReturn { x ->
        TailCallOptimizationChecker.saveStackTrace(x)
        result += s
        next = x
        COROUTINE_SUSPENDED
    }

    override val context: CoroutineContext
        get() = EmptyCoroutineContext

    override fun resumeWith(result: Result<Unit>) {
        result.getOrThrow()
        done = true
    }
}

suspend fun Scope.emitBoth() {
    emit("O")
    emit("K")
}

suspend fun Scope.emitTail() = emitBoth()

suspend fun SequenceScope<String>.yieldBoth() {
    yield("O")
    yield("K")
}

suspend fun SequenceScope<String>.yieldTail() = yieldBoth()

fun box(): String {
    val scope = ScopeImpl()
    val block: suspend Scope.() -> Unit = { emitTail() }
    block.startCoroutine(scope, scope)
    TailCallOptimizationChecker.checkStateMachineIn("emitTail")

    while (!scope.done) {
        val next = scope.next ?: return "FAIL 0: no continuation"
        scope.next = null
        next.resume(Unit)
    }
    if (scope.result != "OK") return "FAIL 1: ${scope.result}"

    val fromSequence = sequence { yieldTail() }.joinToString("")
    if (fromSequence != "OK") return "FAIL 2: $fromSequence"

    return "OK"
}
