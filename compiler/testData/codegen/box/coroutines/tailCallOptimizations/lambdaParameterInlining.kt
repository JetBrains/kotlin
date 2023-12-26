// TARGET_BACKEND: JVM
// FULL_JDK
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_TAIL_CALL_OPTIMIZATION
// JVM_ABI_K1_K2_DIFF: KT-63864

import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun assert(value: () -> Boolean) {}

class ChannelSegment<E>(val id: Long)

suspend fun suspendHere() = suspendCoroutineUninterceptedOrReturn<Unit> { x ->
    TailCallOptimizationChecker.saveStackTrace(x)
    COROUTINE_SUSPENDED
}

private const val RESULT_SUSPEND_NO_WAITER = 3

open class BufferedChannel<E> {
    private val sendSegment = ChannelSegment<E>(0)

    suspend fun send(element: E): Unit =
        sendImpl(
            onClosed = {},
            onNoWaiterSuspend = { sendOnNoWaiterSuspend() }
        )

    private suspend fun sendOnNoWaiterSuspend() {
        suspendHere()
    }

    private fun findSegmentSend(): ChannelSegment<E>? = null

    private fun updateCellSend(): Int = RESULT_SUSPEND_NO_WAITER

    private inline fun <R> sendImpl(
        onClosed: () -> R,
        onNoWaiterSuspend: () -> R = { error("unexpected") }
    ): R {
        var segment = sendSegment
        while (true) {
            val closed = false
            val id = 0L
            if (segment.id != id) {
                assert { segment.id < id }
                // Find the required segment.
                segment = null ?:
                        if (closed) return onClosed() else continue
            }
            when(updateCellSend()) {
                RESULT_SUSPEND_NO_WAITER -> {
                    return onNoWaiterSuspend()
                }
            }
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    builder {
        BufferedChannel<Int>().send(0)
    }
    TailCallOptimizationChecker.checkNoStateMachineIn("send")
    return "OK"
}