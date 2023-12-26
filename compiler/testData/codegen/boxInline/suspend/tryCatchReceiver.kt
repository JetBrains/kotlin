// WITH_COROUTINES
// NO_CHECK_LAMBDA_INLINING
// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-62464

// FILE: inlined.kt

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

inline suspend fun <T> Flow<T>.collect(crossinline action: suspend (value: T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

inline fun <T, R> Flow<T>.transform(
    crossinline transform: suspend FlowCollector<R>.(value: T) -> Unit
): Flow<R> = flow {
    collect { value ->
        return@collect transform(value)
    }
}

inline fun <T, R> Flow<T>.map(crossinline transform: suspend (value: T) -> R): Flow<R> = transform { value ->
   return@transform emit(transform(value))
}

public fun <T> flow(block: suspend FlowCollector<T>.() -> Unit): Flow<T> = SafeFlow(block)

private class SafeFlow<T>(private val block: suspend FlowCollector<T>.() -> Unit) : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) {
        collector.block()
    }
}

// FILE: inlineSite.kt
import kotlin.coroutines.*
import helpers.*

fun Flow<String>.abc() = map { line ->
    try {
        line
    } catch (e: Exception) {
        e.message!!
    }
}

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var res = "FAIL"
    builder {
        flow<String> {
            emit("OK")
        }.abc().collect {
            res = it
        }
    }
    return res
}
