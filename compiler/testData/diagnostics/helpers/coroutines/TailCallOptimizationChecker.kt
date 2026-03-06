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

    fun checkNoStateMachineIn(method: String) {
        stackTrace.find { it?.methodName?.startsWith(method) == true }?.let { error("tail-call optimization miss: method at " + it + " has state-machine " +
                                                                                            stackTrace.joinToString(separator = "\n")) }
    }

    fun checkStateMachineIn(method: String) {
        stackTrace.find { it?.methodName?.startsWith(method) == true } ?: error("tail-call optimization hit: method " + method + " has no state-machine " +
                                                                                        stackTrace.joinToString(separator = "\n"))
    }
}

val TailCallOptimizationChecker = TailCallOptimizationCheckerClass()
