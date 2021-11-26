// WITH_STDLIB

import kotlin.coroutines.*

class WrappedChannel<T>(channel: Channel<T> = Channel()): ReceiveChannel<T> by channel

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

fun <T> ReceiveChannel<T>.consumeAsFlow(): Flow<T> = ChannelAsFlow(this)

class ChannelAsFlow<T>(
    private val channel: ReceiveChannel<T>
): ChannelFlow<T>() {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.emit(channel.receive())
    }
}

abstract class ChannelFlow<T>: Flow<T>

var res = "FAIL"

object StringCollector : FlowCollector<String> {
    override suspend fun emit(value: String) {
        res = value
    }
}

interface ReceiveChannel<out E> {
    suspend fun receive(): E
}

interface Channel<E>: ReceiveChannel<E>

fun <E> Channel(): Channel<E> = object : Channel<E> {
    override suspend fun receive(): E {
        return "OK" as E
    }
}

fun box(): String {
    builder {
        WrappedChannel<String>().consumeAsFlow().collect(StringCollector)
    }
    return res
}