// WITH_RUNTIME

import kotlin.experimental.ExperimentalTypeInference

private fun CoroutineScope.asFairChannel(flow: Flow<*>): ReceiveChannel<Any> = produce {
    val channel = channel as ChannelCoroutine<Any>
    flow.collect { value ->
        return@collect channel.sendFair(value ?: Any())
    }
}

private fun CoroutineScope.asChannel(flow: Flow<*>): ReceiveChannel<Any> = produce {
    flow.collect { value ->
        return@collect channel.send(value ?: Any())
    }
}

suspend inline fun <T> Flow<T>.collect(crossinline action: suspend (value: T) -> Unit) {}

open class ChannelCoroutine<E> {
    suspend fun sendFair(element: E) {}
}

interface CoroutineScope
interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

interface ReceiveChannel<out E>

@OptIn(ExperimentalTypeInference::class)
fun <E> CoroutineScope.produce(
    @BuilderInference block: suspend ProducerScope<E>.() -> Unit
): ReceiveChannel<E> = TODO()

interface ProducerScope<in E> : CoroutineScope, SendChannel<E> {
    val channel: SendChannel<E>
}

interface SendChannel<in E> {
    suspend fun send(e: E)
}