// WITH_COROUTINES
// WITH_STDLIB
// FILE: test.kt

interface Flow<T> {
    abstract suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<T> {
    suspend fun emit(value: T)
}

inline fun <T> flow(crossinline body: suspend FlowCollector<T>.() -> Unit): Flow<T> =
    object : Flow<T> {
        override suspend fun collect(collector: FlowCollector<T>) = collector.body()
    }

suspend inline fun <T> Flow<T>.collect(crossinline body: suspend (T) -> Unit) =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = body(value)
    })

inline fun <T> Flow<T>.filter(crossinline predicate: suspend (T) -> Boolean): Flow<T> =
    flow<T> {
        this@filter.collect { if (predicate(it)) emit(it) }
    }

inline fun <reified R> Flow<*>.filterIsInstance(): Flow<R> =
    filter { it is R } as Flow<R>

// FILE: box.kt
import kotlin.coroutines.*
import helpers.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(EmptyContinuation)
}

fun box(): String {
    var result = "fail"
    builder {
        flow<String> { emit("OK") }.filterIsInstance<String>().collect { result = it }
    }
    return result
}
