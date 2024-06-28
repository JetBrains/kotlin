// WITH_STDLIB
// WITH_COROUTINES
// CHECK_STATE_MACHINE
// JVM_ABI_K1_K2_DIFF: KT-62464

// In this test the following transformation are occuring:
//   flow$1 -> flowWith$$inlined$flow$1
//   flow$1 -> check$$inlined$flow$1
//   flow$1 -> flowWith$$inlined$flow$2
//   flowWith$$inlined$flow$2 -> check$$inlined$flowWith$1

// All thansformations, except the third, shall generate state-machine.
// The third shall not generate state-machine, since it is retransformed.

// FILE: inline.kt
package flow

interface FlowCollector<T> {
    suspend fun emit(value: T)
}

interface Flow<T : Any> {
    suspend fun collect(collector: FlowCollector<T>)
}

public inline fun <T : Any> flow(crossinline block: suspend FlowCollector<T>.() -> Unit) = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.block()
        collector.block()
    }
}

suspend inline fun <T : Any> Flow<T>.collect(crossinline action: suspend (T) -> Unit) {
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) {
            action(value)
            action(value)
        }
    })
}

inline fun <T : Any, R : Any> Flow<T>.flowWith(crossinline builderBlock: suspend Flow<T>.() -> Flow<R>): Flow<T> =
    flow {
        builderBlock()
        builderBlock()
    }

// FILE: box.kt
import flow.*

import helpers.*
import kotlin.coroutines.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(CheckStateMachineContinuation)
}

suspend fun check() {
    val f: Unit = flow<Int> {
        emit(1)
    }.flowWith {
        StateMachineChecker.suspendHere()
        StateMachineChecker.suspendHere()
        this
    }.collect {
        // In this test collect is just terminating operation, which just runs the lazy computations
    }
}

fun box(): String {
    StateMachineChecker.reset()
    builder {
        check()
    }
    StateMachineChecker.check(numberOfSuspensions = 8)
    return "OK"
}
