// TARGET_BACKEND: JVM
// WITH_RUNTIME
// WITH_COROUTINES

// FILE: flow.kt
package flow

interface FlowCollector<T> {
    suspend fun emit(value: T)
}

interface Flow<T : Any> {
    suspend fun collect(collector: FlowCollector<T>)
}

public inline fun <T : Any> flow(crossinline block: suspend FlowCollector<T>.() -> Unit): Flow<T> = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) = collector.block()
}

inline suspend fun <T : Any> Flow<T>.collect(crossinline action: suspend (T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

inline suspend fun inlineMe(crossinline block: suspend () -> Unit) = suspend {
    block()
}

// FILE: Test.kt

import flow.*
import helpers.*
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var str = "FAIL"
    builder {
        flow<Unit> {
            var continuation: Continuation<Unit>? = null
            suspendCoroutineUninterceptedOrReturn<Unit> { continuation = it; Unit }
            str = "$continuation"
        }.collect {
        }
    }
    if ("(Test.kt:" !in str) return str
    builder {
        inlineMe {
            var continuation: Continuation<Unit>? = null
            suspendCoroutineUninterceptedOrReturn<Unit> { continuation = it; Unit }
            str = "$continuation"
        }()
    }
    if ("(Test.kt:" !in str) return str
    return "OK"
}