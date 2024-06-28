// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.experimental.ExperimentalTypeInference

fun aFlow(): Flow<Unit> = channelFlow {
    awaitClose {
    }
}

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

@OptIn(ExperimentalTypeInference::class)
fun <T> channelFlow(block: suspend ProducerScope<T>.() -> Unit): Flow<T> = TODO()

interface ProducerScope<in E>

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

suspend fun ProducerScope<*>.awaitClose(block: () -> Unit = {}) {}