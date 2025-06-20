// TARGET_BACKEND: JVM
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
