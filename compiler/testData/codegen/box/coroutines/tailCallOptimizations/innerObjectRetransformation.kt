// IGNORE_INLINER: IR
// WITH_STDLIB
// WITH_COROUTINES
// CHECK_BYTECODE_LISTING
// FIR_IDENTICAL

// In this test the following transformation are occuring:
//   flow$1 -> flowWith$$inlined$flow$1
//   flow$1 -> check$$inlined$flow$1
//   flow$1 -> flowWith$$inlined$flow$2
//   flowWith$$inlined$flow$2 -> check$$inlined$flowWith$1

// All thansformations, except the third, shall generate state-machine.
// The third shall not generate state-machine, since it is retransformed.

package flow

import kotlin.coroutines.*
import helpers.*

interface FlowCollector<T> {
    suspend fun emit(value: T)
}

interface Flow<T : Any> {
    suspend fun collect(collector: FlowCollector<T>)
}

public inline fun <T : Any> flow(crossinline block: suspend FlowCollector<T>.() -> Unit) = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) = collector.block()
}

suspend inline fun <T : Any> Flow<T>.collect(crossinline action: suspend (T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

inline fun <T : Any, R : Any> Flow<T>.flowWith(crossinline builderBlock: suspend Flow<T>.() -> Flow<R>): Flow<T> =
    flow {
        builderBlock()
    }

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

suspend fun check() {
    val f: Unit = flow<Int> {
        emit(1)
    }.flowWith {
        this
    }.collect {
        // In this test collect is just terminating operation, which just runs the lazy computations
    }
}

fun box(): String {
    builder {
        check()
    }
    return "OK"
}
