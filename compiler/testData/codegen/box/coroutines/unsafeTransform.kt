// WITH_STDLIB
// WITH_COROUTINES

import kotlin.experimental.ExperimentalTypeInference
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import helpers.*

@OptIn(ExperimentalTypeInference::class)
inline fun <T, R> Flow<T>.unsafeTransform(
    @BuilderInference crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = unsafeFlow { // Note: unsafe flow is used here, because unsafeTransform is only for internal use
    collect { value ->
        // kludge, without it Unit will be returned and TCE won't kick in, KT-28938
        return@collect transform(value)
    }
}

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

fun interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

@OptIn(ExperimentalTypeInference::class)
inline fun <T> unsafeFlow(@BuilderInference crossinline block: suspend FlowCollector<T>.() -> Unit): Flow<T> {
    return object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) {
            collector.block()
        }
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var x = "init"
    
    builder {
        unsafeFlow { emit("OK") }.collect { x = it }
    }
    return x
}