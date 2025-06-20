// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// API_VERSION: LATEST

// Using internal ModuleNameRetriever in stdlib replacement
// DISABLE_IR_VISIBILITY_CHECKS: JVM_IR
// PREFER_IN_TEST_OVER_STDLIB

// FILE: TailCallAsyncStackTraceEntry.kt

package kotlin.coroutines.jvm.internal

import kotlin.coroutines.*

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
internal fun <T> wrapContinuation(
    declaringClass: String, methodName: String, fileName: String, lineNumber: Int,
    continuation: T,
): T where T : Continuation<Any?>, T : CoroutineStackFrame {
    val result =  object : Continuation<Any?>, CoroutineStackFrame {
        override val context: CoroutineContext
            get() = continuation.context

        override fun resumeWith(result: Result<Any?>) {
            continuation.resumeWith(result)
        }

        override val callerFrame: CoroutineStackFrame?
            get() = continuation

        override fun getStackTraceElement(): StackTraceElement? {
            val moduleName = ModuleNameRetriever.getModuleName(this)
            val moduleAndClass = if (moduleName == null) declaringClass else "$moduleName/${declaringClass}"
            return StackTraceElement(moduleAndClass, methodName, fileName, lineNumber)
        }

        override fun toString(): String =
            "Continuation at ${getStackTraceElement() ?: this::class.java.name}"
    } as T
    return result
}

// FILE: test.kt
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
    TailCallOptimizationChecker.checkStateMachineIn("suspendHere")

    return result
}
