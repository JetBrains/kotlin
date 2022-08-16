package helpers

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import kotlin.coroutines.jvm.internal.*

class TailCallOptimizationCheckerClass {
    private val stackTrace = arrayListOf<StackTraceElement?>()

    suspend fun saveStackTrace() = suspendCoroutineUninterceptedOrReturn<Unit> {
        saveStackTrace(it)
    }

    fun saveStackTrace(c: Continuation<*>) {
        if (c !is CoroutineStackFrame) error("Continuation " + c + " is not subtype of CoroutineStackFrame")
        stackTrace.clear()
        var csf: CoroutineStackFrame? = c
        while (csf != null) {
            stackTrace.add(csf.getStackTraceElement())
            csf = csf.callerFrame
        }
    }

    private fun findStackTraceElement(method: String, className: String?): StackTraceElement? {
        val list = stackTrace.filter { it?.methodName == method }
        if (className != null) {
            return list.find { it?.className?.endsWith(className) == true }
        } else {
            return list.firstOrNull()
        }
    }

    fun checkNoStateMachineIn(method: String, className: String? = null) {
        findStackTraceElement(method, className)?.let {
            error("tail-call optimization miss: method at $it has state-machine " + stackTrace.joinToString(separator = "\n"))
        }
    }

    fun checkStateMachineIn(method: String, className: String? = null) {
        findStackTraceElement(method, className)
            ?: error("tail-call optimization hit: method $method has no state-machine " + stackTrace.joinToString(separator = "\n"))
    }
}

val TailCallOptimizationChecker = TailCallOptimizationCheckerClass()
