// FIR_IDENTICAL
// OPT_IN: kotlin.RequiresOptIn
// DIAGNOSTICS: -UNUSED_PARAMETER

import kotlin.experimental.ExperimentalTypeInference

fun test() {
    flow {
        emit(42)
        kotlin.coroutines.coroutineContext
    }
}

@OptIn(ExperimentalTypeInference::class)
fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): Flow<T> = TODO()

interface Flow<out T>

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}
