// WITH_STDLIB
// IGNORE_BACKEND: JVM
// FILE: 1.kt

@file:OptIn(ExperimentalTypeInference::class)

import kotlin.experimental.*

fun interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

interface SendChannel<in E> {
    suspend fun send(element: E)
}

suspend fun <T> Flow<T>.toList(): List<T> {
    val destination = ArrayList<T>()
    collect { value ->
        destination.add(value)
    }
    return destination
}

fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): Flow<T> = SafeFlow(block)

private class SafeFlow<T>(private val block: suspend FlowCollector<T>.() -> Unit) : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.block()
    }
}

fun <T> channelFlow(block: suspend SendChannel<T>.() -> Unit): Flow<T> =
    ChannelFlowBuilder(block)

private open class ChannelFlowBuilder<T>(
    private val block: suspend SendChannel<T>.() -> Unit
) : ChannelFlow<T>() {
    override suspend fun collectTo(scope: SendChannel<T>) =
        block(scope)
}

abstract class ChannelFlow<T> : Flow<T> {
    protected abstract suspend fun collectTo(scope: SendChannel<T>)

    override suspend fun collect(collector: FlowCollector<T>): Unit {
        collectTo(object : SendChannel<T> {
            override suspend fun send(element: T) {}
        })
    }
}

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

inline fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> = flow {
    collect { value ->
        emit(transform(value))
    }
}

fun <T, R> Flow<T>.flatMapMerge(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    map(transform).flattenMerge()

fun <T> Flow<Flow<T>>.flattenMerge(): Flow<T> =
    ChannelFlowMerge(this)

// FILE: 2.kt

class ChannelFlowMerge<T>(val flow: Flow<Flow<T>>) : ChannelFlow<T>() {
    override suspend fun collectTo(scope: SendChannel<T>) {
        flow.collect {}
    }
}

// FILE: 3.kt

import kotlin.coroutines.*

fun box(): String {
    val l: suspend Any.() -> Unit = {
        flow { emit(1) }.flatMapMerge {
            channelFlow {
                val value = channelFlow { send(1) }
                send(value)
            }
        }.toList()
    }
    l.startCoroutine(Any(), Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
    return "OK"
}
