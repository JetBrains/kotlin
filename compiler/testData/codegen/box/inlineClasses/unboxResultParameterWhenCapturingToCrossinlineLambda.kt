// WITH_STDLIB

import kotlin.coroutines.*

interface Flow<out T> {
    suspend fun collect(collector: FlowCollector<T>)
}

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

suspend inline fun <T> Flow<T>.collect(crossinline action: suspend (value: T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(Continuation(EmptyCoroutineContext) {
        it.getOrThrow()
    })
}

fun box(): String {
    val flow: Flow<Result<String>> = object : Flow<Result<String>> {
        override suspend fun collect(collector: FlowCollector<Result<String>>) {
            collector.emit(Result.success("OK"))
        }
    }

    var res = "FAIL"
    builder {
        flow.collect { result ->
            result.onSuccess { text ->
                res = text
            }
        }
    }
    return res
}