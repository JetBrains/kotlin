// TODO: KT-36987 KT-37093
// WITH_STDLIB

// There should be no $foo$$inlined$map$1$1 class

interface FlowCollector<T> {
    suspend fun emit(value: T)
}

interface Flow<T> {
    suspend fun collect(collector: FlowCollector<T>)
}

public inline fun <T> flow(crossinline block: suspend FlowCollector<T>.() -> Unit) = object : Flow<T> {
    override suspend fun collect(collector: FlowCollector<T>) = collector.block()
}

suspend inline fun <T> Flow<T>.collect(crossinline action: suspend (T) -> Unit): Unit =
    collect(object : FlowCollector<T> {
        override suspend fun emit(value: T) = action(value)
    })

public inline fun <T, R> Flow<T>.transform(crossinline transformer: suspend FlowCollector<R>.(value: T) -> Unit): Flow<R> {
    return flow {
        return@flow collect { value ->
            return@collect transformer(value)
        }
    }
}

public inline fun <T, R> Flow<T>.map(crossinline transformer: suspend (value: T) -> R): Flow<R> = transform { value -> return@transform emit(transformer(value)) }

suspend fun foo() {
    flow<Int> {
        emit(1)
    }.map { it + 1 }
        .collect {
        }
}
